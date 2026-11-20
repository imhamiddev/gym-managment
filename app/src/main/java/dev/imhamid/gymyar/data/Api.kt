package dev.imhamid.gymyar.data

import android.content.Context
import android.content.SharedPreferences
import dev.imhamid.gymyar.BuildConfig
import dev.imhamid.gymyar.GymyarApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Thin Supabase client: GoTrue (auth) + PostgREST RPC over OkHttp.
 * The session lives in private SharedPreferences and is refreshed automatically.
 */
object Api {
    const val OFFLINE = "اتصال به اینترنت برقرار نیست."
    const val SESSION_EXPIRED = "نشست شما تمام شده است. دوباره وارد شوید."
    private const val BACKEND_MISSING =
        "بک‌اند Supabase هنوز راه‌اندازی نشده است. فایل SQL داخل پوشه‌ی supabase را در SQL Editor اجرا کنید."

    private val base = BuildConfig.SUPABASE_URL.trimEnd('/')
    private val key = BuildConfig.SUPABASE_ANON_KEY
    private val jsonMedia = "application/json; charset=utf-8".toMediaType()

    val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val prefs: SharedPreferences
        get() = GymyarApp.instance.getSharedPreferences("session", Context.MODE_PRIVATE)

    private val refreshLock = Mutex()
    private val _signedIn = MutableStateFlow(prefs.getString("refresh", null) != null)
    val signedIn: StateFlow<Boolean> get() = _signedIn

    val userId: String? get() = prefs.getString("uid", null)

    sealed interface Raw {
        data class Ok(val body: String) : Raw
        data class Err(val message: String) : Raw
        data object Offline : Raw
    }

    private fun nowSec() = System.currentTimeMillis() / 1000

    // ------------------------------------------------------------ http

    private suspend fun execute(request: Request): Pair<Int, String> = withContext(Dispatchers.IO) {
        client.newCall(request).execute().use { it.code to (it.body?.string() ?: "") }
    }

    private fun authRequest(path: String, body: JsonObject, bearer: String? = null): Request =
        Request.Builder()
            .url("$base$path")
            .header("apikey", key)
            .header("Authorization", "Bearer ${bearer ?: key}")
            .header("Accept", "application/json")
            .post(body.toString().toRequestBody(jsonMedia))
            .build()

    private fun errorText(body: String): String = runCatching {
        val o = json.parseToJsonElement(body).jsonObject
        listOf("msg", "message", "error_description", "error")
            .firstNotNullOfOrNull { o[it]?.jsonPrimitive?.contentOrNull }
    }.getOrNull() ?: ""

    // ------------------------------------------------------------ session

    private fun saveSession(o: JsonObject): Boolean {
        val access = o["access_token"]?.jsonPrimitive?.contentOrNull ?: return false
        val refresh = o["refresh_token"]?.jsonPrimitive?.contentOrNull ?: return false
        val expiresIn = o["expires_in"]?.jsonPrimitive?.longOrNull ?: 3600L
        val uid = runCatching { o["user"]?.jsonObject?.get("id")?.jsonPrimitive?.contentOrNull }.getOrNull()
            ?: prefs.getString("uid", null)
            ?: return false
        prefs.edit()
            .putString("access", access)
            .putString("refresh", refresh)
            .putLong("exp", nowSec() + expiresIn)
            .putString("uid", uid)
            .apply()
        _signedIn.value = true
        return true
    }

    fun signOutLocal() {
        prefs.edit().clear().apply()
        Cache.clearAll()
        _signedIn.value = false
    }

    private suspend fun validToken(force: Boolean = false): String? = refreshLock.withLock {
        val access = prefs.getString("access", null)
        val refresh = prefs.getString("refresh", null) ?: return@withLock null
        val exp = prefs.getLong("exp", 0L)
        if (!force && access != null && exp - 60 > nowSec()) return@withLock access
        try {
            val (code, body) = execute(
                authRequest(
                    "/auth/v1/token?grant_type=refresh_token",
                    buildJsonObject { put("refresh_token", refresh) },
                ),
            )
            if (code in 200..299) {
                val ok = runCatching { saveSession(json.parseToJsonElement(body).jsonObject) }.getOrDefault(false)
                if (ok) prefs.getString("access", null) else access
            } else if (code == 400 || code == 401 || code == 403) {
                signOutLocal()
                null
            } else {
                access
            }
        } catch (e: IOException) {
            access
        }
    }

    // ------------------------------------------------------------ auth

    private fun emailOf(phone: String) = "$phone@gymyar.app"

    private fun authFailure(code: Int, body: String): String {
        val text = errorText(body).lowercase()
        return when {
            code == 429 || "rate limit" in text -> "تعداد تلاش‌ها زیاد است. کمی بعد دوباره امتحان کنید."
            "invalid login" in text || "invalid_credentials" in body -> "شماره تلفن یا رمز عبور درست نیست."
            "already registered" in text || "user_already_exists" in body -> "این شماره قبلاً ثبت‌نام کرده است. وارد شوید."
            "password" in text && ("short" in text || "least" in text || "weak" in text) -> "رمز عبور باید حداقل ۶ کاراکتر باشد."
            "signups not allowed" in text || "signup_disabled" in body -> "ثبت‌نام در Supabase غیرفعال است."
            "database error" in text -> "ساخت حساب انجام نشد. مطمئن شوید فایل SQL بک‌اند اجرا شده است."
            else -> "خطای ورود (کد ${code}). دوباره تلاش کنید."
        }
    }

    /** Returns an error message, or null on success. */
    suspend fun signIn(phone: String, password: String): String? = try {
        val (code, body) = execute(
            authRequest(
                "/auth/v1/token?grant_type=password",
                buildJsonObject { put("email", emailOf(phone)); put("password", password) },
            ),
        )
        if (code in 200..299 && runCatching { saveSession(json.parseToJsonElement(body).jsonObject) }.getOrDefault(false)) null
        else authFailure(code, body)
    } catch (e: IOException) {
        OFFLINE
    }

    /** Returns an error message, or null on success (the user is then signed in). */
    suspend fun signUp(first: String, last: String, phone: String, password: String, role: String): String? = try {
        val (code, body) = execute(
            authRequest(
                "/auth/v1/signup",
                buildJsonObject {
                    put("email", emailOf(phone))
                    put("password", password)
                    put("data", buildJsonObject { put("first_name", first); put("last_name", last); put("role", role) })
                },
            ),
        )
        if (code in 200..299) {
            val ok = runCatching { saveSession(json.parseToJsonElement(body).jsonObject) }.getOrDefault(false)
            if (ok) null else "ثبت‌نام انجام شد اما ورود خودکار ممکن نیست: در Supabase گزینه‌ی «Confirm email» را خاموش کنید."
        } else authFailure(code, body)
    } catch (e: IOException) {
        OFFLINE
    }

    suspend fun signOut() {
        val access = prefs.getString("access", null)
        if (access != null) {
            runCatching { execute(authRequest("/auth/v1/logout", JsonObject(emptyMap()), access)) }
        }
        signOutLocal()
    }

    // ------------------------------------------------------------ rpc

    private fun rpcFailure(code: Int, body: String): String {
        if (code == 404 || "PGRST202" in body) return BACKEND_MISSING
        if (code == 401) return SESSION_EXPIRED
        val msg = errorText(body)
        return if (msg.isNotBlank()) "خطای سرور: $msg" else "خطای سرور (کد $code)."
    }

    suspend fun rpc(fn: String, params: JsonObject = JsonObject(emptyMap())): Raw {
        var token = validToken() ?: return Raw.Err(SESSION_EXPIRED)
        for (attempt in 0..1) {
            val request = Request.Builder()
                .url("$base/rest/v1/rpc/$fn")
                .header("apikey", key)
                .header("Authorization", "Bearer $token")
                .header("Accept", "application/json")
                .post(params.toString().toRequestBody(jsonMedia))
                .build()
            val (code, body) = try {
                execute(request)
            } catch (e: IOException) {
                return Raw.Offline
            }
            if (code in 200..299) return Raw.Ok(body)
            if (code == 401 && attempt == 0) {
                token = validToken(force = true) ?: return Raw.Err(SESSION_EXPIRED)
                continue
            }
            return Raw.Err(rpcFailure(code, body))
        }
        return Raw.Err(SESSION_EXPIRED)
    }

    /** Small helper to build RPC params. */
    fun params(block: kotlinx.serialization.json.JsonObjectBuilder.() -> Unit): JsonObject = buildJsonObject(block)
}
