package dev.imhamid.gymyar.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import dev.imhamid.gymyar.ui.components.PrimaryButton
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
    val primary = MaterialTheme.colorScheme.primary
    Box2(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) primary else MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
    ) {
        Text(
            label,
            color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

@Composable
private fun Box2(modifier: Modifier, content: @Composable () -> Unit) {
    androidx.compose.foundation.layout.Box(modifier = modifier, contentAlignment = Alignment.Center) { content() }
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
    val scope = rememberCoroutineScope()
    var first by remember(me.firstName) { mutableStateOf(me.firstName) }
    var last by remember(me.lastName) { mutableStateOf(me.lastName) }
    var error by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }
    var confirmLogout by remember { mutableStateOf(false) }
    val theme by app.themeMode.collectAsStateWithLifecycleCompat()

    Column(
        modifier = modifier.verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Avatar(me.firstName, me.lastName, size = 64.dp)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(me.fullName, style = MaterialTheme.typography.titleLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    ToneBadge(me.roleLabel, MaterialTheme.colorScheme.primary)
                    Text(me.phone.fa(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (gymName != null) {
                    Text("باشگاه: $gymName", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        AppCard {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SectionTitle("ویرایش نام")
                AppField(first, { first = it }, "نام")
                AppField(last, { last = it }, "نام خانوادگی")
                ErrorText(error)
                PrimaryButton(
                    "ذخیره", loading = saving,
                    onClick = {
                        saving = true; error = null
                        scope.launch {
                            when (val r = Repo.updateMyProfile(first, last)) {
                                is Res.Ok -> { UiEvents.toast("پروفایل ذخیره شد"); app.refreshMe() }
                                is Res.Fail -> error = r.message
                            }
                            saving = false
                        }
                    },
                )
            }
        }

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

        extra()

        SecondaryButton(
            "خروج از حساب", onClick = { confirmLogout = true },
            icon = Icons.AutoMirrored.Rounded.Logout, tone = MaterialTheme.gym.danger,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
    }

    if (confirmLogout) {
        ConfirmDialog(
            title = "خروج از حساب", text = "از حساب خود خارج می‌شوید. ادامه می‌دهید؟", confirmLabel = "خروج",
            onConfirm = { confirmLogout = false; app.logout() },
            onDismiss = { confirmLogout = false },
        )
    }
}
