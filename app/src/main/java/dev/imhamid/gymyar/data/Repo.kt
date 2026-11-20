package dev.imhamid.gymyar.data

import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

sealed interface Res<out T> {
    /** [stale] = the data comes from the offline cache. */
    data class Ok<T>(val value: T, val stale: Boolean = false) : Res<T>
    data class Fail(val message: String, val offline: Boolean = false) : Res<Nothing>
}

/**
 * Every server call goes through here. Reads are cached per user (offline mode),
 * writes (act) need a connection and return the server's Persian error message on failure.
 */
object Repo {
    private val NOT_SIGNED_IN = "وارد حساب خود شوید."
    private val BAD_RESPONSE = "پاسخ نامعتبر از سرور دریافت شد."

    /** The SQL functions answer business errors as {"error": "..."}. */
    private fun serverError(body: String): String? = runCatching {
        val el = Api.json.parseToJsonElement(body)
        if (el is JsonObject) el["error"]?.jsonPrimitive?.contentOrNull else null
    }.getOrNull()

    suspend fun <T> read(
        cacheName: String,
        fn: String,
        serializer: KSerializer<T>,
        params: JsonObject = JsonObject(emptyMap()),
    ): Res<T> {
        val uid = Api.userId ?: return Res.Fail(NOT_SIGNED_IN)
        return when (val r = Api.rpc(fn, params)) {
            is Api.Raw.Ok -> {
                val err = serverError(r.body)
                if (err != null) {
                    Res.Fail(err)
                } else try {
                    val value = Api.json.decodeFromString(serializer, r.body)
                    Cache.put(uid, cacheName, r.body)
                    Res.Ok(value)
                } catch (e: Exception) {
                    Res.Fail(BAD_RESPONSE)
                }
            }
            is Api.Raw.Err -> Res.Fail(r.message)
            Api.Raw.Offline -> {
                val cached = Cache.get(uid, cacheName)
                if (cached != null) {
                    try {
                        Res.Ok(Api.json.decodeFromString(serializer, cached), stale = true)
                    } catch (e: Exception) {
                        Res.Fail(Api.OFFLINE, offline = true)
                    }
                } else {
                    Res.Fail(Api.OFFLINE, offline = true)
                }
            }
        }
    }

    /** A change on the server. Returns Ok(answer) or Fail(Persian message). */
    suspend fun act(fn: String, params: JsonObject = JsonObject(emptyMap())): Res<JsonObject> =
        when (val r = Api.rpc(fn, params)) {
            is Api.Raw.Ok -> {
                val err = serverError(r.body)
                if (err != null) Res.Fail(err)
                else runCatching { Res.Ok(Api.json.parseToJsonElement(r.body).jsonObject) }
                    .getOrElse { Res.Ok(JsonObject(emptyMap())) }
            }
            is Api.Raw.Err -> Res.Fail(r.message)
            Api.Raw.Offline -> Res.Fail("برای این کار به اینترنت نیاز دارید. اتصال را بررسی کنید.", offline = true)
        }

    // ---------------------------------------------------------------- reads

    suspend fun me() = read("me", "me", Me.serializer())
    suspend fun coachHome() = read("coach_home", "coach_home", CoachHome.serializer())
    suspend fun athleteHome() = read("athlete_home", "athlete_home", AthleteHome.serializer())
    suspend fun coachCoaches() = read("coach_coaches", "coach_coaches", CoachesDto.serializer())
    suspend fun checkinInfo() = read("checkin_info", "checkin_info", CheckinInfo.serializer())
    suspend fun attendance(day: String) = read(
        "attendance_$day", "coach_attendance", AttendanceDay.serializer(),
        Api.params { put("p_day", day) },
    )

    // ---------------------------------------------------------------- account

    suspend fun updateMyProfile(first: String, last: String) =
        act("update_my_profile", Api.params { put("p_first", first); put("p_last", last) })
}
