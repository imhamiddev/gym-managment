package dev.imhamid.gymyar.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.imhamid.gymyar.ui.theme.gym
import dev.imhamid.gymyar.util.fa
import dev.imhamid.gymyar.util.isGymDay
import dev.imhamid.gymyar.util.jalaliOf
import dev.imhamid.gymyar.util.jsWeekday
import java.time.LocalDate

private val WEEK_LETTERS = listOf("ش", "ی", "د", "س", "چ", "پ", "ج")

/** Saturday of the week that contains [day] (the Persian week starts on Saturday). */
fun weekStartOf(day: LocalDate): LocalDate = day.minusDays(((day.jsWeekday() + 1) % 7).toLong())

/**
 * One Persian week as seven tappable days (the date strip of the reference design).
 * Selected day = lime capsule, closed gym days are faded, [marked] days get a small dot.
 */
@Composable
fun WeekStrip(
    selected: LocalDate,
    weekOf: LocalDate,
    workDays: List<Int>,
    modifier: Modifier = Modifier,
    marked: Set<LocalDate> = emptySet(),
    today: LocalDate? = null,
    onSelect: ((LocalDate) -> Unit)? = null,
) {
    val g = MaterialTheme.gym
    val start = weekStartOf(weekOf)
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        for (i in 0 until 7) {
            val d = start.plusDays(i.toLong())
            val on = d == selected
            val open = isGymDay(workDays, d)
            val base = Modifier.weight(1f)
                .clip(RoundedCornerShape(26.dp))
                .background(if (on) g.lime else Color.Transparent)
                .let { if (onSelect != null) it.clickable { onSelect(d) } else it }
                .padding(vertical = 8.dp)
            Column(
                modifier = base,
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    WEEK_LETTERS[i],
                    style = MaterialTheme.typography.labelMedium,
                    color = if (on) g.onLime else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Box(
                    modifier = Modifier.size(30.dp).clip(CircleShape)
                        .background(if (on) MaterialTheme.colorScheme.surface else Color.Transparent)
                        .alpha(if (open) 1f else 0.38f),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        jalaliOf(d).jd.fa(),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (d == today) FontWeight.ExtraBold else FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                if (d in marked) {
                    Box(Modifier.size(6.dp).clip(CircleShape).background(g.accent))
                } else {
                    Spacer(Modifier.height(6.dp))
                }
            }
        }
    }
}
