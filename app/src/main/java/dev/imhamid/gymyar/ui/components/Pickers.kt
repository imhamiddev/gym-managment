package dev.imhamid.gymyar.ui.components

import android.app.TimePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import dev.imhamid.gymyar.ui.theme.gym
import dev.imhamid.gymyar.util.*
import java.time.LocalDate

/** Horizontally scrollable pill selector (filters, durations, plans...). */
@Composable
fun PillTabs(
    options: List<Pair<String, String>>,
    selected: String?,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        for ((key, label) in options) {
            val on = key == selected
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(if (on) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { onSelect(key) }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (on) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

/** Read-only field that opens [content] when tapped. */
@Composable
private fun ClickField(label: String, value: String, icon: @Composable () -> Unit, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        modifier = modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium).clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            icon()
            Column(modifier = Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(value, style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

@Composable
fun DateField(
    label: String,
    date: LocalDate?,
    onPick: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "انتخاب تاریخ",
    minDate: LocalDate = LocalDate.of(1900, 1, 1),
    maxDate: LocalDate? = null,
) {
    var open by remember { mutableStateOf(false) }
    ClickField(
        label = label,
        value = date?.let { formatJalaliLong(it) } ?: placeholder,
        icon = { Icon(Icons.Rounded.CalendarMonth, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
        modifier = modifier,
    ) { open = true }
    if (open) {
        JalaliDatePickerDialog(
            initial = date ?: todayTehran().let { if (maxDate != null && it > maxDate) maxDate else it },
            minDate = minDate, maxDate = maxDate,
            onDismiss = { open = false },
            onPick = { open = false; onPick(it) },
        )
    }
}

@Composable
fun TimeField(label: String, value: String, onChange: (String) -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    ClickField(
        label = label,
        value = value.fa(),
        icon = { Icon(Icons.Rounded.Schedule, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
        modifier = modifier,
    ) {
        val h = value.substringBefore(':').toIntOrNull() ?: 8
        val m = value.substringAfter(':', "0").toIntOrNull() ?: 0
        TimePickerDialog(context, { _, hh, mm -> onChange("%02d:%02d".format(hh, mm)) }, h, m, true).show()
    }
}

private val WEEK_HEAD = listOf("ش", "ی", "د", "س", "چ", "پ", "ج")

@Composable
fun JalaliDatePickerDialog(
    initial: LocalDate,
    onDismiss: () -> Unit,
    onPick: (LocalDate) -> Unit,
    minDate: LocalDate = LocalDate.of(1900, 1, 1),
    maxDate: LocalDate? = null,
) {
    val today = remember { todayTehran() }
    val start = jalaliOf(initial)
    var year by remember { mutableIntStateOf(start.jy) }
    var month by remember { mutableIntStateOf(start.jm) }
    var selected by remember { mutableStateOf(initial) }
    var showYears by remember { mutableStateOf(false) }

    fun allowed(d: LocalDate) = d >= minDate && (maxDate == null || d <= maxDate)
    val minYear = jalaliOf(minDate).jy
    val maxYear = jalaliOf(maxDate ?: today.plusYears(30)).jy

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.surface) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = {
                        if (month == 1) { month = 12; year -= 1 } else month -= 1
                    }, enabled = !showYears) { Icon(Icons.Rounded.ChevronRight, contentDescription = "ماه قبل") }
                    TextButton(onClick = { showYears = !showYears }, modifier = Modifier.weight(1f)) {
                        Text("${Jalali.MONTHS[month - 1]} ${year.fa()}", style = MaterialTheme.typography.titleMedium)
                    }
                    IconButton(onClick = {
                        if (month == 12) { month = 1; year += 1 } else month += 1
                    }, enabled = !showYears) { Icon(Icons.Rounded.ChevronLeft, contentDescription = "ماه بعد") }
                }

                if (showYears) {
                    val years = (minYear..maxYear).toList()
                    val listState = rememberLazyListState(initialFirstVisibleItemIndex = maxOf(0, years.indexOf(year) - 2))
                    LazyColumn(state = listState, modifier = Modifier.height(280.dp)) {
                        items(years) { y ->
                            val on = y == year
                            Box(
                                modifier = Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium)
                                    .background(if (on) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else androidx.compose.ui.graphics.Color.Transparent)
                                    .clickable { year = y; showYears = false }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(y.fa(), style = MaterialTheme.typography.titleSmall, color = if (on) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }
                } else {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        for (h in WEEK_HEAD) {
                            Text(h, modifier = Modifier.weight(1f), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    val first = jalaliToDate(year, month, 1) ?: initial
                    val offset = (first.jsWeekday() + 1) % 7
                    val len = Jalali.monthLength(year, month)
                    val cells: List<Int?> = List(offset) { null } + (1..len).toList()
                    val padded = cells + List((7 - cells.size % 7) % 7) { null }
                    for (week in padded.chunked(7)) {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            for (day in week) {
                                Box(modifier = Modifier.weight(1f).height(42.dp), contentAlignment = Alignment.Center) {
                                    if (day != null) {
                                        val date = jalaliToDate(year, month, day)
                                        val ok = date != null && allowed(date)
                                        val on = date != null && date == selected
                                        val isToday = date == today
                                        Box(
                                            modifier = Modifier.size(38.dp).clip(CircleShape)
                                                .background(if (on) MaterialTheme.colorScheme.primary else androidx.compose.ui.graphics.Color.Transparent)
                                                .then(if (isToday && !on) Modifier.background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)) else Modifier)
                                                .clickable(enabled = ok) { if (date != null) selected = date },
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Text(
                                                day.fa(),
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = when {
                                                    on -> MaterialTheme.colorScheme.onPrimary
                                                    !ok -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                                    else -> MaterialTheme.colorScheme.onSurface
                                                },
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Text(
                    "انتخاب‌شده: ${formatJalaliLong(selected)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(onClick = {
                        if (allowed(today)) { selected = today; val j = jalaliOf(today); year = j.jy; month = j.jm; showYears = false }
                    }) { Text("امروز") }
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = onDismiss) { Text("انصراف") }
                    TextButton(onClick = { onPick(selected) }, enabled = allowed(selected)) {
                        Text("تأیید", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RefreshBox(refreshing: Boolean, onRefresh: () -> Unit, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    androidx.compose.material3.pulltorefresh.PullToRefreshBox(
        isRefreshing = refreshing,
        onRefresh = onRefresh,
        modifier = modifier,
        content = { content() },
    )
}
