package dev.imhamid.gymyar

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.imhamid.gymyar.ui.auth.AuthScreen
import dev.imhamid.gymyar.ui.auth.BrandMark
import dev.imhamid.gymyar.ui.athlete.AthleteApp
import dev.imhamid.gymyar.ui.coach.CoachApp
import dev.imhamid.gymyar.ui.components.AppBackground
import dev.imhamid.gymyar.ui.components.NotificationPermissionPrompt
import dev.imhamid.gymyar.ui.components.PrimaryButton
import dev.imhamid.gymyar.ui.components.ScreenSkeleton
import dev.imhamid.gymyar.ui.components.SecondaryButton
import dev.imhamid.gymyar.ui.components.UiEvents
import dev.imhamid.gymyar.ui.components.UpdateBanner
import dev.imhamid.gymyar.ui.components.UpdateDialog
import dev.imhamid.gymyar.ui.theme.GymyarTheme
import dev.imhamid.gymyar.ui.theme.ThemeMode

class MainActivity : ComponentActivity() {
    private val appVm: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val mode by appVm.themeMode.collectAsStateWithLifecycle()
            val dark = when (mode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            val view = LocalView.current
            SideEffect {
                val window = (view.context as? Activity)?.window
                if (window != null) {
                    val c = WindowCompat.getInsetsController(window, view)
                    c.isAppearanceLightStatusBars = !dark
                    c.isAppearanceLightNavigationBars = !dark
                }
            }
            GymyarTheme(mode) { AppRoot(appVm) }
        }
    }

    override fun onStart() {
        super.onStart()
        appVm.checkForUpdate()
    }
}

@Composable
private fun AppRoot(vm: AppViewModel) {
    val state by vm.state.collectAsStateWithLifecycle()
    val update by vm.update.collectAsStateWithLifecycle()
    val snack = remember { SnackbarHostState() }
    LaunchedEffect(Unit) { UiEvents.messages.collect { snack.showSnackbar(it) } }

    AppBackground {
    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(snack) },
    ) { inner ->
        Column(Modifier.fillMaxSize().padding(inner)) {
        update?.let { UpdateBanner(it, onClick = vm::openUpdateDialog) }
        Box(Modifier.fillMaxWidth().weight(1f)) {
            when (val s = state) {
                AppViewModel.State.Loading -> Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                    BrandMark()
                    ScreenSkeleton()
                }
                AppViewModel.State.LoggedOut -> AuthScreen()
                is AppViewModel.State.Failed -> Column(
                    Modifier.fillMaxSize().padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterVertically),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    BrandMark()
                    Text(s.message, style = MaterialTheme.typography.bodyMedium)
                    PrimaryButton("تلاش دوباره", onClick = { vm.load() })
                    SecondaryButton("خروج از حساب", onClick = { vm.logout() })
                }
                is AppViewModel.State.Ready -> {
                    if (s.me.isCoach) CoachApp(me = s.me, app = vm) else AthleteApp(me = s.me, app = vm)
                    NotificationPermissionPrompt()
                }
            }
        }
        }
    }
    }

    update?.takeIf { it.dialog }?.let {
        UpdateDialog(
            state = it,
            onDismiss = vm::closeUpdateDialog,
            onDownload = vm::startDownload,
            onCancel = vm::cancelDownload,
            onInstall = vm::installUpdate,
        )
    }
}
