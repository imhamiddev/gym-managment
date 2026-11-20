package dev.imhamid.gymyar.ui.athlete

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.imhamid.gymyar.data.AthleteMembership
import dev.imhamid.gymyar.data.Me
import dev.imhamid.gymyar.ui.components.*
import dev.imhamid.gymyar.ui.theme.gym
import dev.imhamid.gymyar.util.*
import java.time.LocalDate

@Composable
private fun Info(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleSmall)
    }
}

@Composable
fun AthleteDashboard(me: Me, m: AthleteMembership, stale: Boolean, onScan: () -> Unit) {
    val g = MaterialTheme.gym
    val primary = MaterialTheme.colorScheme.primary
    val clipboard = LocalClipboardManager.current
    val today = remember { todayTehran() }
    val gym = m.gym

    val remaining = remainingDays(m.payments.map { it.paidDate to it.durationDays }, today)
    val latest = m.payments.maxByOrNull { it.endDate }
    val fraction = if (latest != null && remaining != null && latest.durationDays > 0) (remaining.toFloat() / latest.durationDays).coerceIn(0f, 1f) else 0f
    val tone = when {
        remaining == null || remaining == 0 -> g.danger
        remaining <= 7 -> g.warning
        else -> g.success
    }
    val presentCount = m.attendance.count { it.present }
    val openToday = isGymDay(gym.workDays, today)

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (stale) Banner("حالت آفلاین: آخرین اطلاعات ذخیره‌شده نمایش داده می‌شود.", g.warning)
        PageHeader("سلام ${me.firstName}", "${gym.name} · مربی: ${gym.coachName}", Icons.Rounded.FitnessCenter)

        if (isBirthday(m.birth, today)) {
            Row(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(g.attendance.soft(0.1f)).padding(14.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                IconBubble(Icons.Rounded.Cake, g.attendance)
                Text("تولدت مبارک! 🎉", style = MaterialTheme.typography.titleSmall)
            }
        }

        // tuition status
        AppCard {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    IconBubble(Icons.Rounded.Payments, tone, size = 40.dp, iconSize = 20.dp)
                    Text("وضعیت شهریه", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    RemainingBadge(remaining)
                }
                if (remaining == null) {
                    Text("هنوز شهریه‌ای برای شما ثبت نشده است. با مربی هماهنگ کنید.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(remaining.fa(), style = MaterialTheme.typography.displaySmall, color = tone)
                        Text("روز مانده", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 8.dp))
                    }
                    val animatedFraction by animateFloatAsState(fraction, tween(800, easing = FastOutSlowInEasing), label = "tuition-progress")
                    LinearProgressIndicator(
                        progress = { animatedFraction }, color = tone, trackColor = tone.soft(0.15f),
                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(50)),
                    )
                    if (latest != null) {
                        Text(
                            "اعتبار تا ${formatJalaliLong(latest.endDate)}",
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        // check-in
        PrimaryButton("ثبت حضور با QR", onClick = onScan, icon = Icons.Rounded.QrCodeScanner, container = g.attendance, content = androidx.compose.ui.graphics.Color.White)
        Text(
            if (openToday) "امروز باشگاه فعال است (${formatHours(gym.openTime, gym.closeTime)})؛ QR داخل باشگاه را اسکن کنید."
            else "امروز باشگاه فعال نیست. روزهای فعال: ${describeDays(gym.workDays)}",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        // card number
        val card = gym.cardNumber
        if (card != null) {
            AppCard {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    IconBubble(Icons.Rounded.CreditCard, g.tuition)
                    Column(modifier = Modifier.weight(1f)) {
                        Text("شماره کارت برای واریز شهریه", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(formatCard(card), style = MaterialTheme.typography.titleMedium.copy(textDirection = TextDirection.Ltr, letterSpacing = 1.sp))
                    }
                    FilledTonalIconButton(onClick = { clipboard.setText(AnnotatedString(card)); UiEvents.toast("شماره کارت کپی شد") }) {
                        Icon(Icons.Rounded.ContentCopy, contentDescription = "کپی شماره کارت")
                    }
                }
            }
        }

        // body info
        val age = m.birth?.let { ageOn(it, today) }
        if (m.heightCm != null || m.weightKg != null || age != null || !m.goal.isNullOrBlank()) {
            AppCard {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    SectionTitle("مشخصات من")
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Info("قد", m.heightCm?.let { "${it.fa()} cm" } ?: "—", Modifier.weight(1f))
                        Info("وزن", m.weightKg?.let { "${(if (it % 1.0 == 0.0) it.toInt().toString() else it.toString()).fa()} kg" } ?: "—", Modifier.weight(1f))
                        Info("سن", age?.let { "${it.fa()} سال" } ?: "—", Modifier.weight(1f))
                    }
                    if (!m.goal.isNullOrBlank()) Info("هدف", m.goal)
                }
            }
        }

        // program
        AppCard {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionTitle("برنامه تمرینی")
                if (m.program.isNullOrBlank()) {
                    Text("مربی هنوز برنامه‌ای ثبت نکرده است.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    Text(m.program, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        // attendance
        AppCard {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionTitle("حضور در ۳۰ روز اخیر") { ToneBadge("${presentCount.fa()} جلسه", g.attendance) }
                if (m.attendance.isEmpty()) {
                    Text("هنوز حضوری ثبت نشده است.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    m.attendance.forEachIndexed { i, a ->
                        if (i > 0) HorizontalDivider(color = g.border)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val d = runCatching { LocalDate.parse(a.day) }.getOrNull()
                            Text(
                                d?.let { "${WEEKDAY_NAMES[it.jsWeekday()] ?: ""} ${formatJalali(it)}" } ?: a.day,
                                style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f),
                            )
                            if (a.present) ToneBadge("حاضر", g.success, dot = true) else ToneBadge("غایب", g.danger, dot = true)
                        }
                    }
                }
            }
        }

        // payments
        AppCard {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionTitle("تاریخچه پرداخت‌ها")
                if (m.payments.isEmpty()) {
                    Text("پرداختی ثبت نشده است.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    m.payments.forEachIndexed { i, p ->
                        if (i > 0) HorizontalDivider(color = g.border)
                        Column {
                            Text(formatToman(p.amount), style = MaterialTheme.typography.titleSmall)
                            Text(
                                "${formatJalali(p.paidDate)} · ${formatDays(p.durationDays)} · تا ${formatJalali(p.endDate)}",
                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }

        // gym info
        AppCard {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionTitle("اطلاعات باشگاه")
                Info("روزهای فعال", describeDays(gym.workDays))
                Info("ساعت کاری", formatHours(gym.openTime, gym.closeTime))
                Info("مربی", gym.coachName)
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}
