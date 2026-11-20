package dev.imhamid.gymyar.util

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

private const val FA_DIGITS = "۰۱۲۳۴۵۶۷۸۹"

/** Latin digits -> Persian digits (any other character is kept). */
fun String.fa(): String = buildString(length) {
    for (c in this@fa) append(if (c in '0'..'9') FA_DIGITS[c - '0'] else c)
}

fun Int.fa(): String = toString().fa()
fun Long.fa(): String = toString().fa()

/** Persian / Arabic-Indic digits -> Latin digits. */
fun String.en(): String = buildString(length) {
    for (c in this@en) append(
        when (c) {
            in '۰'..'۹' -> '0' + (c - '۰')
            in '٠'..'٩' -> '0' + (c - '٠')
            else -> c
        },
    )
}

/** "۱٬۵۰۰٬۰۰۰ تومان" */
fun formatToman(n: Long): String {
    val s = n.toString()
    val grouped = buildString {
        for ((i, c) in s.withIndex()) {
            if (i > 0 && (s.length - i) % 3 == 0) append('٬')
            append(c)
        }
    }
    return "${grouped.fa()} تومان"
}

fun formatDays(n: Int): String = "${n.fa()} روز"

/** "6037991234567890" -> "۶۰۳۷ ۹۹۱۲ ۳۴۵۶ ۷۸۹۰" */
fun formatCard(card: String): String = card.chunked(4).joinToString(" ").fa()

fun formatHours(open: String, close: String) = "${open.fa()} تا ${close.fa()}"

/** Any digits -> clean 16-digit card number, or null. */
fun cleanCard(input: String): String? {
    val d = input.en().replace(Regex("[\\s\\-_.]"), "")
    return if (Regex("^\\d{16}$").matches(d)) d else null
}

fun isTime(s: String) = Regex("^([01]\\d|2[0-3]):[0-5]\\d$").matches(s)

// ---------------------------------------------------------------- weekdays (JS numbering: 0 = Sunday)

val WEEKDAY_NAMES = mapOf(
    6 to "شنبه", 0 to "یک‌شنبه", 1 to "دوشنبه", 2 to "سه‌شنبه", 3 to "چهارشنبه", 4 to "پنج‌شنبه", 5 to "جمعه",
)
val WEEK_ORDER = listOf(6, 0, 1, 2, 3, 4, 5)
val EVEN_DAYS = listOf(6, 1, 3)
val ODD_DAYS = listOf(0, 2, 4)
val ALL_DAYS = listOf(0, 1, 2, 3, 4, 5, 6)

data class DayPreset(val key: String, val label: String, val days: List<Int>)

val DAY_PRESETS = listOf(
    DayPreset("even", "روزهای زوج", EVEN_DAYS),
    DayPreset("odd", "روزهای فرد", ODD_DAYS),
    DayPreset("all", "همه روزها", ALL_DAYS),
)

private fun sameDays(a: List<Int>, b: List<Int>) = a.size == b.size && a.all { it in b }

fun describeDays(days: List<Int>): String {
    val list = WEEK_ORDER.filter { it in days }.joinToString("، ") { WEEKDAY_NAMES.getValue(it) }
    val preset = DAY_PRESETS.firstOrNull { sameDays(it.days, days) }
    if (preset?.key == "all") return "همه روزهای هفته"
    return if (preset != null) "${preset.label} ($list)" else list.ifEmpty { "—" }
}

/** JS-style weekday of a date: 0 = Sunday ... 6 = Saturday. */
fun LocalDate.jsWeekday(): Int = if (dayOfWeek == DayOfWeek.SUNDAY) 0 else dayOfWeek.value

fun isGymDay(workDays: List<Int>, date: LocalDate) = date.jsWeekday() in workDays

/** Nearest open day before/after [from] (step -1 or 1); [from] itself if none. */
fun nextGymDay(workDays: List<Int>, from: LocalDate, step: Int): LocalDate {
    if (workDays.isEmpty()) return from
    var cur = from
    repeat(7) {
        cur = cur.plusDays(step.toLong())
        if (isGymDay(workDays, cur)) return cur
    }
    return from
}

// ---------------------------------------------------------------- dates

val TEHRAN: ZoneId = ZoneId.of("Asia/Tehran")

fun todayTehran(): LocalDate = LocalDate.now(TEHRAN)

fun minutesTehran(): Int {
    val t = ZonedDateTime.now(TEHRAN)
    return t.hour * 60 + t.minute
}

fun jalaliOf(d: LocalDate): Jalali.J = Jalali.toJalali(d.year, d.monthValue, d.dayOfMonth)

fun jalaliToDate(jy: Int, jm: Int, jd: Int): LocalDate? {
    if (!Jalali.isValid(jy, jm, jd)) return null
    val g = Jalali.toGregorian(jy, jm, jd)
    return LocalDate.of(g.gy, g.gm, g.gd)
}

private fun p2(n: Int) = n.toString().padStart(2, '0')

/** "۱۴۰۵/۰۸/۲۶" */
fun formatJalali(d: LocalDate): String {
    val j = jalaliOf(d)
    return "${j.jy}/${p2(j.jm)}/${p2(j.jd)}".fa()
}

/** "۲۶ آبان ۱۴۰۵" */
fun formatJalaliLong(d: LocalDate): String {
    val j = jalaliOf(d)
    return "${j.jd.fa()} ${Jalali.MONTHS[j.jm - 1]} ${j.jy.fa()}"
}

fun formatJalali(ymd: String): String = runCatching { formatJalali(LocalDate.parse(ymd)) }.getOrDefault(ymd)

fun isBirthday(birth: LocalDate?, today: LocalDate): Boolean {
    if (birth == null) return false
    val b = jalaliOf(birth)
    val t = jalaliOf(today)
    return b.jm == t.jm && b.jd == t.jd
}

fun ageOn(birth: LocalDate, today: LocalDate): Int {
    val b = jalaliOf(birth)
    val t = jalaliOf(today)
    return t.jy - b.jy - (if (t.jm < b.jm || (t.jm == b.jm && t.jd < b.jd)) 1 else 0)
}

/** Days left from [today] until the latest payment runs out. null = no payment yet. */
fun remainingDays(payments: List<Pair<LocalDate, Int>>, today: LocalDate): Int? {
    if (payments.isEmpty()) return null
    val end = payments.maxOf { it.first.plusDays(it.second.toLong()) }
    return maxOf(0L, java.time.temporal.ChronoUnit.DAYS.between(today, end)).toInt()
}
