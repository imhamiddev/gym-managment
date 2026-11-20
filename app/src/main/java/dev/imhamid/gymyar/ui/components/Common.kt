package dev.imhamid.gymyar.ui.components

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
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.gym.border),
    ) {
        Column(
            modifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier,
            content = content,
        )
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier, trailing: @Composable (RowScope.() -> Unit)? = null) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text, style = MaterialTheme.typography.titleMedium)
        if (trailing != null) trailing()
    }
}

@Composable
fun IconBubble(icon: ImageVector, tone: Color, size: Dp = 44.dp, iconSize: Dp = 22.dp) {
    Box(
        modifier = Modifier.size(size).clip(RoundedCornerShape(size * 0.32f)).background(tone.soft()),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = tone, modifier = Modifier.size(iconSize))
    }
}

@Composable
fun PageHeader(title: String, description: String? = null, icon: ImageVector? = null, tone: Color = MaterialTheme.colorScheme.primary) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        if (icon != null) IconBubble(icon, tone, size = 48.dp)
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (description != null) {
                Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun StatCard(icon: ImageVector, tone: Color, label: String, value: Int, modifier: Modifier = Modifier) {
    AppCard(modifier = modifier) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            IconBubble(icon, tone, size = 40.dp, iconSize = 20.dp)
            Column {
                Text(value.fa(), style = MaterialTheme.typography.titleLarge)
                Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun EmptyState(icon: ImageVector, tone: Color, title: String, description: String? = null, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 32.dp),
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
    val palette = listOf(MaterialTheme.colorScheme.primary, g.members, g.tuition, g.attendance, g.warning, g.success)
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
        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
        singleLine = true,
        shape = MaterialTheme.shapes.large,
        modifier = modifier.fillMaxWidth(),
    )
}

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
    Button(
        onClick = onClick,
        enabled = enabled && !loading,
        shape = MaterialTheme.shapes.medium,
        colors = ButtonDefaults.buttonColors(containerColor = container, contentColor = content),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        modifier = modifier.fillMaxWidth().height(52.dp),
    ) {
        if (loading) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = content)
        } else {
            if (icon != null) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
            }
            Text(text, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    tone: Color = MaterialTheme.colorScheme.onSurface,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, MaterialTheme.gym.borderStrong),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = tone),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
        modifier = modifier.height(48.dp),
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun ErrorText(message: String?, modifier: Modifier = Modifier) {
    if (message.isNullOrBlank()) return
    Row(
        modifier = modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium).background(MaterialTheme.gym.danger.soft(0.1f)).padding(12.dp),
    ) {
        Text(message, color = MaterialTheme.gym.danger, style = MaterialTheme.typography.bodySmall)
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
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirmLabel, color = if (danger) MaterialTheme.gym.danger else MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
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
