package dev.imhamid.gymyar.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.core.content.ContextCompat
import androidx.navigation.NavBackStackEntry
import dev.imhamid.gymyar.data.Settings
import dev.imhamid.gymyar.ui.theme.gym
import kotlinx.coroutines.delay

// ------------------------------------------------------------------ press feedback

/** Shrinks the element a little while it is pressed and springs back on release. */
@Composable
fun Modifier.pressScale(source: MutableInteractionSource, pressed: Float = 0.97f): Modifier {
    val isPressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) pressed else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "press",
    )
    return this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

/** Bottom-bar icon that grows slightly when its tab is selected. */
@Composable
fun TabIcon(icon: ImageVector, selected: Boolean) {
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.14f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "tab",
    )
    Icon(icon, contentDescription = null, modifier = Modifier.graphicsLayer { scaleX = scale; scaleY = scale })
}

// ------------------------------------------------------------------ navigation motion

private const val SLIDE_MS = 320

private fun AnimatedContentTransitionScope<NavBackStackEntry>.isTabSwitch(top: Set<String>): Boolean =
    initialState.destination.route in top && targetState.destination.route in top

/**
 * Switching between bottom-bar tabs is instant (no fade, no slide).
 * Opening or closing a detail screen slides it in from the leading edge (RTL aware).
 */
class NavMotion(private val top: Set<String>) {
    val enter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        if (isTabSwitch(top)) EnterTransition.None
        else slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(SLIDE_MS, easing = FastOutSlowInEasing))
    }
    val exit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        if (isTabSwitch(top)) ExitTransition.None
        else slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(SLIDE_MS, easing = FastOutSlowInEasing), targetOffset = { it / 4 })
    }
    val popEnter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        if (isTabSwitch(top)) EnterTransition.None
        else slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(SLIDE_MS, easing = FastOutSlowInEasing), initialOffset = { it / 4 })
    }
    val popExit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        if (isTabSwitch(top)) ExitTransition.None
        else slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(SLIDE_MS, easing = FastOutSlowInEasing))
    }
}

// ------------------------------------------------------------------ notification permission

/**
 * Asks once for the notification permission (Android 13+) with a short explanation first.
 * The reminders switch in the profile stays available for people who say "later".
 */
@Composable
fun NotificationPermissionPrompt() {
    if (Build.VERSION.SDK_INT < 33) return
    val context = LocalContext.current
    var show by remember { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

    LaunchedEffect(Unit) {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        if (!granted && !Settings.notificationsPrompted) {
            delay(700)
            show = true
        }
    }

    if (show) {
        AlertDialog(
            onDismissRequest = { show = false; Settings.notificationsPrompted = true },
            icon = { Icon(Icons.Rounded.Notifications, contentDescription = null, tint = MaterialTheme.gym.accent) },
            title = { Text("فعال‌سازی اعلان‌ها") },
            text = { Text("برای اینکه یادآوری‌ها (مثل نزدیک شدن پایان شهریه) را به‌موقع ببینید، اجازه‌ی ارسال اعلان بدهید.") },
            confirmButton = {
                TextButton(onClick = {
                    show = false
                    Settings.notificationsPrompted = true
                    launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }) { Text("اجازه می‌دهم", fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { show = false; Settings.notificationsPrompted = true }) { Text("بعداً") }
            },
        )
    }
}
