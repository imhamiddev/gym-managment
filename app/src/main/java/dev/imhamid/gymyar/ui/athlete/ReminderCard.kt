package dev.imhamid.gymyar.ui.athlete

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import dev.imhamid.gymyar.data.Settings
import dev.imhamid.gymyar.ui.components.AppCard
import dev.imhamid.gymyar.ui.components.IconBubble
import dev.imhamid.gymyar.ui.components.UiEvents
import dev.imhamid.gymyar.ui.theme.gym
import dev.imhamid.gymyar.work.Reminders

/** Switch for the tuition reminders (2 days before, 1 day before, on the last day). */
@Composable
fun ReminderCard() {
    val context = LocalContext.current
    var enabled by remember { mutableStateOf(Settings.remindersEnabled && Reminders.canNotify(context)) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        enabled = granted
        Settings.remindersEnabled = granted
        if (!granted) UiEvents.toast("برای یادآوری، اجازه‌ی اعلان لازم است.")
    }
    AppCard {
        Row(
            modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            IconBubble(Icons.Rounded.Notifications, MaterialTheme.gym.warning)
            Column(modifier = Modifier.weight(1f)) {
                Text("یادآوری پایان شهریه", style = MaterialTheme.typography.titleSmall)
                Text(
                    "۲ روز قبل، ۱ روز قبل و روز پایان، ساعت ۹ صبح",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(checked = enabled, onCheckedChange = { on ->
                if (!on) {
                    enabled = false; Settings.remindersEnabled = false
                } else if (Build.VERSION.SDK_INT >= 33 && !Reminders.canNotify(context)) {
                    launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    enabled = true; Settings.remindersEnabled = true
                }
            })
        }
    }
}
