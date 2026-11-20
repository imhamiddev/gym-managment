package dev.imhamid.gymyar.ui.common

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.imhamid.gymyar.AppViewModel
import dev.imhamid.gymyar.data.Me
import dev.imhamid.gymyar.data.Repo
import dev.imhamid.gymyar.data.Res
import dev.imhamid.gymyar.ui.components.AppCard
import dev.imhamid.gymyar.ui.components.AppField
import dev.imhamid.gymyar.ui.components.Avatar
import dev.imhamid.gymyar.ui.components.ConfirmDialog
import dev.imhamid.gymyar.ui.components.ErrorText
import dev.imhamid.gymyar.ui.components.InkButton
import dev.imhamid.gymyar.ui.components.ScreenColumn
import dev.imhamid.gymyar.ui.components.SecondaryButton
import dev.imhamid.gymyar.ui.components.SectionTitle
import dev.imhamid.gymyar.ui.components.ToneBadge
import dev.imhamid.gymyar.ui.components.UiEvents
import dev.imhamid.gymyar.ui.theme.ThemeMode
import dev.imhamid.gymyar.ui.theme.gym
import dev.imhamid.gymyar.util.fa
import kotlinx.coroutines.launch

@Composable
private fun ThemeChoice(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val g = MaterialTheme.gym
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(if (selected) g.lime else MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            color = if (selected) g.onLime else MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
        )
    }
}

/** Name is only editable after tapping "ویرایش پروفایل" — the page itself just shows it. */
@Composable
private fun EditProfileDialog(me: Me, app: AppViewModel, onDismiss: () -> Unit) {
    val scope = rememberCoroutineScope()
    var first by remember { mutableStateOf(me.firstName) }
    var last by remember { mutableStateOf(me.lastName) }
    var error by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = { if (!saving) onDismiss() },
        shape = MaterialTheme.shapes.extraLarge,
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text("ویرایش پروفایل") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                AppField(first, { first = it }, "نام")
                AppField(last, { last = it }, "نام خانوادگی")
                ErrorText(error)
            }
        },
        confirmButton = {
            TextButton(enabled = !saving, onClick = {
                saving = true; error = null
                scope.launch {
                    when (val r = Repo.updateMyProfile(first, last)) {
                        is Res.Ok -> { UiEvents.toast("پروفایل ذخیره شد"); app.refreshMe(); onDismiss() }
                        is Res.Fail -> error = r.message
                    }
                    saving = false
                }
            }) { Text("ذخیره", color = MaterialTheme.gym.accent, fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(enabled = !saving, onClick = onDismiss) { Text("انصراف") } },
    )
}

/** Account info, theme and logout. Shared by coach and athlete; [extra] adds role-specific cards. */
@Composable
fun ProfileScreen(
    me: Me,
    app: AppViewModel,
    gymName: String?,
    modifier: Modifier = Modifier,
    extra: @Composable () -> Unit = {},
) {
    var editing by remember { mutableStateOf(false) }
    var confirmLogout by remember { mutableStateOf(false) }
    val theme by app.themeMode.collectAsStateWithLifecycleCompat()

    ScreenColumn(modifier = modifier) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Avatar(me.firstName, me.lastName, size = 88.dp)
            Text(me.fullName, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                ToneBadge(me.roleLabel, MaterialTheme.gym.accent)
                Text(me.phone.fa(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (gymName != null) {
                Text("باشگاه: $gymName", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(4.dp))
            InkButton("ویرایش پروفایل", onClick = { editing = true }, icon = Icons.Rounded.Edit)
        }

        extra()

        AppCard {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SectionTitle("نمایش")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    ThemeChoice("مطابق سیستم", theme == ThemeMode.SYSTEM, { app.setTheme(ThemeMode.SYSTEM) }, Modifier.weight(1f))
                    ThemeChoice("روشن", theme == ThemeMode.LIGHT, { app.setTheme(ThemeMode.LIGHT) }, Modifier.weight(1f))
                    ThemeChoice("تاریک", theme == ThemeMode.DARK, { app.setTheme(ThemeMode.DARK) }, Modifier.weight(1f))
                }
            }
        }

        SecondaryButton(
            "خروج از حساب", onClick = { confirmLogout = true },
            icon = Icons.AutoMirrored.Rounded.Logout, tone = MaterialTheme.gym.danger,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
    }

    if (editing) EditProfileDialog(me, app, onDismiss = { editing = false })

    if (confirmLogout) {
        ConfirmDialog(
            title = "خروج از حساب", text = "از حساب خود خارج می‌شوید. ادامه می‌دهید؟", confirmLabel = "خروج",
            onConfirm = { confirmLogout = false; app.logout() },
            onDismiss = { confirmLogout = false },
        )
    }
}
