package dev.imhamid.gymyar.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.imhamid.gymyar.ui.theme.gym

/**
 * Page background: calm off-white with a soft lime glow at the top
 * (the gradient header of the reference design). Every main screen sits on it.
 */
@Composable
fun AppBackground(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    val g = MaterialTheme.gym
    val bg = MaterialTheme.colorScheme.background
    Box(modifier.fillMaxSize().background(bg)) {
        Box(
            Modifier.fillMaxWidth().height(300.dp)
                .background(Brush.verticalGradient(listOf(g.headerTop, bg))),
        )
        content()
    }
}

/** One slot of the floating bottom bar. A [center] slot is the big lime action button. */
data class NavSpec(val route: String, val label: String, val icon: ImageVector, val center: Boolean = false)

@Composable
fun FloatingNavBar(items: List<NavSpec>, selected: String?, onSelect: (String) -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
        shape = RoundedCornerShape(32.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 10.dp,
        border = BorderStroke(1.dp, MaterialTheme.gym.border),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(72.dp).padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            items.forEach { item ->
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    if (item.center) CenterNavButton(item, item.route == selected) { onSelect(item.route) }
                    else NavTabItem(item, item.route == selected) { onSelect(item.route) }
                }
            }
        }
    }
}

@Composable
private fun NavTabItem(item: NavSpec, selected: Boolean, onClick: () -> Unit) {
    val g = MaterialTheme.gym
    val source = remember { MutableInteractionSource() }
    Column(
        modifier = Modifier.clip(RoundedCornerShape(20.dp))
            .clickable(interactionSource = source, indication = null, onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Box(
            modifier = Modifier.height(30.dp).width(52.dp).clip(CircleShape)
                .background(if (selected) g.lime else Color.Transparent),
            contentAlignment = Alignment.Center,
        ) {
            CompositionLocalProvider(LocalContentColor provides (if (selected) g.onLime else MaterialTheme.colorScheme.onSurfaceVariant)) {
                TabIcon(item.icon, selected)
            }
        }
        Text(
            item.label,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun CenterNavButton(item: NavSpec, selected: Boolean, onClick: () -> Unit) {
    val g = MaterialTheme.gym
    val source = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier.size(58.dp)
            .pressScale(source)
            .clip(CircleShape)
            .background(g.lime)
            .then(if (selected) Modifier.border(2.5.dp, g.ink, CircleShape) else Modifier)
            .clickable(interactionSource = source, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(item.icon, contentDescription = item.label, tint = g.onLime, modifier = Modifier.size(28.dp))
    }
}

/** Standard scrolling page: one column, 16dp gutters and even spacing so nothing feels crowded. */
@Composable
fun ScreenColumn(modifier: Modifier = Modifier, spacing: Dp = 16.dp, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(spacing),
        content = content,
    )
}

/** Small lime round button with an icon (copy, add...). */
@Composable
fun LimeIconButton(icon: ImageVector, description: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val g = MaterialTheme.gym
    Box(
        modifier = modifier.size(44.dp).clip(CircleShape).background(g.lime).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = description, tint = g.onLime, modifier = Modifier.size(20.dp))
    }
}
