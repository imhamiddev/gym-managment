package dev.imhamid.gymyar.ui.coach

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.imhamid.gymyar.data.CoachHome
import dev.imhamid.gymyar.data.GymInfo
import dev.imhamid.gymyar.data.Me
import dev.imhamid.gymyar.data.PendingDto
import dev.imhamid.gymyar.ui.components.*
import dev.imhamid.gymyar.ui.theme.gym
import dev.imhamid.gymyar.util.*
import kotlinx.coroutines.launch
import kotlinx.serialization.json.put

@Composable
fun CreateGymCard(vm: CoachViewModel) {
    var name by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    AppCard {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionTitle("ساخت باشگاه")
            Text(
                "نام باشگاه را بنویسید؛ بعد از ساخت، یک کد دعوت برای ورزشکاران می‌گیرید.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            AppField(name, { name = it }, "نام باشگاه", imeAction = ImeAction.Done, onDone = {})
            ErrorText(error)
            PrimaryButton("ساخت باشگاه", loading = loading, onClick = {
                loading = true; error = null
                scope.launch {
                    error = vm.act("create_gym", dev.imhamid.gymyar.data.Api.params { put("p_name", name) })
                    loading = false
                }
            })
        }
    }
}

@Composable
private fun RequestRow(p: PendingDto, busy: Boolean, onRespond: (Boolean) -> Unit) {
    val g = MaterialTheme.gym
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        val parts = p.name.split(" ", limit = 2)
        Avatar(parts.firstOrNull() ?: "", parts.getOrElse(1) { "" })
        Column(modifier = Modifier.weight(1f)) {
            Text(p.name, style = MaterialTheme.typography.bodyLarge, maxLines = 1)
            Text(p.phone.fa(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        FilledIconButton(
            onClick = { onRespond(true) }, enabled = !busy,
            colors = IconButtonDefaults.filledIconButtonColors(containerColor = g.success, contentColor = androidx.compose.ui.graphics.Color.White),
        ) { Icon(Icons.Rounded.Check, contentDescription = "تایید") }
        FilledTonalIconButton(
            onClick = { onRespond(false) }, enabled = !busy,
            colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = g.danger.soft(0.14f), contentColor = g.danger),
        ) { Icon(Icons.Rounded.Close, contentDescription = "رد") }
    }
}

@Composable
fun OverviewScreen(me: Me, data: CoachHome, stale: Boolean, vm: CoachViewModel, go: (String) -> Unit) {
    val gym = data.gym
    val g = MaterialTheme.gym
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboardManager.current
    var busy by remember { mutableStateOf(false) }
    val today = remember { todayTehran() }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (stale) Banner("حالت آفلاین: آخرین اطلاعات ذخیره‌شده نمایش داده می‌شود.", g.warning)

        if (gym == null) {
            PageHeader("خوش آمدید، ${me.firstName}", "برای شروع، باشگاه خود را بسازید.", Icons.Rounded.Dashboard)
            CreateGymCard(vm)
            return@Column
        }

        PageHeader(gym.name, "نمای کلی باشگاه", Icons.Rounded.Dashboard)

        val primary = MaterialTheme.colorScheme.primary
        Row(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(primary.copy(alpha = 0.10f)).padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(Modifier.size(48.dp).clip(RoundedCornerShape(16.dp)).background(primary), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.ConfirmationNumber, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text("کد باشگاه برای ورزشکاران", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    gym.code,
                    style = MaterialTheme.typography.headlineSmall.copy(textDirection = TextDirection.Ltr, letterSpacing = 6.sp),
                )
            }
            FilledTonalIconButton(onClick = {
                clipboard.setText(AnnotatedString(gym.code))
                UiEvents.toast("کد کپی شد")
            }) { Icon(Icons.Rounded.ContentCopy, contentDescription = "کپی کد") }
        }

        val members = data.members
        val birthdays = members.filter { isBirthday(it.birth, today) }
        if (birthdays.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(g.attendance.soft(0.1f)).padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                IconBubble(Icons.Rounded.Cake, g.attendance)
                Column {
                    Text("تولد امروز", style = MaterialTheme.typography.titleSmall)
                    Text(birthdays.joinToString("، ") { it.fullName }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        val rem = members.map { it.remaining(today) }
        val active = rem.count { (it ?: 0) > 7 }
        val expiring = rem.count { it != null && it > 0 && it <= 7 }
        val expired = rem.count { it == null || it == 0 }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            StatCard(Icons.Rounded.Groups, g.members, "عضو", members.size, Modifier.weight(1f))
            StatCard(Icons.Rounded.VerifiedUser, g.success, "شهریه فعال", active, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            StatCard(Icons.Rounded.HourglassTop, g.warning, "رو به پایان", expiring, Modifier.weight(1f))
            StatCard(Icons.Rounded.ErrorOutline, g.danger, "منقضی / بدون شهریه", expired, Modifier.weight(1f))
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            QuickAction(Icons.Rounded.EventAvailable, g.attendance, "حضور و غیاب", "ثبت جلسه امروز") { go("attendance") }
            QuickAction(Icons.Rounded.Payments, g.tuition, "شهریه", "ثبت پرداخت جدید") { go("tuition") }
            QuickAction(Icons.Rounded.Groups, g.members, "اعضا", "مشخصات و وضعیت") { go("members") }
        }

        SectionTitle("درخواست‌های عضویت") {
            if (data.pending.isNotEmpty()) ToneBadge(data.pending.size.fa(), g.members)
        }
        AppCard {
            if (data.pending.isEmpty()) {
                EmptyState(Icons.Rounded.PersonAdd, g.members, "درخواستی نیست", "کد باشگاه را برای ورزشکاران بفرستید؛ درخواست‌هایشان اینجا می‌آید.")
            } else {
                data.pending.forEachIndexed { i, p ->
                    if (i > 0) HorizontalDivider(color = g.border)
                    RequestRow(p, busy) { approve ->
                        busy = true
                        scope.launch {
                            val err = vm.act("respond_request", dev.imhamid.gymyar.data.Api.params { put("p_id", p.id); put("p_approve", approve) })
                            if (err != null) UiEvents.toast(err) else UiEvents.toast(if (approve) "عضو تایید شد" else "درخواست رد شد")
                            busy = false
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun QuickAction(icon: androidx.compose.ui.graphics.vector.ImageVector, tone: androidx.compose.ui.graphics.Color, title: String, hint: String, onClick: () -> Unit) {
    AppCard(onClick = onClick) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            IconBubble(icon, tone)
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(hint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Rounded.ChevronLeft, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
