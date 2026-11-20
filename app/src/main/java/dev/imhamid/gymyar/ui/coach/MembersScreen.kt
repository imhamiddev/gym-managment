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
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import dev.imhamid.gymyar.data.Api
import dev.imhamid.gymyar.data.CoachHome
import dev.imhamid.gymyar.data.MemberDto
import dev.imhamid.gymyar.ui.components.*
import dev.imhamid.gymyar.ui.theme.gym
import dev.imhamid.gymyar.util.*
import kotlinx.coroutines.launch
import kotlinx.serialization.json.put
import java.time.LocalDate

private fun matches(m: MemberDto, filter: String, today: LocalDate): Boolean {
    val r = m.remaining(today)
    return when (filter) {
        "active" -> (r ?: 0) > 7
        "expiring" -> r != null && r > 0 && r <= 7
        "expired" -> r == null || r == 0
        else -> true
    }
}

@Composable
fun MembersScreen(data: CoachHome, stale: Boolean, vm: CoachViewModel) {
    val today = remember { todayTehran() }
    var q by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf("all") }
    var selectedId by remember { mutableStateOf<String?>(null) }
    val g = MaterialTheme.gym

    val sorted = remember(data.members) { CoachViewModel.sortByName(data.members) { "${it.lastName} ${it.firstName}" } }
    val list = sorted.filter { matches(it, filter, today) && it.fullName.contains(q.trim()) }
    val selected = data.members.firstOrNull { it.id == selectedId }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        if (stale) Banner("حالت آفلاین: آخرین اطلاعات ذخیره‌شده نمایش داده می‌شود.", g.warning)
        PageHeader("مدیریت اعضا", "${data.members.size.fa()} عضو", Icons.Rounded.Groups, g.members)
        SearchField(q, { q = it }, "جستجوی نام یا نام خانوادگی")
        PillTabs(
            listOf("all" to "همه", "active" to "فعال", "expiring" to "رو به پایان", "expired" to "منقضی"),
            filter, { filter = it },
        )
        AppCard {
            if (list.isEmpty()) {
                EmptyState(
                    Icons.Rounded.Groups, g.members,
                    if (data.members.isEmpty()) "هنوز عضوی ندارید" else "عضوی پیدا نشد",
                    if (data.members.isEmpty()) "کد باشگاه را برای ورزشکاران بفرستید و درخواست‌هایشان را تایید کنید." else "نام دیگری را جستجو کنید یا فیلتر را تغییر دهید.",
                )
            } else {
                list.forEachIndexed { i, m ->
                    if (i > 0) HorizontalDivider(color = g.border)
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { selectedId = m.id }.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Avatar(m.firstName, m.lastName)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(m.fullName, style = MaterialTheme.typography.bodyLarge, maxLines = 1)
                            Text(m.phone.fa(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (isBirthday(m.birth, today)) ToneBadge("تولد امروز", g.attendance)
                        RemainingBadge(m.remaining(today))
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
    }

    if (selected != null) {
        MemberSheet(member = selected, vm = vm, onClose = { selectedId = null })
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MemberSheet(member: MemberDto, vm: CoachViewModel, onClose: () -> Unit) {
    val g = MaterialTheme.gym
    val today = remember { todayTehran() }
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var height by remember(member.id) { mutableStateOf(member.heightCm?.toString().orEmpty()) }
    var weight by remember(member.id) { mutableStateOf(member.weightKg?.let { if (it % 1.0 == 0.0) it.toInt().toString() else it.toString() }.orEmpty()) }
    var goal by remember(member.id) { mutableStateOf(member.goal.orEmpty()) }
    var program by remember(member.id) { mutableStateOf(member.program.orEmpty()) }
    var notes by remember(member.id) { mutableStateOf(member.notes.orEmpty()) }
    var birth by remember(member.id) { mutableStateOf(member.birth) }
    var error by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }
    var confirmRemove by remember { mutableStateOf(false) }

    fun num(s: String): Double? = s.en().filter { it.isDigit() || it == '.' }.toDoubleOrNull()

    ModalBottomSheet(onDismissRequest = onClose, sheetState = sheetState, containerColor = MaterialTheme.colorScheme.surface) {
        Column(
            modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).imePadding().padding(horizontal = 20.dp).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Avatar(member.firstName, member.lastName, size = 56.dp)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(member.fullName, style = MaterialTheme.typography.titleLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        RemainingBadge(member.remaining(today))
                        if (isBirthday(member.birth, today)) ToneBadge("تولد امروز", g.attendance)
                    }
                }
            }
            AppCard {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                    DetailRow("شماره تلفن", member.phone.fa())
                    HorizontalDivider(color = g.border)
                    DetailRow("عضویت از", member.joined?.let { formatJalali(it) } ?: "—")
                    HorizontalDivider(color = g.border)
                    DetailRow("آخرین شهریه", member.lastPayment?.let { "${formatToman(it.amount)} · ${formatJalali(it.paidOn)}" } ?: "ثبت نشده")
                    HorizontalDivider(color = g.border)
                    DetailRow("حضور در ۳۰ روز اخیر", member.presentLast30.fa())
                    member.birth?.let {
                        HorizontalDivider(color = g.border)
                        DetailRow("سن", "${ageOn(it, today).fa()} سال")
                    }
                }
            }

            SectionTitle("مشخصات و برنامه")
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                AppField(height, { height = it }, "قد (cm)", Modifier.weight(1f), keyboardType = KeyboardType.Number, ltr = true)
                AppField(weight, { weight = it }, "وزن (kg)", Modifier.weight(1f), keyboardType = KeyboardType.Decimal, ltr = true)
            }
            DateField("تاریخ تولد", birth, { birth = it }, maxDate = today, placeholder = "ثبت نشده")
            AppField(goal, { goal = it }, "هدف (مثلاً کاهش وزن)")
            AppField(program, { program = it }, "برنامه تمرینی (برای ورزشکار نمایش داده می‌شود)", singleLine = false, minLines = 3)
            AppField(notes, { notes = it }, "یادداشت خصوصی مربی (ورزشکار نمی‌بیند)", singleLine = false, minLines = 2)
            ErrorText(error)
            PrimaryButton("ذخیره مشخصات", loading = saving, onClick = {
                saving = true; error = null
                scope.launch {
                    val err = vm.act("update_member_profile", Api.params {
                        put("p_membership", member.id)
                        put("p_height", if (height.isBlank()) null else num(height))
                        put("p_weight", if (weight.isBlank()) null else num(weight))
                        put("p_goal", goal)
                        put("p_program", program)
                        put("p_notes", notes)
                        put("p_birth", birth?.toString())
                    })
                    if (err != null) error = err else UiEvents.toast("مشخصات ذخیره شد")
                    saving = false
                }
            })
            SecondaryButton("حذف از باشگاه", onClick = { confirmRemove = true }, icon = Icons.Rounded.Delete, tone = g.danger, modifier = Modifier.fillMaxWidth())
        }
    }

    if (confirmRemove) {
        ConfirmDialog(
            title = "حذف عضو",
            text = "«${member.fullName}» همراه با شهریه‌ها و حضور و غیابش از باشگاه حذف می‌شود. ادامه می‌دهید؟",
            confirmLabel = "حذف",
            onConfirm = {
                confirmRemove = false
                scope.launch {
                    val err = vm.act("remove_member", Api.params { put("p_id", member.id) })
                    if (err != null) error = err else { UiEvents.toast("عضو از باشگاه حذف شد"); onClose() }
                }
            },
            onDismiss = { confirmRemove = false },
        )
    }
}
