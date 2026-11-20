package dev.imhamid.gymyar.ui.coach

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.EventAvailable
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.imhamid.gymyar.data.Api
import dev.imhamid.gymyar.data.CoachHome
import dev.imhamid.gymyar.data.Repo
import dev.imhamid.gymyar.data.Res
import dev.imhamid.gymyar.ui.components.AppCard
import dev.imhamid.gymyar.ui.components.Avatar
import dev.imhamid.gymyar.ui.components.Banner
import dev.imhamid.gymyar.ui.components.EmptyState
import dev.imhamid.gymyar.ui.components.ErrorText
import dev.imhamid.gymyar.ui.components.IconBubble
import dev.imhamid.gymyar.ui.components.InkButton
import dev.imhamid.gymyar.ui.components.PageHeader
import dev.imhamid.gymyar.ui.components.PrimaryButton
import dev.imhamid.gymyar.ui.components.SkeletonBox
import dev.imhamid.gymyar.ui.components.ToneBadge
import dev.imhamid.gymyar.ui.components.UiEvents
import dev.imhamid.gymyar.ui.components.WeekStrip
import dev.imhamid.gymyar.ui.components.soft
import dev.imhamid.gymyar.ui.theme.gym
import dev.imhamid.gymyar.util.WEEKDAY_NAMES
import dev.imhamid.gymyar.util.describeDays
import dev.imhamid.gymyar.util.fa
import dev.imhamid.gymyar.util.formatJalaliLong
import dev.imhamid.gymyar.util.formatHours
import dev.imhamid.gymyar.util.isGymDay
import dev.imhamid.gymyar.util.jsWeekday
import dev.imhamid.gymyar.util.nextGymDay
import dev.imhamid.gymyar.util.todayTehran
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.time.LocalDate

@Composable
private fun PresenceToggle(present: Boolean, enabled: Boolean, onChange: (Boolean) -> Unit) {
    val g = MaterialTheme.gym
    Row(
        modifier = Modifier.clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant).padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        @Composable
        fun Option(label: String, icon: ImageVector, on: Boolean, activeBg: Color, activeFg: Color, value: Boolean) {
            Row(
                modifier = Modifier.clip(CircleShape)
                    .background(if (on) activeBg else Color.Transparent)
                    .clickable(enabled = enabled) { onChange(value) }
                    .padding(horizontal = 14.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                if (on) Icon(icon, contentDescription = null, tint = activeFg, modifier = Modifier.size(14.dp))
                Text(
                    label, style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (on) FontWeight.Bold else FontWeight.Medium,
                    color = if (on) activeFg else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Option("حاضر", Icons.Rounded.Check, present, g.lime, g.onLime, true)
        Option("غایب", Icons.Rounded.Close, !present, g.danger.soft(0.16f), g.danger, false)
    }
}

/** Shown instead of the member list on days the gym is closed: attendance cannot be recorded. */
@Composable
private fun LockedDay(day: LocalDate, today: LocalDate, workDays: List<Int>, openTime: String, closeTime: String, onJumpToOpenDay: () -> Unit) {
    val g = MaterialTheme.gym
    AppCard {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            IconBubble(Icons.Rounded.Lock, g.warning, size = 68.dp, iconSize = 32.dp)
            Text("حضور و غیاب قفل است", style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
            Text(
                if (day == today) "امروز باشگاه تعطیل است؛ حضور و غیاب فقط در روزهای کاری باز می‌شود."
                else "باشگاه در این روز فعال نیست؛ حضور و غیاب فقط در روزهای کاری ثبت می‌شود.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center,
            )
            Text(
                "روزهای فعال: ${describeDays(workDays)}\nساعت کاری: ${formatHours(openTime, closeTime)}",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center,
            )
            if (workDays.isNotEmpty()) {
                Spacer(Modifier.size(4.dp))
                InkButton("رفتن به آخرین روز کاری", onClick = onJumpToOpenDay)
            }
        }
    }
}

@Composable
fun AttendanceScreen(data: CoachHome, stale: Boolean, vm: CoachViewModel) {
    val g = MaterialTheme.gym
    val gym = data.gym ?: return
    val scope = rememberCoroutineScope()
    val today = remember { todayTehran() }
    val lastOpen = remember(gym.workDays) { if (isGymDay(gym.workDays, today)) today else nextGymDay(gym.workDays, today, -1) }

    // The screen always starts on today. If the gym is closed today, it is locked.
    var day by remember { mutableStateOf(today) }
    var marks by remember { mutableStateOf<Map<String, Boolean>>(emptyMap()) }
    var loadedFor by remember { mutableStateOf<LocalDate?>(null) }
    var dirty by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val members = remember(data.members) { CoachViewModel.sortByName(data.members) { "${it.lastName} ${it.firstName}" } }
    val open = isGymDay(gym.workDays, day)

    LaunchedEffect(day, members.size, open) {
        loadedFor = null; dirty = false; error = null
        if (open) {
            when (val r = Repo.attendance(day.toString())) {
                is Res.Ok -> marks = r.value.records.associate { it.membershipId to it.present }
                is Res.Fail -> { marks = emptyMap(); error = r.message }
            }
        } else marks = emptyMap()
        loadedFor = day
    }

    val presentCount = members.count { marks[it.id] == true }
    val allPresent = members.isNotEmpty() && presentCount == members.size

    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (stale) Banner("حالت آفلاین: آخرین اطلاعات ذخیره‌شده نمایش داده می‌شود.", g.warning)
            PageHeader("حضور و غیاب", formatJalaliLong(day), Icons.Rounded.EventAvailable)

            AppCard {
                Row(
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = { day = day.minusDays(7) }, modifier = Modifier.size(32.dp)) { Icon(Icons.Rounded.ChevronRight, contentDescription = "هفته قبل") }
                    Box(Modifier.weight(1f)) {
                        WeekStrip(selected = day, weekOf = day, workDays = gym.workDays, today = today, onSelect = { day = it })
                    }
                    IconButton(onClick = { day = day.plusDays(7) }, modifier = Modifier.size(32.dp)) { Icon(Icons.Rounded.ChevronLeft, contentDescription = "هفته بعد") }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ToneBadge(WEEKDAY_NAMES[day.jsWeekday()] ?: "", if (open) g.success else g.warning, dot = true)
                if (day == today) ToneBadge("امروز", g.accent)
                if (!open) ToneBadge("تعطیل", g.warning)
                Spacer(Modifier.weight(1f))
                if (day != today) {
                    Text(
                        "برگشت به امروز",
                        style = MaterialTheme.typography.labelMedium, color = g.accent, fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { day = today },
                    )
                }
            }
            ErrorText(error)

            when {
                !open -> LockedDay(day, today, gym.workDays, gym.openTime, gym.closeTime) { day = lastOpen }
                members.isEmpty() -> AppCard { EmptyState(Icons.Rounded.Groups, g.members, "عضوی برای ثبت نیست", "اول درخواست ورزشکاران را تایید کنید.") }
                loadedFor != day -> repeat(4) { SkeletonBox(Modifier.fillMaxWidth(), 60.dp) }
                else -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("${presentCount.fa()} حاضر از ${members.size.fa()} نفر", style = MaterialTheme.typography.titleSmall)
                        Spacer(Modifier.weight(1f))
                        TextButton(onClick = {
                            marks = members.associate { it.id to !allPresent }; dirty = true
                        }) { Text(if (allPresent) "همه غایب" else "همه حاضر", color = g.accent, fontWeight = FontWeight.Bold) }
                    }
                    AppCard {
                        members.forEachIndexed { i, m ->
                            if (i > 0) HorizontalDivider(color = g.border)
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Avatar(m.firstName, m.lastName, size = 40.dp)
                                Text(m.fullName, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f), maxLines = 1)
                                PresenceToggle(marks[m.id] == true, enabled = true) { v ->
                                    marks = marks + (m.id to v); dirty = true
                                }
                            }
                        }
                    }
                }
            }
        }

        if (open && members.isNotEmpty() && loadedFor == day) {
            Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
                PrimaryButton(
                    if (dirty) "ذخیره حضور و غیاب" else "ذخیره‌شده", loading = saving, enabled = dirty,
                    onClick = {
                        saving = true; error = null
                        scope.launch {
                            val records = buildJsonArray {
                                for (m in members) add(buildJsonObject { put("membership_id", m.id); put("present", marks[m.id] == true) })
                            }
                            val err = vm.act("save_attendance", Api.params { put("p_day", day.toString()); put("p_records", records) })
                            if (err != null) error = err else {
                                dirty = false
                                UiEvents.toast("حضور و غیاب ذخیره شد (${presentCount.fa()} از ${members.size.fa()})")
                            }
                            saving = false
                        }
                    },
                )
            }
        }
    }
}
