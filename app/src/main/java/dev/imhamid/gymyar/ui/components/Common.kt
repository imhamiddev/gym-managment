package dev.imhamid.gymyar.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text as M3Text
import dev.imhamid.gymyar.ui.theme.gym
import dev.imhamid.gymyar.util.fa
import dev.imhamid.gymyar.util.formatDays

/** Soft tint of a tone color (used behind icons and badges). */
fun Color.soft(alpha: Float = 0.13f): Color = copy(alpha = alpha)

// ------------------------------------------------------------------ cards & layout

@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val source = remember { MutableInteractionSource() }
    Surface(
        modifier = (if (onClick != null) modifier.pressScale(source, 0.98f) else modifier).fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.gym.border),
    ) {
        Column(
            modifier = (if (onClick != null) Modifier.clickable(interactionSource = source, indication = androidx.compose.material3.ripple(), onClick = onClick) else Modifier)
                .animateContentSize(),
            content = content,
        )
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier, trailing: @Composable (RowScope.() -> Unit)? = null) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f, fill = trailing == null))
        if (trailing != null) trailing()
    }
}

@Composable
fun IconBubble(icon: ImageVector, tone: Color, size: Dp = 44.dp, iconSize: Dp = 22.dp) {
    Box(
        modifier = Modifier.size(size).clip(CircleShape).background(tone.soft(0.16f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = tone, modifier = Modifier.size(iconSize))
    }
}

/** White round button holding an icon (header actions, like the reference design). */
@Composable
fun RoundIcon(icon: ImageVector, description: String?, modifier: Modifier = Modifier, size: Dp = 46.dp, onClick: (() -> Unit)? = null) {
    val base = modifier.size(size).clip(CircleShape).background(MaterialTheme.colorScheme.surface)
    Box(
        modifier = if (onClick != null) base.clickable(onClick = onClick) else base,
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = description, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(size * 0.48f))
    }
}

/** Screen title block: big title, one quiet line under it, optional action on the leading side. */
@Composable
fun PageHeader(
    title: String,
    description: String? = null,
    icon: ImageVector? = null,
    tone: Color = MaterialTheme.colorScheme.primary,
    action: (@Composable () -> Unit)? = null,
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        if (icon != null) RoundIcon(icon, null, size = 48.dp)
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (description != null) {
                Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
        if (action != null) action()
    }
}

/** Soft pastel tile with a big number (the "1200 Kcal" tiles of the reference). */
@Composable
fun StatCard(icon: ImageVector, tone: Color, label: String, value: Int, modifier: Modifier = Modifier) {
    // The number counts up when the card appears and whenever the value changes.
    var target by remember { mutableIntStateOf(0) }
    LaunchedEffect(value) { target = value }
    val shown by animateIntAsState(target, tween(700, easing = FastOutSlowInEasing), label = "count")
    Column(
        modifier = modifier
            .heightIn(min = STAT_CARD_MIN_HEIGHT)
            .clip(MaterialTheme.shapes.large)
            .background(tone.soft(0.14f))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(Modifier.size(36.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surface), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = tone, modifier = Modifier.size(19.dp))
        }
        Column {
            Text(shown.fa(), style = MaterialTheme.typography.titleLarge)
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
        }
    }
}

/** All stat cards share one minimum height so a two-line label never makes a card taller than its neighbours. */
val STAT_CARD_MIN_HEIGHT = 112.dp

/** Pastel tile with free text value (for athlete home: days left, last payment...). */
@Composable
fun InfoTile(icon: ImageVector, tone: Color, label: String, value: String, modifier: Modifier = Modifier, unit: String? = null) {
    Column(
        modifier = modifier
            .heightIn(min = STAT_CARD_MIN_HEIGHT)
            .clip(MaterialTheme.shapes.large)
            .background(tone.soft(0.14f))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(Modifier.size(36.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surface), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = tone, modifier = Modifier.size(19.dp))
        }
        Column {
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(value, style = MaterialTheme.typography.titleLarge, maxLines = 1)
                if (unit != null) Text(unit, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 3.dp))
            }
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
        }
    }
}

@Composable
fun EmptyState(icon: ImageVector, tone: Color, title: String, description: String? = null, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        IconBubble(icon, tone, size = 56.dp, iconSize = 28.dp)
        Text(title, style = MaterialTheme.typography.titleSmall, textAlign = TextAlign.Center)
        if (description != null) {
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        }
    }
}

/** A plain label/value line used inside cards. */
@Composable
fun KeyValueRow(label: String, value: String, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}

// ------------------------------------------------------------------ badges & avatar

@Composable
fun ToneBadge(text: String, tone: Color, modifier: Modifier = Modifier, dot: Boolean = false) {
    Row(
        modifier = modifier.clip(CircleShape).background(tone.soft()).padding(horizontal = 10.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (dot) Box(Modifier.size(6.dp).clip(CircleShape).background(tone))
        Text(text, style = MaterialTheme.typography.labelMedium, color = tone, maxLines = 1)
    }
}

@Composable
fun RemainingBadge(days: Int?, modifier: Modifier = Modifier) {
    val g = MaterialTheme.gym
    when {
        days == null -> ToneBadge("بدون شهریه", MaterialTheme.colorScheme.onSurfaceVariant, modifier)
        days == 0 -> ToneBadge("منقضی شده", g.danger, modifier, dot = true)
        else -> ToneBadge(formatDays(days), if (days <= 7) g.warning else g.success, modifier, dot = true)
    }
}

@Composable
fun Avatar(first: String, last: String, size: Dp = 44.dp) {
    val g = MaterialTheme.gym
    val palette = listOf(g.accent, g.members, g.tuition, g.attendance, g.warning, g.success)
    var h = 0
    for (ch in "$first $last") h = (h * 31 + ch.code) and 0x7fffffff
    val tone = palette[h % palette.size]
    val initials = (first.trim().firstOrNull()?.toString() ?: "") + (last.trim().firstOrNull()?.toString() ?: "")
    Box(
        modifier = Modifier.size(size).clip(CircleShape).background(tone.soft(0.16f)),
        contentAlignment = Alignment.Center,
    ) {
        Text(initials.ifBlank { "؟" }, color = tone, fontWeight = FontWeight.Bold, fontSize = (size.value * 0.34f).sp)
    }
}

// ------------------------------------------------------------------ inputs & buttons

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = MaterialTheme.gym.accent,
    focusedLabelColor = MaterialTheme.gym.accent,
    cursorColor = MaterialTheme.gym.accent,
    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
    focusedContainerColor = MaterialTheme.colorScheme.surface,
    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
)

@Composable
fun AppField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    ltr: Boolean = false,
    password: Boolean = false,
    singleLine: Boolean = true,
    minLines: Int = 1,
    enabled: Boolean = true,
    placeholder: String? = null,
    trailing: (@Composable () -> Unit)? = null,
    leading: (@Composable () -> Unit)? = null,
    imeAction: ImeAction = ImeAction.Next,
    onDone: (() -> Unit)? = null,
    supporting: String? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = if (placeholder != null) ({ Text(placeholder, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)) }) else null,
        modifier = modifier.fillMaxWidth(),
        enabled = enabled,
        singleLine = singleLine,
        minLines = minLines,
        shape = MaterialTheme.shapes.medium,
        colors = fieldColors(),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
        keyboardActions = KeyboardActions(onDone = { onDone?.invoke() }),
        visualTransformation = if (password) PasswordVisualTransformation() else VisualTransformation.None,
        textStyle = LocalTextStyle.current.copy(textDirection = if (ltr) TextDirection.Ltr else TextDirection.Content),
        trailingIcon = trailing,
        leadingIcon = leading,
        supportingText = if (supporting != null) ({ Text(supporting) }) else null,
    )
}

@Composable
fun SearchField(value: String, onValueChange: (String) -> Unit, placeholder: String, modifier: Modifier = Modifier) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder, color = MaterialTheme.colorScheme.onSurfaceVariant) },
        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
        singleLine = true,
        shape = CircleShape,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.gym.accent,
            unfocusedBorderColor = Color.Transparent,
            cursorColor = MaterialTheme.gym.accent,
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
        ),
        modifier = modifier.fillMaxWidth(),
    )
}

/** Main action: full-width lime pill with dark text. */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    loading: Boolean = false,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    container: Color = MaterialTheme.colorScheme.primary,
    content: Color = MaterialTheme.colorScheme.onPrimary,
) {
    val source = remember { MutableInteractionSource() }
    val haptic = LocalHapticFeedback.current
    Button(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onClick()
        },
        enabled = enabled && !loading,
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = container, contentColor = content,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        interactionSource = source,
        modifier = modifier.pressScale(source).fillMaxWidth().height(54.dp),
    ) {
        AnimatedContent(
            targetState = loading,
            transitionSpec = { fadeIn(tween(160)) togetherWith fadeOut(tween(120)) },
            label = "button-content",
        ) { isLoading ->
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = content)
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (icon != null) {
                        Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(text, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/** Quiet outlined pill. */
@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    tone: Color = MaterialTheme.colorScheme.onSurface,
) {
    val source = remember { MutableInteractionSource() }
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = CircleShape,
        border = BorderStroke(1.dp, MaterialTheme.gym.borderStrong),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = tone),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp),
        interactionSource = source,
        modifier = modifier.pressScale(source).height(50.dp),
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

/** Small black pill (the "View" button of the reference). */
@Composable
fun InkButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: ImageVector? = null) {
    Row(
        modifier = modifier.clip(CircleShape).background(MaterialTheme.gym.ink).clickable(onClick = onClick).padding(horizontal = 18.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (icon != null) Icon(icon, contentDescription = null, tint = MaterialTheme.gym.onInk, modifier = Modifier.size(16.dp))
        Text(text, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.gym.onInk)
    }
}

@Composable
fun ErrorText(message: String?, modifier: Modifier = Modifier) {
    if (message.isNullOrBlank()) return
    AnimatedVisibility(
        visibleState = remember(message) { MutableTransitionState(false).apply { targetState = true } },
        enter = expandVertically(tween(220)) + fadeIn(tween(220)),
    ) {
        Row(
            modifier = modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium).background(MaterialTheme.gym.danger.soft(0.1f)).padding(12.dp),
        ) {
            Text(message, color = MaterialTheme.gym.danger, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun ConfirmDialog(
    title: String,
    text: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    danger: Boolean = true,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.extraLarge,
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirmLabel, color = if (danger) MaterialTheme.gym.danger else MaterialTheme.gym.accent, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف") } },
    )
}

// ------------------------------------------------------------------ loading skeletons

@Composable
fun SkeletonBox(modifier: Modifier = Modifier, height: Dp = 16.dp) {
    val t = rememberInfiniteTransition(label = "skeleton")
    val a by t.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "alpha",
    )
    Box(
        modifier = modifier.height(height).alpha(a).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceContainerHigh),
    )
}

/** Placeholder shown while a screen loads for the first time. */
@Composable
fun ScreenSkeleton(modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            SkeletonBox(Modifier.size(48.dp), 48.dp)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SkeletonBox(Modifier.width(150.dp), 18.dp)
                SkeletonBox(Modifier.width(100.dp), 12.dp)
            }
        }
        SkeletonBox(Modifier.fillMaxWidth(), 96.dp)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SkeletonBox(Modifier.weight(1f), 70.dp)
            SkeletonBox(Modifier.weight(1f), 70.dp)
        }
        repeat(4) { SkeletonBox(Modifier.fillMaxWidth(), 64.dp) }
    }
}

@Composable
fun Banner(text: String, tone: Color, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium).background(tone.soft(0.12f)).padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        M3Text(text, color = tone, style = MaterialTheme.typography.bodySmall)
    }
}
