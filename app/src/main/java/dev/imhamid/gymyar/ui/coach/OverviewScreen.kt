package dev.imhamid.gymyar.ui.coach

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import kotlinx.coroutines.delay
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
import androidx.compose.foundation.shape.CircleShape
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
            colors = IconButtonDefaults.filledIconButtonColors(containerColor = g.lime, contentColor = g.onLime),
        ) { Icon(Icons.Rounded.Check, contentDescription = "تایید") }
        FilledTonalIconButton(
            onClick = { onRespond(false) }, enabled = !busy,
            colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = g.danger.soft(0.14f), contentColor = g.danger),
        ) { Icon(Icons.Rounded.Close, contentDescription = "رد") }
    }
}

@Composable
fun OverviewScreen(me: Me, data: CoachHome, stale: Boolean, vm: CoachViewModel) {
    val gym = data.gym
    val g = MaterialTheme.gym
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboardManager.current
    var busy by remember { mutableStateOf(false) }
    var copied by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current
    LaunchedEffect(copied) { if (copied) { delay(1600); copied = false } }
    val today = remember { todayTehran() }

    ScreenColumn {
        if (stale) Banner("حالت آفلاین: آخرین اطلاعات ذخیره‌شده نمایش داده می‌شود.", g.warning)

        if (gym == null) {
            PageHeader("خوش آمدید، ${me.firstName}", "برای شروع، باشگاه خود را بسازید.", Icons.Rounded.Dashboard)
            CreateGymCard(vm)
            return@ScreenColumn
        }

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Avatar(me.firstName, me.lastName, size = 52.dp)
            Column(modifier = Modifier.weight(1f)) {
                Text(gym.name, style = MaterialTheme.typography.titleLarge, maxLines = 1)
                Text("سلام ${me.firstName}، خوش آمدید", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            }
        }

        // invite code: the one lime block of the page
        Row(
            modifier = Modifier.fillMaxWidth().clip(MaterialTheme.shapes.large).background(g.lime).padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("کد باشگاه برای ورزشکاران", style = MaterialTheme.typography.bodySmall, color = g.onLime.copy(alpha = 0.7f))
                Text(
                    gym.code,
                    style = MaterialTheme.typography.headlineSmall.copy(textDirection = TextDirection.Ltr, letterSpacing = 6.sp),
                    color = g.onLime,
                )
            }
            Box(
                modifier = Modifier.size(46.dp).clip(CircleShape).background(g.ink).clickable {
                    clipboard.setText(AnnotatedString(gym.code))
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    copied = true
                    UiEvents.toast("کد کپی شد")
                },
                contentAlignment = Alignment.Center,
            ) {
                AnimatedContent(
                    targetState = copied,
                    transitionSpec = { (scaleIn(spring(Spring.DampingRatioMediumBouncy)) + fadeIn()) togetherWith (scaleOut() + fadeOut()) },
                    label = "copy-icon",
                ) { done ->
                    if (done) Icon(Icons.Rounded.Check, contentDescription = "کپی شد", tint = g.lime)
                    else Icon(Icons.Rounded.ContentCopy, contentDescription = "کپی کد", tint = g.onInk)
                }
            }
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
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            StatCard(Icons.Rounded.Groups, g.members, "عضو", members.size, Modifier.weight(1f).fillMaxHeight())
            StatCard(Icons.Rounded.VerifiedUser, g.success, "شهریه فعال", active, Modifier.weight(1f).fillMaxHeight())
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            StatCard(Icons.Rounded.HourglassTop, g.warning, "رو به پایان", expiring, Modifier.weight(1f).fillMaxHeight())
            StatCard(Icons.Rounded.ErrorOutline, g.danger, "منقضی / بدون شهریه", expired, Modifier.weight(1f).fillMaxHeight())
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
