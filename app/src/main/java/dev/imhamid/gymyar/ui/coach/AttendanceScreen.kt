package dev.imhamid.gymyar.ui.coach

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.imhamid.gymyar.data.Api
import dev.imhamid.gymyar.data.CoachHome
import dev.imhamid.gymyar.data.Repo
import dev.imhamid.gymyar.data.Res
import dev.imhamid.gymyar.ui.components.*
import dev.imhamid.gymyar.ui.theme.gym
import dev.imhamid.gymyar.util.*
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
        fun Option(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, on: Boolean, activeBg: Color, activeFg: Color, value: Boolean) {
            Row(
                modifier = Modifier.clip(CircleShape)
                    .background(if (on) activeBg else Color.Transparent)
                    .clickable(enabled = enabled) { onChange(value) }
                    .padding(horizontal = 14.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                if (on) Icon(icon, contentDescription = null, tint = activeFg, modifier = Modifier.size(14.dp))
                Text(label, style = MaterialTheme.typography.labelMedium, color = if (on) activeFg else MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Option("حاضر", Icons.Rounded.Check, present, g.success, Color.White, true)
        Option("غایب", Icons.Rounded.Close, !present, g.danger.soft(0.16f), g.danger, false)
    }
}

@Composable
fun AttendanceScreen(data: CoachHome, stale: Boolean, vm: CoachViewModel) {
    val g = MaterialTheme.gym
    val gym = data.gym ?: return
    val scope = rememberCoroutineScope()
    val today = remember { todayTehran() }
    val lastOpen = remember(gym.workDays) { if (isGymDay(gym.workDays, today)) today else nextGymDay(gym.workDays, today, -1) }

    var day by remember { mutableStateOf(lastOpen) }
    var marks by remember { mutableStateOf<Map<String, Boolean>>(emptyMap()) }
    var loadedFor by remember { mutableStateOf<LocalDate?>(null) }
    var dirty by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val members = remember(data.members) { CoachViewModel.sortByName(data.members) { "${it.lastName} ${it.firstName}" } }
    val open = isGymDay(gym.workDays, day)

    LaunchedEffect(day, members.size) {
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
            PageHeader("حضور و غیاب", "روزهای فعال: ${describeDays(gym.workDays)}", Icons.Rounded.EventAvailable, g.attendance)

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(onClick = { day = nextGymDay(gym.workDays, day, -1) }) { Icon(Icons.Rounded.ChevronRight, contentDescription = "جلسه قبل") }
                DateField("تاریخ", day, { day = it }, modifier = Modifier.weight(1f))
                IconButton(onClick = { day = nextGymDay(gym.workDays, day, 1) }) { Icon(Icons.Rounded.ChevronLeft, contentDescription = "جلسه بعد") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ToneBadge(WEEKDAY_NAMES[day.jsWeekday()] ?: "", g.attendance)
                if (day == today) ToneBadge("امروز", MaterialTheme.colorScheme.primary)
                if (day != lastOpen) {
                    Text(
                        "رفتن به آخرین جلسه",
                        style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable { day = lastOpen },
                    )
                }
            }
            ErrorText(error)

            when {
                !open -> AppCard { EmptyState(Icons.Rounded.EventBusy, g.attendance, "باشگاه در این روز فعال نیست", "حضور و غیاب فقط برای روزهای کاری ثبت می‌شود.") }
                members.isEmpty() -> AppCard { EmptyState(Icons.Rounded.Groups, g.attendance, "عضوی برای ثبت نیست", "اول درخواست ورزشکاران را تایید کنید.") }
                loadedFor != day -> repeat(4) { SkeletonBox(Modifier.fillMaxWidth(), 60.dp) }
                else -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("${presentCount.fa()} حاضر از ${members.size.fa()} نفر", style = MaterialTheme.typography.titleSmall)
                        Spacer(Modifier.weight(1f))
                        TextButton(onClick = {
                            marks = members.associate { it.id to !allPresent }; dirty = true
                        }) { Text(if (allPresent) "همه غایب" else "همه حاضر") }
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
            Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 6.dp) {
                Box(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    PrimaryButton(
                        if (dirty) "ذخیره حضور و غیاب" else "ذخیره‌شده", loading = saving, enabled = dirty,
                        container = g.attendance, content = Color.White,
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
}
