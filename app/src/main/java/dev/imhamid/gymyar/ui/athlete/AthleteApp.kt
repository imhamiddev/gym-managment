package dev.imhamid.gymyar.ui.athlete

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import dev.imhamid.gymyar.AppViewModel
import dev.imhamid.gymyar.data.Api
import dev.imhamid.gymyar.data.Me
import dev.imhamid.gymyar.ui.common.ProfileScreen
import dev.imhamid.gymyar.ui.components.*
import dev.imhamid.gymyar.ui.theme.gym
import kotlinx.coroutines.launch
import kotlinx.serialization.json.put

private data class NavTab(val route: String, val label: String, val icon: ImageVector)

private fun NavController.goTab(route: String) = navigate(route) {
    popUpTo(graph.findStartDestination().id) { saveState = true }
    launchSingleTop = true
    restoreState = true
}

@Composable
private fun JoinGymCard(vm: AthleteViewModel) {
    var code by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    fun submit() {
        if (loading) return
        loading = true; error = null
        scope.launch {
            error = vm.act("join_gym", Api.params { put("p_code", code) })
            loading = false
        }
    }
    AppCard {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionTitle("عضویت در باشگاه")
            Text(
                "کد باشگاه را از مربی بگیرید و اینجا وارد کنید. بعد از تایید مربی، پنل شما فعال می‌شود.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            AppField(code, { code = it.uppercase() }, "کد باشگاه", ltr = true, placeholder = "مثلاً K7M2QX", imeAction = ImeAction.Done, onDone = { submit() })
            ErrorText(error)
            PrimaryButton("ارسال درخواست عضویت", loading = loading, onClick = { submit() })
        }
    }
}

@Composable
fun AthleteApp(me: Me, app: AppViewModel) {
    val vm: AthleteViewModel = viewModel()
    val st by vm.state.collectAsStateWithLifecycle()
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route
    val membership = st.data.membership
    val g = MaterialTheme.gym
    val scope = rememberCoroutineScope()

    val tabs = listOf(
        NavTab("home", "پنل من", Icons.Rounded.Dashboard),
        NavTab("profile", "پروفایل", Icons.Rounded.Person),
    )

    if (!st.loaded && st.error == null) {
        ScreenSkeleton()
        return
    }
    if (!st.loaded) {
        Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterVertically), horizontalAlignment = Alignment.CenterHorizontally) {
            ErrorText(st.error)
            PrimaryButton("تلاش دوباره", onClick = { vm.refresh() })
            SecondaryButton("خروج از حساب", onClick = { app.logout() })
        }
        return
    }

    // The scanner is full screen, without the bottom bar.
    var scanning by rememberSaveable { mutableStateOf(false) }
    if (scanning) {
        BackHandler { scanning = false }
        ScannerScreen(vm = vm, onClose = { scanning = false })
        return
    }

    Column(Modifier.fillMaxSize()) {
        Box(Modifier.weight(1f)) {
            RefreshBox(refreshing = st.refreshing, onRefresh = { vm.refresh() }) {
                NavHost(navController = nav, startDestination = "home", modifier = Modifier.fillMaxSize()) {
                    composable("home") {
                        when {
                            membership == null -> Column(
                                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                            ) {
                                if (st.stale) Banner("حالت آفلاین: آخرین اطلاعات ذخیره‌شده نمایش داده می‌شود.", g.warning)
                                PageHeader("خوش آمدید، ${me.firstName}", "برای شروع به یک باشگاه بپیوندید.", Icons.Rounded.FitnessCenter)
                                JoinGymCard(vm)
                            }
                            !membership.isApproved -> Column(
                                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                            ) {
                                PageHeader("منتظر تایید مربی", membership.gym.name, Icons.Rounded.HourglassTop, g.warning)
                                AppCard {
                                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                        Text(
                                            "درخواست شما برای باشگاه «${membership.gym.name}» ارسال شد. به‌محض اینکه مربی تایید کند، پنل شما فعال می‌شود. صفحه را به پایین بکشید تا به‌روز شود.",
                                            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                        SecondaryButton("لغو درخواست", onClick = {
                                            scope.launch { vm.act("cancel_request")?.let { UiEvents.toast(it) } }
                                        }, modifier = Modifier.fillMaxWidth(), tone = g.danger)
                                    }
                                }
                            }
                            else -> AthleteDashboard(me, membership, st.stale, onScan = { scanning = true })
                        }
                    }
                    composable("profile") { ProfileScreen(me, app, membership?.gym?.name, Modifier.fillMaxSize()) }
                }
            }
        }
        run {
            NavigationBar(windowInsets = WindowInsets(0, 0, 0, 0), containerColor = MaterialTheme.colorScheme.surface) {
                tabs.forEach { t ->
                    NavigationBarItem(
                        selected = route == t.route,
                        onClick = { nav.goTab(t.route) },
                        icon = { Icon(t.icon, contentDescription = null) },
                        label = { Text(t.label, style = MaterialTheme.typography.labelSmall, maxLines = 1) },
                    )
                }
            }
        }
    }
}
