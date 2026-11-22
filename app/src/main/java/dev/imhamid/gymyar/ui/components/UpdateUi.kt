package dev.imhamid.gymyar.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.SystemUpdateAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.imhamid.gymyar.AppViewModel.UpdatePhase
import dev.imhamid.gymyar.AppViewModel.UpdateState
import dev.imhamid.gymyar.data.Updater
import dev.imhamid.gymyar.ui.theme.gym
import dev.imhamid.gymyar.util.fa
import java.util.Locale

private fun megabytes(size: Long): String? =
    if (size > 0) String.format(Locale.US, "%.1f", size / 1_048_576.0).fa() else null

/** Thin strip at the very top of the screen while a newer version is available. Tap = open the dialog. */
@Composable
fun UpdateBanner(state: UpdateState, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val g = MaterialTheme.gym
    val text = when (val p = state.phase) {
        is UpdatePhase.Downloading ->
            "در حال دانلود نسخه‌ی ${state.release.version.fa()}" + (p.progress?.let { " — ${(it * 100).toInt().fa()}٪" } ?: "")
        is UpdatePhase.Ready -> "نسخه‌ی ${state.release.version.fa()} دانلود شد — برای نصب بزنید"
        else -> "نسخه‌ی جدید ${state.release.version.fa()} منتشر شده — بزنید"
    }
    Row(
        modifier = modifier.fillMaxWidth().background(g.lime).clickable(onClick = onClick)
            .height(34.dp).padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
    ) {
        Icon(Icons.Rounded.SystemUpdateAlt, contentDescription = null, tint = g.onLime, modifier = Modifier.size(16.dp))
        Text(text, color = g.onLime, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

/** Dialog shown when the app opens with a newer release: download inside the app, then install. */
@Composable
fun UpdateDialog(state: UpdateState, onDismiss: () -> Unit, onDownload: () -> Unit, onCancel: () -> Unit, onInstall: (android.content.Context) -> Unit) {
    val context = LocalContext.current
    val rel = state.release
    val phase = state.phase
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.extraLarge,
        containerColor = MaterialTheme.colorScheme.surface,
        icon = { Icon(Icons.Rounded.SystemUpdateAlt, contentDescription = null, tint = MaterialTheme.gym.accent) },
        title = { Text("نسخه‌ی جدید باشگاه‌یار") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                val size = megabytes(rel.size)?.let { " (${it} مگابایت)" } ?: ""
                Text(
                    "نسخه‌ی ${rel.version.fa()}$size منتشر شده است. نسخه‌ی فعلی شما ${Updater.currentVersion.fa()} است.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                when (phase) {
                    is UpdatePhase.Downloading -> {
                        if (phase.progress != null) LinearProgressIndicator(progress = { phase.progress }, modifier = Modifier.fillMaxWidth())
                        else LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        Text(
                            "در حال دانلود…" + (phase.progress?.let { " ${(it * 100).toInt().fa()}٪" } ?: ""),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    is UpdatePhase.Ready -> Text("دانلود کامل شد. برای نصب، دکمه‌ی «نصب» را بزنید.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    is UpdatePhase.Failed -> Text(phase.message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.gym.danger)
                    UpdatePhase.Available -> Unit
                }
            }
        },
        confirmButton = {
            when (phase) {
                UpdatePhase.Available -> TextButton(onClick = onDownload) { Text("دانلود و نصب", fontWeight = FontWeight.Bold) }
                is UpdatePhase.Downloading -> TextButton(onClick = onDismiss) { Text("ادامه در پس‌زمینه", fontWeight = FontWeight.Bold) }
                is UpdatePhase.Ready -> TextButton(onClick = { onInstall(context) }) { Text("نصب", fontWeight = FontWeight.Bold) }
                is UpdatePhase.Failed -> TextButton(onClick = onDownload) { Text("تلاش دوباره", fontWeight = FontWeight.Bold) }
            }
        },
        dismissButton = {
            when (phase) {
                is UpdatePhase.Downloading -> TextButton(onClick = onCancel) { Text("لغو") }
                else -> TextButton(onClick = onDismiss) { Text("بعداً") }
            }
        },
    )
}
