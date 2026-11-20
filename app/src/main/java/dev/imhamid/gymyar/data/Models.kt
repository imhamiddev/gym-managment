package dev.imhamid.gymyar.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.LocalDate

@Serializable
data class Me(
    val id: String,
    val phone: String,
    @SerialName("first_name") val firstName: String,
    @SerialName("last_name") val lastName: String,
    val role: String,
) {
    val isCoach: Boolean get() = role == "COACH"
    val fullName: String get() = "$firstName $lastName"
    val roleLabel: String get() = if (isCoach) "مربی" else "ورزشکار"
}

@Serializable
data class GymInfo(
    val id: String,
    val name: String,
    val code: String,
    @SerialName("work_days") val workDays: List<Int> = emptyList(),
    @SerialName("open_time") val openTime: String = "06:00",
    @SerialName("close_time") val closeTime: String = "22:00",
    @SerialName("card_number") val cardNumber: String? = null,
    @SerialName("is_owner") val isOwner: Boolean = false,
)

@Serializable
data class PaymentDto(
    val id: String,
    val amount: Long,
    @SerialName("paid_on") val paidOn: String,
    @SerialName("duration_days") val durationDays: Int,
) {
    val paidDate: LocalDate get() = LocalDate.parse(paidOn)
    val endDate: LocalDate get() = paidDate.plusDays(durationDays.toLong())
}

@Serializable
data class MemberDto(
    val id: String,
    @SerialName("first_name") val firstName: String,
    @SerialName("last_name") val lastName: String,
    val phone: String,
    val joined: String? = null,
    @SerialName("height_cm") val heightCm: Int? = null,
    @SerialName("weight_kg") val weightKg: Double? = null,
    @SerialName("birth_date") val birthDate: String? = null,
    @SerialName("present_last_30") val presentLast30: Int = 0,
    val payments: List<PaymentDto> = emptyList(),
) {
    val fullName: String get() = "$firstName $lastName"
    val lastPayment: PaymentDto? get() = payments.firstOrNull()
    val birth: LocalDate? get() = birthDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
    fun remaining(today: LocalDate): Int? =
        dev.imhamid.gymyar.util.remainingDays(payments.map { it.paidDate to it.durationDays }, today)
}

@Serializable
data class PendingDto(
    val id: String,
    val name: String,
    val phone: String,
    val requested: String? = null,
)

@Serializable
data class PlanDto(
    val id: String,
    val name: String,
    val amount: Long,
    @SerialName("duration_days") val durationDays: Int,
)

@Serializable
data class CoachHome(
    val gym: GymInfo? = null,
    val members: List<MemberDto> = emptyList(),
    val pending: List<PendingDto> = emptyList(),
    val plans: List<PlanDto> = emptyList(),
)

@Serializable
data class AttendanceRecord(
    @SerialName("membership_id") val membershipId: String,
    val present: Boolean,
)

@Serializable
data class AttendanceDay(
    val day: String? = null,
    val records: List<AttendanceRecord> = emptyList(),
)

@Serializable
data class CoachItem(
    @SerialName("user_id") val userId: String,
    val name: String,
    val phone: String,
)

@Serializable
data class CoachesDto(val coaches: List<CoachItem> = emptyList())

@Serializable
data class CheckinInfo(
    @SerialName("gym_id") val gymId: String,
    @SerialName("gym_name") val gymName: String,
    val token: String,
    @SerialName("is_owner") val isOwner: Boolean = false,
)

@Serializable
data class CodeDto(val code: String)

@Serializable
data class AthleteGym(
    val id: String,
    val name: String,
    @SerialName("coach_name") val coachName: String = "",
    @SerialName("work_days") val workDays: List<Int> = emptyList(),
    @SerialName("open_time") val openTime: String = "06:00",
    @SerialName("close_time") val closeTime: String = "22:00",
    @SerialName("card_number") val cardNumber: String? = null,
)

@Serializable
data class AthleteDay(val day: String, val present: Boolean)

@Serializable
data class AthleteMembership(
    val id: String,
    val status: String,
    val gym: AthleteGym,
    @SerialName("height_cm") val heightCm: Int? = null,
    @SerialName("weight_kg") val weightKg: Double? = null,
    @SerialName("birth_date") val birthDate: String? = null,
    val payments: List<PaymentDto> = emptyList(),
    val attendance: List<AthleteDay> = emptyList(),
) {
    val isApproved: Boolean get() = status == "APPROVED"
    val birth: LocalDate? get() = birthDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
}

@Serializable
data class AthleteHome(val membership: AthleteMembership? = null)
