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

/** Home tab before the athlete is an approved member: join a gym, or wait for the coach. */
@Composable
private fun HomeContent(me: Me, vm: AthleteViewModel, st: AthleteViewModel.UiState, onScan: () -> Unit) {
    val g = MaterialTheme.gym
    val scope = rememberCoroutineScope()
    val membership = st.data.membership
    when {
        membership == null -> ScreenColumn {
            if (st.stale) Banner("حالت آفلاین: آخرین اطلاعات ذخیره‌شده نمایش داده می‌شود.", g.warning)
            PageHeader("خوش آمدید، ${me.firstName}", "برای شروع به یک باشگاه بپیوندید.", Icons.Rounded.FitnessCenter)
            JoinGymCard(vm)
        }
        !membership.isApproved -> ScreenColumn {
            PageHeader("منتظر تایید مربی", membership.gym.name, Icons.Rounded.HourglassTop)
            AppCard {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
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
        else -> AthleteHome(me, membership, st.stale, onScan = onScan)
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
    val approved = membership?.isApproved == true
    var scanning by rememberSaveable { mutableStateOf(false) }

    val specs = if (approved) listOf(
        NavSpec("home", "خانه", Icons.Rounded.Home),
        NavSpec("attendance", "حضور", Icons.Rounded.EventAvailable),
        NavSpec("scan", "ثبت حضور با QR", Icons.Rounded.QrCodeScanner, center = true),
        NavSpec("club", "باشگاه", Icons.Rounded.FitnessCenter),
        NavSpec("profile", "پروفایل", Icons.Rounded.Person),
    ) else listOf(
        NavSpec("home", "خانه", Icons.Rounded.Home),
        NavSpec("profile", "پروفایل", Icons.Rounded.Person),
    )
    val motion = remember(approved) { NavMotion(specs.filter { !it.center }.map { it.route }) }

    Box(Modifier.fillMaxSize()) {
        when {
            !st.loaded && st.error == null -> ScreenSkeleton()
            !st.loaded -> Column(
                Modifier.fillMaxSize().padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                ErrorText(st.error)
                PrimaryButton("تلاش دوباره", onClick = { vm.refresh() })
                SecondaryButton("خروج از حساب", onClick = { app.logout() })
            }
            // The scanner is full screen, without the bottom bar.
            scanning -> {
                BackHandler { scanning = false }
                ScannerScreen(vm = vm, onClose = { scanning = false })
            }
            else -> Column(Modifier.fillMaxSize()) {
                Box(Modifier.weight(1f)) {
                    RefreshBox(refreshing = st.refreshing, onRefresh = { vm.refresh() }) {
                        NavHost(
                            navController = nav, startDestination = "home", modifier = Modifier.fillMaxSize(),
                            enterTransition = motion.enter, exitTransition = motion.exit,
                            popEnterTransition = motion.popEnter, popExitTransition = motion.popExit,
                        ) {
                            composable("home") { HomeContent(me, vm, st, onScan = { scanning = true }) }
                            composable("attendance") {
                                if (membership != null && approved) AthleteAttendance(membership, st.stale)
                                else HomeContent(me, vm, st, onScan = { scanning = true })
                            }
                            composable("club") {
                                if (membership != null && approved) AthleteClub(membership, st.stale)
                                else HomeContent(me, vm, st, onScan = { scanning = true })
                            }
                            composable("profile") {
                                ProfileScreen(me, app, membership?.gym?.name, Modifier.fillMaxSize()) {
                                    if (membership != null && approved) AthleteBodyCard(membership)
                                }
                            }
                        }
                    }
                }
                FloatingNavBar(
                    items = specs, selected = route,
                    onSelect = { r -> if (r == "scan") scanning = true else nav.goTab(r) },
                )
            }
        }
    }
}
