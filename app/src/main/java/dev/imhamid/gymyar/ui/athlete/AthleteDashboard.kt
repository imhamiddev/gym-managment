package dev.imhamid.gymyar.ui.athlete

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cake
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.EventAvailable
import androidx.compose.material.icons.rounded.EventBusy
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Receipt
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.imhamid.gymyar.data.AthleteMembership
import dev.imhamid.gymyar.data.Me
import dev.imhamid.gymyar.ui.components.AppCard
import dev.imhamid.gymyar.ui.components.Avatar
import dev.imhamid.gymyar.ui.components.Banner
import dev.imhamid.gymyar.ui.components.EmptyState
import dev.imhamid.gymyar.ui.components.IconBubble
import dev.imhamid.gymyar.ui.components.InfoTile
import dev.imhamid.gymyar.ui.components.LimeIconButton
import dev.imhamid.gymyar.ui.components.PageHeader
import dev.imhamid.gymyar.ui.components.PillTabs
import dev.imhamid.gymyar.ui.components.PrimaryButton
import dev.imhamid.gymyar.ui.components.RemainingBadge
import dev.imhamid.gymyar.ui.components.ScreenColumn
import dev.imhamid.gymyar.ui.components.SectionTitle
import dev.imhamid.gymyar.ui.components.ToneBadge
import dev.imhamid.gymyar.ui.components.UiEvents
import dev.imhamid.gymyar.ui.components.WeekStrip
import dev.imhamid.gymyar.ui.components.soft
import dev.imhamid.gymyar.ui.theme.gym
import dev.imhamid.gymyar.util.WEEKDAY_NAMES
import dev.imhamid.gymyar.util.ageOn
import dev.imhamid.gymyar.util.describeDays
import dev.imhamid.gymyar.util.fa
import dev.imhamid.gymyar.util.formatCard
import dev.imhamid.gymyar.util.formatDays
import dev.imhamid.gymyar.util.formatHours
import dev.imhamid.gymyar.util.formatJalali
import dev.imhamid.gymyar.util.formatJalaliLong
import dev.imhamid.gymyar.util.formatToman
import dev.imhamid.gymyar.util.isBirthday
import dev.imhamid.gymyar.util.isGymDay
import dev.imhamid.gymyar.util.jsWeekday
import dev.imhamid.gymyar.util.remainingDays
import dev.imhamid.gymyar.util.todayTehran
import java.time.LocalDate

// ------------------------------------------------------------------ shared pieces

@Composable
private fun mutedColor() = MaterialTheme.colorScheme.onSurfaceVariant

/** Label on top, value under it. Used for facts that can be long (work days...). */
@Composable
private fun InfoLine(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth().padding(vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = mutedColor())
        Text(value, style = MaterialTheme.typography.titleSmall)
    }
}

private fun toneFor(remaining: Int?, danger: androidx.compose.ui.graphics.Color, warning: androidx.compose.ui.graphics.Color, success: androidx.compose.ui.graphics.Color) =
    when {
        remaining == null || remaining == 0 -> danger
        remaining <= 7 -> warning
        else -> success
    }

/** Big "days left" card with a progress bar. */
@Composable
private fun TuitionStatusCard(m: AthleteMembership, today: LocalDate) {
    val g = MaterialTheme.gym
    val remaining = remainingDays(m.payments.map { it.paidDate to it.durationDays }, today)
    val latest = m.payments.maxByOrNull { it.endDate }
    val fraction = if (latest != null && remaining != null && latest.durationDays > 0) (remaining.toFloat() / latest.durationDays).coerceIn(0f, 1f) else 0f
    val tone = toneFor(remaining, g.danger, g.warning, g.success)
    val animated by animateFloatAsState(fraction, tween(800, easing = FastOutSlowInEasing), label = "tuition-progress")

    Column(
        modifier = Modifier.fillMaxWidth().clip(MaterialTheme.shapes.large).background(tone.soft(0.13f)).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("وضعیت شهریه", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            RemainingBadge(remaining)
        }
        if (remaining == null) {
            Text("هنوز شهریه‌ای برای شما ثبت نشده است. با مربی هماهنگ کنید.", style = MaterialTheme.typography.bodyMedium, color = mutedColor())
        } else {
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(remaining.fa(), style = MaterialTheme.typography.displaySmall, color = tone)
                Text("روز مانده", style = MaterialTheme.typography.bodyMedium, color = mutedColor(), modifier = Modifier.padding(bottom = 8.dp))
            }
            LinearProgressIndicator(
                progress = { animated }, color = tone, trackColor = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(50)),
            )
            if (latest != null) {
                Text("اعتبار تا ${formatJalaliLong(latest.endDate)}", style = MaterialTheme.typography.bodySmall, color = mutedColor())
            }
        }
    }
}

// ------------------------------------------------------------------ home

@Composable
fun AthleteHome(me: Me, m: AthleteMembership, stale: Boolean, onScan: () -> Unit) {
    val g = MaterialTheme.gym
    val today = remember { todayTehran() }
    val gym = m.gym
    val presentDays = remember(m.attendance) {
        m.attendance.filter { it.present }.mapNotNull { runCatching { LocalDate.parse(it.day) }.getOrNull() }.toSet()
    }
    val presentCount = presentDays.size
    val openToday = isGymDay(gym.workDays, today)
    val last = m.payments.firstOrNull()

    ScreenColumn {
        if (stale) Banner("حالت آفلاین: آخرین اطلاعات ذخیره‌شده نمایش داده می‌شود.", g.warning)

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Avatar(me.firstName, me.lastName, size = 52.dp)
            Column(modifier = Modifier.weight(1f)) {
                Text("سلام ${me.firstName}", style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${gym.name} · مربی: ${gym.coachName}", style = MaterialTheme.typography.bodySmall, color = mutedColor(), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }

        if (isBirthday(m.birth, today)) {
            Row(
                modifier = Modifier.fillMaxWidth().clip(MaterialTheme.shapes.large).background(g.attendance.soft(0.1f)).padding(14.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                IconBubble(Icons.Rounded.Cake, g.attendance)
                Text("تولدت مبارک! 🎉", style = MaterialTheme.typography.titleSmall)
            }
        }

        AppCard {
            Column(Modifier.padding(horizontal = 8.dp, vertical = 10.dp)) {
                WeekStrip(selected = today, weekOf = today, workDays = gym.workDays, marked = presentDays, today = today)
            }
        }

        TuitionStatusCard(m, today)

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            InfoTile(Icons.Rounded.EventAvailable, g.members, "حضور در ۳۰ روز اخیر", presentCount.fa(), Modifier.weight(1f).fillMaxHeight(), unit = "جلسه")
            InfoTile(
                Icons.Rounded.Payments, g.warning, "آخرین پرداخت",
                last?.let { formatToman(it.amount).removeSuffix(" تومان") } ?: "—",
                Modifier.weight(1f).fillMaxHeight(),
                unit = if (last != null) "تومان" else null,
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            PrimaryButton("ثبت حضور با QR", onClick = onScan, icon = Icons.Rounded.QrCodeScanner)
            Text(
                if (openToday) "امروز باشگاه فعال است (${formatHours(gym.openTime, gym.closeTime)})؛ QR داخل باشگاه را اسکن کنید."
                else "امروز باشگاه فعال نیست. روزهای فعال: ${describeDays(gym.workDays)}",
                style = MaterialTheme.typography.bodySmall, color = mutedColor(),
            )
        }
    }
}

// ------------------------------------------------------------------ attendance tab

@Composable
fun AthleteAttendance(m: AthleteMembership, stale: Boolean) {
    val g = MaterialTheme.gym
    val present = m.attendance.count { it.present }
    val absent = m.attendance.size - present

    ScreenColumn {
        if (stale) Banner("حالت آفلاین: آخرین اطلاعات ذخیره‌شده نمایش داده می‌شود.", g.warning)
        PageHeader("حضور و غیاب", "۳۰ روز اخیر", Icons.Rounded.EventAvailable)

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            InfoTile(Icons.Rounded.EventAvailable, g.success, "حاضر", present.fa(), Modifier.weight(1f).fillMaxHeight(), unit = "جلسه")
            InfoTile(Icons.Rounded.EventBusy, g.danger, "غایب", absent.fa(), Modifier.weight(1f).fillMaxHeight(), unit = "جلسه")
        }

        AppCard {
            if (m.attendance.isEmpty()) {
                EmptyState(Icons.Rounded.EventAvailable, g.success, "هنوز حضوری ثبت نشده", "با اسکن QR باشگاه، حضورتان اینجا ثبت می‌شود.")
            } else {
                Column(Modifier.padding(horizontal = 16.dp)) {
                    m.attendance.forEachIndexed { i, a ->
                        if (i > 0) HorizontalDivider(color = g.border)
                        Row(Modifier.padding(vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
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
    }
}

// ------------------------------------------------------------------ gym info + tuition tab

@Composable
fun AthleteClub(m: AthleteMembership, stale: Boolean) {
    val g = MaterialTheme.gym
    val gym = m.gym
    val today = remember { todayTehran() }
    var tab by rememberSaveable { mutableStateOf("club") }

    ScreenColumn {
        if (stale) Banner("حالت آفلاین: آخرین اطلاعات ذخیره‌شده نمایش داده می‌شود.", g.warning)
        PageHeader(gym.name, "اطلاعات باشگاه و شهریه", Icons.Rounded.FitnessCenter)
        PillTabs(listOf("club" to "اطلاعات باشگاه", "tuition" to "شهریه"), tab, { tab = it })

        if (tab == "club") {
            val openToday = isGymDay(gym.workDays, today)
            AppCard {
                Column(Modifier.padding(horizontal = 18.dp, vertical = 6.dp)) {
                    InfoLine("مربی اصلی", gym.coachName)
                    HorizontalDivider(color = g.border)
                    val others = gym.coaches.filter { !it.isMain }
                    if (others.isNotEmpty()) {
                        InfoLine(if (others.size == 1) "مربی دیگر" else "مربیان دیگر", others.joinToString("، ") { it.name })
                        HorizontalDivider(color = g.border)
                    }
                    InfoLine("روزهای فعال", describeDays(gym.workDays))
                    HorizontalDivider(color = g.border)
                    InfoLine("ساعت کاری", formatHours(gym.openTime, gym.closeTime))
                    HorizontalDivider(color = g.border)
                    Row(Modifier.fillMaxWidth().padding(vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("امروز", style = MaterialTheme.typography.labelMedium, color = mutedColor(), modifier = Modifier.weight(1f))
                        if (openToday) ToneBadge("باشگاه فعال است", g.success, dot = true) else ToneBadge("باشگاه تعطیل است", g.danger, dot = true)
                    }
                }
            }
        } else {
            TuitionStatusCard(m, today)

            val card = gym.cardNumber
            if (card != null) {
                val clipboard = LocalClipboardManager.current
                AppCard {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        IconBubble(Icons.Rounded.CreditCard, g.tuition)
                        Column(modifier = Modifier.weight(1f)) {
                            Text("شماره کارت برای واریز شهریه", style = MaterialTheme.typography.bodySmall, color = mutedColor())
                            Text(formatCard(card), style = MaterialTheme.typography.titleMedium.copy(textDirection = TextDirection.Ltr, letterSpacing = 1.sp))
                        }
                        LimeIconButton(Icons.Rounded.ContentCopy, "کپی شماره کارت", {
                            clipboard.setText(AnnotatedString(card)); UiEvents.toast("شماره کارت کپی شد")
                        })
                    }
                }
            }

            SectionTitle("تاریخچه پرداخت‌ها") { if (m.payments.isNotEmpty()) ToneBadge(m.payments.size.fa(), g.tuition) }
            AppCard {
                if (m.payments.isEmpty()) {
                    EmptyState(Icons.Rounded.Receipt, g.tuition, "پرداختی ثبت نشده", "بعد از ثبت شهریه توسط مربی، اینجا می‌بینید.")
                } else {
                    Column(Modifier.padding(horizontal = 16.dp)) {
                        m.payments.forEachIndexed { i, p ->
                            if (i > 0) HorizontalDivider(color = g.border)
                            Column(Modifier.padding(vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(formatToman(p.amount), style = MaterialTheme.typography.titleSmall)
                                Text(
                                    "${formatJalali(p.paidDate)} · ${formatDays(p.durationDays)} · تا ${formatJalali(p.endDate)}",
                                    style = MaterialTheme.typography.bodySmall, color = mutedColor(),
                                )
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(4.dp))
    }
}

// ------------------------------------------------------------------ profile extra

/** Height / weight / age, shown on the profile page when the coach has entered them. */
@Composable
fun AthleteBodyCard(m: AthleteMembership) {
    val today = remember { todayTehran() }
    val age = m.birth?.let { ageOn(it, today) }
    if (m.heightCm == null && m.weightKg == null && age == null) return
    AppCard {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionTitle("مشخصات بدنی")
            Row(Modifier.fillMaxWidth()) {
                BodyStat("قد", m.heightCm?.let { "${it.fa()} cm" } ?: "—", Modifier.weight(1f))
                BodyStat("وزن", m.weightKg?.let { "${(if (it % 1.0 == 0.0) it.toInt().toString() else it.toString()).fa()} kg" } ?: "—", Modifier.weight(1f))
                BodyStat("سن", age?.let { "${it.fa()} سال" } ?: "—", Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun BodyStat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = mutedColor())
        Text(value, style = MaterialTheme.typography.titleSmall)
    }
}
