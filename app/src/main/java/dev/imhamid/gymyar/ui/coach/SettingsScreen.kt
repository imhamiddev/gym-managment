package dev.imhamid.gymyar.ui.coach

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import dev.imhamid.gymyar.data.Api
import dev.imhamid.gymyar.data.CoachHome
import dev.imhamid.gymyar.data.CoachItem
import dev.imhamid.gymyar.data.GymInfo
import dev.imhamid.gymyar.data.PlanDto
import dev.imhamid.gymyar.data.Repo
import dev.imhamid.gymyar.data.Res
import dev.imhamid.gymyar.ui.components.*
import dev.imhamid.gymyar.ui.theme.gym
import dev.imhamid.gymyar.util.*
import kotlinx.coroutines.launch
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

@Composable
fun SettingsScreen(data: CoachHome, vm: CoachViewModel) {
    val gym = data.gym ?: return
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        GymSettingsCard(gym, vm)
        PlansCard(data.plans, vm)
        CoachesCard(vm)
        Spacer(Modifier.height(8.dp))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GymSettingsCard(gym: GymInfo, vm: CoachViewModel) {
    val scope = rememberCoroutineScope()
    var name by remember(gym.id) { mutableStateOf(gym.name) }
    var code by remember(gym.id) { mutableStateOf(gym.code) }
    var days by remember(gym.id) { mutableStateOf(gym.workDays.toSet()) }
    var open by remember(gym.id) { mutableStateOf(gym.openTime) }
    var close by remember(gym.id) { mutableStateOf(gym.closeTime) }
    var card by remember(gym.id) { mutableStateOf(gym.cardNumber.orEmpty()) }
    var error by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }
    val cardClean = if (card.isBlank()) null else cleanCard(card)

    AppCard {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionTitle("تنظیمات باشگاه")
            AppField(name, { name = it }, "نام باشگاه")
            AppField(
                code, { code = it.uppercase() }, "کد دعوت (۴ تا ۶ حرف یا عدد)", ltr = true,
                trailing = {
                    TextButton(onClick = {
                        scope.launch {
                            when (val r = Repo.act("suggest_gym_code")) {
                                is Res.Ok -> r.value["code"]?.jsonPrimitive?.contentOrNull?.let { code = it }
                                is Res.Fail -> error = r.message
                            }
                        }
                    }) { Text("کد جدید") }
                },
            )

            Text("روزهای فعال باشگاه", style = MaterialTheme.typography.labelLarge)
            PillTabs(
                DAY_PRESETS.map { it.key to it.label },
                DAY_PRESETS.firstOrNull { p -> p.days.size == days.size && p.days.all { it in days } }?.key,
                { key -> days = DAY_PRESETS.first { it.key == key }.days.toSet() },
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                for (d in WEEK_ORDER) {
                    val on = d in days
                    Box(
                        modifier = Modifier.clip(MaterialTheme.shapes.medium)
                            .background(if (on) MaterialTheme.colorScheme.primary.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { days = if (on) days - d else days + d }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                    ) {
                        Text(
                            WEEKDAY_NAMES.getValue(d), style = MaterialTheme.typography.labelLarge,
                            color = if (on) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                TimeField("ساعت شروع", open, { open = it }, Modifier.weight(1f))
                TimeField("ساعت پایان", close, { close = it }, Modifier.weight(1f))
            }
            AppField(
                card, { card = it }, "شماره کارت برای واریز شهریه (اختیاری)", keyboardType = KeyboardType.Number, ltr = true,
                supporting = cardClean?.let { formatCard(it) } ?: if (card.isBlank()) "ورزشکارها این شماره را در پنل خود می‌بینند." else "شماره کارت باید ۱۶ رقم باشد.",
            )
            ErrorText(error)
            PrimaryButton("ذخیره تنظیمات", loading = saving, onClick = {
                saving = true; error = null
                scope.launch {
                    val err = vm.act("update_gym_settings", Api.params {
                        put("p_name", name)
                        put("p_code", code)
                        put("p_work_days", buildJsonArray { days.sorted().forEach { add(it) } })
                        put("p_open", open)
                        put("p_close", close)
                        put("p_card", card)
                    })
                    if (err != null) error = err else UiEvents.toast("تنظیمات ذخیره شد")
                    saving = false
                }
            })
        }
    }
}

@Composable
private fun PlansCard(plans: List<PlanDto>, vm: CoachViewModel) {
    val g = MaterialTheme.gym
    val scope = rememberCoroutineScope()
    var editing by remember { mutableStateOf<PlanDto?>(null) }
    var creating by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<PlanDto?>(null) }

    AppCard {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionTitle("پلن‌های شهریه")
            Text(
                "پلن‌ها هنگام ثبت شهریه با یک ضربه مبلغ و مدت را پر می‌کنند.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (plans.isEmpty()) {
                EmptyState(Icons.Rounded.Payments, g.tuition, "هنوز پلنی نساخته‌اید")
            } else {
                plans.forEach { p ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(p.name, style = MaterialTheme.typography.titleSmall)
                            Text("${formatToman(p.amount)} · ${formatDays(p.durationDays)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = { editing = p }) { Icon(Icons.Rounded.Edit, contentDescription = "ویرایش") }
                        IconButton(onClick = { deleting = p }) { Icon(Icons.Rounded.Delete, contentDescription = "حذف", tint = g.danger) }
                    }
                }
            }
            SecondaryButton("افزودن پلن", onClick = { creating = true }, icon = Icons.Rounded.Add, modifier = Modifier.fillMaxWidth())
        }
    }

    if (creating || editing != null) {
        PlanDialog(
            plan = editing,
            onDismiss = { creating = false; editing = null },
            onSave = { name, amount, duration ->
                val id = editing?.id
                val err = vm.act("save_plan", Api.params {
                    put("p_name", name); put("p_amount", amount); put("p_duration", duration); put("p_id", id)
                })
                if (err == null) { creating = false; editing = null; UiEvents.toast("پلن ذخیره شد") }
                err
            },
        )
    }
    deleting?.let { p ->
        ConfirmDialog(
            title = "حذف پلن", text = "پلن «${p.name}» حذف شود؟ پرداخت‌های قبلی تغییری نمی‌کنند.", confirmLabel = "حذف",
            onConfirm = {
                deleting = null
                scope.launch {
                    val err = vm.act("delete_plan", Api.params { put("p_id", p.id) })
                    UiEvents.toast(err ?: "پلن حذف شد")
                }
            },
            onDismiss = { deleting = null },
        )
    }
}

@Composable
private fun PlanDialog(plan: PlanDto?, onDismiss: () -> Unit, onSave: suspend (String, Long, Int) -> String?) {
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf(plan?.name.orEmpty()) }
    var amount by remember { mutableStateOf(plan?.amount?.toString().orEmpty()) }
    var duration by remember { mutableIntStateOf(plan?.durationDays ?: 30) }
    var error by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }
    val amountNum = amount.en().filter { it.isDigit() }.toLongOrNull()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (plan == null) "پلن جدید" else "ویرایش پلن") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                AppField(name, { name = it }, "نام پلن (مثلاً ماهانه)")
                AppField(amount, { amount = it }, "مبلغ (تومان)", keyboardType = KeyboardType.Number, ltr = true, supporting = amountNum?.let { formatToman(it) })
                PillTabs(listOf("30" to "۳۰ روز", "60" to "۶۰ روز", "90" to "۹۰ روز"), duration.toString(), { duration = it.toInt() })
                ErrorText(error)
            }
        },
        confirmButton = {
            TextButton(enabled = !saving, onClick = {
                if (amountNum == null || amountNum <= 0) { error = "مبلغ پلن را درست وارد کنید."; return@TextButton }
                saving = true; error = null
                scope.launch {
                    error = onSave(name, amountNum, duration)
                    saving = false
                }
            }) { Text("ذخیره", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف") } },
    )
}

@Composable
private fun CoachesCard(vm: CoachViewModel) {
    val g = MaterialTheme.gym
    val scope = rememberCoroutineScope()
    var coaches by remember { mutableStateOf<List<CoachItem>>(emptyList()) }
    var reload by remember { mutableIntStateOf(0) }
    var phone by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var removing by remember { mutableStateOf<CoachItem?>(null) }

    LaunchedEffect(reload) {
        when (val r = Repo.coachCoaches()) {
            is Res.Ok -> coaches = r.value.coaches
            is Res.Fail -> error = r.message
        }
    }

    AppCard {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionTitle("مربیان کمکی")
            Text(
                "مربی کمکی می‌تواند اعضا، شهریه و حضور و غیاب را مدیریت کند، اما به تنظیمات باشگاه دسترسی ندارد. مربی باید قبلاً با نقش «مربی» ثبت‌نام کرده باشد.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            coaches.forEach { c ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    val parts = c.name.split(" ", limit = 2)
                    Avatar(parts.firstOrNull().orEmpty(), parts.getOrElse(1) { "" }, size = 40.dp)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(c.name, style = MaterialTheme.typography.bodyLarge)
                        Text(c.phone.fa(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = { removing = c }) { Icon(Icons.Rounded.PersonRemove, contentDescription = "حذف مربی", tint = g.danger) }
                }
            }
            AppField(phone, { phone = it }, "شماره‌ی مربی", keyboardType = KeyboardType.Phone, ltr = true, placeholder = "09121234567")
            ErrorText(error)
            PrimaryButton("افزودن مربی", loading = busy, icon = Icons.Rounded.PersonAdd, onClick = {
                busy = true; error = null
                scope.launch {
                    val err = vm.act("add_coach", Api.params { put("p_phone", phone) })
                    if (err != null) error = err else { phone = ""; reload++; UiEvents.toast("مربی اضافه شد") }
                    busy = false
                }
            })
        }
    }

    removing?.let { c ->
        ConfirmDialog(
            title = "حذف مربی", text = "«${c.name}» از مربیان کمکی حذف شود؟", confirmLabel = "حذف",
            onConfirm = {
                removing = null
                scope.launch {
                    val err = vm.act("remove_coach", Api.params { put("p_user", c.userId) })
                    if (err != null) error = err else { reload++; UiEvents.toast("مربی حذف شد") }
                }
            },
            onDismiss = { removing = null },
        )
    }
}
