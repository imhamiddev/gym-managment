package dev.imhamid.gymyar.util

/**
 * Jalaali (Persian) <-> Gregorian conversion.
 * Port of the Borkowski algorithm used by jalaali-js (exact for Jalaali years -61..3177).
 */
object Jalali {
    data class J(val jy: Int, val jm: Int, val jd: Int)
    data class G(val gy: Int, val gm: Int, val gd: Int)

    private val BREAKS = intArrayOf(
        -61, 9, 38, 199, 426, 686, 756, 818, 1111, 1181,
        1210, 1635, 2060, 2097, 2192, 2262, 2324, 2394, 2456, 3178,
    )
    const val MIN_YEAR = -61
    const val MAX_YEAR = 3177

    val MONTHS = listOf(
        "فروردین", "اردیبهشت", "خرداد", "تیر", "مرداد", "شهریور",
        "مهر", "آبان", "آذر", "دی", "بهمن", "اسفند",
    )

    private fun div(a: Int, b: Int): Int = a / b
    private fun mod(a: Int, b: Int): Int = a - (a / b) * b

    private class Core(val gy: Int, val march: Int, val jump: Int, val n: Int)

    private fun core(jy: Int): Core {
        require(jy in MIN_YEAR..MAX_YEAR) { "Invalid Jalaali year $jy" }
        val gy = jy + 621
        var leapJ = -14
        var jp = BREAKS[0]
        var jump = 0
        for (i in 1 until BREAKS.size) {
            val jm = BREAKS[i]
            jump = jm - jp
            if (jy < jm) break
            leapJ += div(jump, 33) * 8 + div(mod(jump, 33), 4)
            jp = jm
        }
        val n = jy - jp
        leapJ += div(n, 33) * 8 + div(mod(n, 33) + 3, 4)
        if (mod(jump, 33) == 4 && jump - n == 4) leapJ += 1
        val leapG = div(gy, 4) - div((div(gy, 100) + 1) * 3, 4) - 150
        return Core(gy, 20 + leapJ - leapG, jump, n)
    }

    private fun leapFromCycle(jump: Int, n: Int): Int {
        var adjusted = n
        if (jump - n < 6) adjusted = n - jump + div(jump + 4, 33) * 33
        var leap = mod(mod(adjusted + 1, 33) - 1, 4)
        if (leap == -1) leap = 4
        return leap
    }

    fun isLeap(jy: Int): Boolean {
        val c = core(jy)
        return leapFromCycle(c.jump, c.n) == 0
    }

    fun monthLength(jy: Int, jm: Int): Int = when {
        jm <= 6 -> 31
        jm <= 11 -> 30
        else -> if (isLeap(jy)) 30 else 29
    }

    fun isValid(jy: Int, jm: Int, jd: Int): Boolean =
        jy in MIN_YEAR..MAX_YEAR && jm in 1..12 && jd >= 1 && jd <= monthLength(jy, jm)

    private fun g2d(gy: Int, gm: Int, gd: Int): Int {
        var d = div((gy + div(gm - 8, 6) + 100100) * 1461, 4) +
            div(153 * mod(gm + 9, 12) + 2, 5) + gd - 34840408
        d = d - div(div(gy + 100100 + div(gm - 8, 6), 100) * 3, 4) + 752
        return d
    }

    private fun d2g(jdn: Int): G {
        var j = 4 * jdn + 139361631
        j += div(div(4 * jdn + 183187720, 146097) * 3, 4) * 4 - 3908
        val i = div(mod(j, 1461), 4) * 5 + 308
        val gd = div(mod(i, 153), 5) + 1
        val gm = mod(div(i, 153), 12) + 1
        val gy = div(j, 1461) - 100100 + div(8 - gm, 6)
        return G(gy, gm, gd)
    }

    private fun j2d(jy: Int, jm: Int, jd: Int): Int {
        val r = core(jy)
        return g2d(r.gy, 3, r.march) + (jm - 1) * 31 - div(jm, 7) * (jm - 7) + jd - 1
    }

    private fun d2j(jdn: Int): J {
        val gy = d2g(jdn).gy
        var jy = minOf(gy - 621, MAX_YEAR)
        val c = core(jy)
        val leap = leapFromCycle(c.jump, c.n)
        val jdn1f = g2d(c.gy, 3, c.march)
        var k = jdn - jdn1f
        if (k >= 0) {
            if (k <= 185) return J(jy, 1 + div(k, 31), mod(k, 31) + 1)
            k -= 186
        } else {
            jy -= 1
            k += 179
            if (leap == 1) k += 1
        }
        return J(jy, 7 + div(k, 30), mod(k, 30) + 1)
    }

    fun toJalali(gy: Int, gm: Int, gd: Int): J = d2j(g2d(gy, gm, gd))
    fun toGregorian(jy: Int, jm: Int, jd: Int): G = d2g(j2d(jy, jm, jd))
}
