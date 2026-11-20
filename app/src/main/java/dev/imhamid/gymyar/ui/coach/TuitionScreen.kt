package dev.imhamid.gymyar.ui.coach

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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import dev.imhamid.gymyar.data.Api
import dev.imhamid.gymyar.data.CoachHome
import dev.imhamid.gymyar.data.MemberDto
import dev.imhamid.gymyar.data.PaymentDto
import dev.imhamid.gymyar.data.PlanDto
import dev.imhamid.gymyar.ui.components.*
import dev.imhamid.gymyar.ui.theme.gym
import dev.imhamid.gymyar.util.*
import kotlinx.coroutines.launch
import kotlinx.serialization.json.put
import java.time.LocalDate

@Composable
fun TuitionScreen(data: CoachHome, stale: Boolean, vm: CoachViewModel) {
    val g = MaterialTheme.gym
    val today = remember { todayTehran() }
    var q by remember { mutableStateOf("") }
    var selectedId by remember { mutableStateOf<String?>(null) }

    val allPayments = data.members.flatMap { it.payments }
    val total = allPayments.sumOf { it.amount }
    val sorted = remember(data.members) { CoachViewModel.sortByName(data.members) { "${it.lastName} ${it.firstName}" } }
    val list = sorted.filter { it.fullName.contains(q.trim()) }
    val selected = data.members.firstOrNull { it.id == selectedId }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        if (stale) Banner("حالت آفلاین: آخرین اطلاعات ذخیره‌شده نمایش داده می‌شود.", g.warning)
        PageHeader("مدیریت شهریه", "ثبت و ویرایش پرداخت‌ها", Icons.Rounded.Payments, g.tuition)

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            AppCard(Modifier.weight(1f)) {
                Column(Modifier.padding(14.dp)) {
                    Text("پرداخت ثبت‌شده", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(allPayments.size.fa(), style = MaterialTheme.typography.titleLarge)
                }
            }
            AppCard(Modifier.weight(1.4f)) {
                Column(Modifier.padding(14.dp)) {
                    Text("مجموع مبلغ", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(formatToman(total), style = MaterialTheme.typography.titleMedium, maxLines = 1)
                }
            }
        }

        SectionTitle("اعضای باشگاه")
        if (data.members.isNotEmpty()) SearchField(q, { q = it }, "جستجوی نام یا نام خانوادگی")
        AppCard {
            when {
                data.members.isEmpty() -> EmptyState(Icons.Rounded.Payments, g.tuition, "ابتدا یک عضو لازم است", "حداقل یک ورزشکار را تایید کنید، بعد شهریه ثبت کنید.")
                list.isEmpty() -> EmptyState(Icons.Rounded.Search, g.tuition, "عضوی پیدا نشد", "نام دیگری را جستجو کنید.")
                else -> list.forEachIndexed { i, m ->
                    if (i > 0) HorizontalDivider(color = g.border)
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { selectedId = m.id }.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Avatar(m.firstName, m.lastName)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(m.fullName, style = MaterialTheme.typography.bodyLarge, maxLines = 1)
                            Text(
                                m.lastPayment?.let { "آخرین پرداخت: ${formatToman(it.amount)}" } ?: "هنوز شهریه‌ای ثبت نشده",
                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1,
                            )
                        }
                        RemainingBadge(m.remaining(today))
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
    }

    if (selected != null) TuitionSheet(member = selected, plans = data.plans, vm = vm, onClose = { selectedId = null })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TuitionSheet(member: MemberDto, plans: List<PlanDto>, vm: CoachViewModel, onClose: () -> Unit) {
    val g = MaterialTheme.gym
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val last = member.lastPayment

    var editingId by remember(member.id) { mutableStateOf<String?>(null) }
    var amount by remember(member.id) { mutableStateOf(last?.amount?.toString().orEmpty()) }
    var date by remember(member.id) { mutableStateOf(todayTehran()) }
    var duration by remember(member.id) { mutableIntStateOf(last?.durationDays ?: 30) }
    var error by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<PaymentDto?>(null) }

    val amountNum = amount.en().filter { it.isDigit() }.toLongOrNull()

    fun resetForm() {
        editingId = null
        amount = member.lastPayment?.amount?.toString().orEmpty()
        date = todayTehran()
        duration = member.lastPayment?.durationDays ?: 30
        error = null
    }

    ModalBottomSheet(onDismissRequest = onClose, sheetState = sheetState, containerColor = MaterialTheme.colorScheme.surface) {
        Column(
            modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).imePadding().padding(horizontal = 20.dp).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Avatar(member.firstName, member.lastName)
                Column(modifier = Modifier.weight(1f)) {
                    Text(member.fullName, style = MaterialTheme.typography.titleMedium)
                    Text(member.phone.fa(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                RemainingBadge(member.remaining(todayTehran()))
            }

            SectionTitle(if (editingId != null) "ویرایش پرداخت" else "ثبت شهریه جدید")
            if (plans.isNotEmpty() && editingId == null) {
                Text("پلن‌ها", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                PillTabs(
                    plans.map { it.id to "${it.name} · ${formatToman(it.amount)}" }, null,
                    { id -> plans.firstOrNull { it.id == id }?.let { amount = it.amount.toString(); duration = it.durationDays } },
                )
            }
            AppField(
                amount, { amount = it }, "مبلغ (تومان)", keyboardType = KeyboardType.Number, ltr = true,
                supporting = amountNum?.let { formatToman(it) },
            )
            DateField("تاریخ پرداخت", date, { date = it })
            Text("مدت شهریه", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            PillTabs(listOf("30" to "۳۰ روز", "60" to "۶۰ روز", "90" to "۹۰ روز"), duration.toString(), { duration = it.toInt() })
            Text(
                "اعتبار تا: ${formatJalaliLong(date.plusDays(duration.toLong()))}",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            ErrorText(error)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                PrimaryButton(
                    if (editingId != null) "ذخیره تغییرات" else "ثبت شهریه", modifier = Modifier.weight(1f), loading = saving,
                    container = g.tuition, content = androidx.compose.ui.graphics.Color.White,
                    onClick = {
                        if (amountNum == null || amountNum <= 0) { error = "مبلغ شهریه را وارد کنید."; return@PrimaryButton }
                        saving = true; error = null
                        scope.launch {
                            val err = vm.act("save_payment", Api.params {
                                put("p_membership", member.id)
                                put("p_amount", amountNum)
                                put("p_paid_on", date.toString())
                                put("p_duration", duration)
                                put("p_id", editingId)
                            })
                            if (err != null) error = err else {
                                UiEvents.toast(if (editingId != null) "تغییرات شهریه ذخیره شد" else "شهریه ثبت شد")
                                resetForm()
                            }
                            saving = false
                        }
                    },
                )
                if (editingId != null) SecondaryButton("انصراف", onClick = { resetForm() }, modifier = Modifier.weight(0.6f))
            }

            SectionTitle("تاریخچه پرداخت‌ها")
            AppCard {
                if (member.payments.isEmpty()) {
                    EmptyState(Icons.Rounded.Payments, g.tuition, "هنوز پرداختی ثبت نشده")
                } else {
                    member.payments.forEachIndexed { i, p ->
                        if (i > 0) HorizontalDivider(color = g.border)
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(formatToman(p.amount), style = MaterialTheme.typography.titleSmall)
                                Text(
                                    "${formatJalali(p.paidDate)} · ${formatDays(p.durationDays)} · تا ${formatJalali(p.endDate)}",
                                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            IconButton(onClick = {
                                editingId = p.id; amount = p.amount.toString(); date = p.paidDate; duration = p.durationDays; error = null
                            }) { Icon(Icons.Rounded.Edit, contentDescription = "ویرایش") }
                            IconButton(onClick = { deleting = p }) { Icon(Icons.Rounded.Delete, contentDescription = "حذف", tint = g.danger) }
                        }
                    }
                }
            }
        }
    }

    deleting?.let { p ->
        ConfirmDialog(
            title = "حذف پرداخت", text = "پرداخت ${formatToman(p.amount)} حذف شود؟", confirmLabel = "حذف",
            onConfirm = {
                deleting = null
                scope.launch {
                    val err = vm.act("delete_payment", Api.params { put("p_id", p.id) })
                    if (err != null) error = err else { UiEvents.toast("شهریه حذف شد"); if (editingId == p.id) resetForm() }
                }
            },
            onDismiss = { deleting = null },
        )
    }
}
