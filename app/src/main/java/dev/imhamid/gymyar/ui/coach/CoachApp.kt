package dev.imhamid.gymyar.ui.coach

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
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
import dev.imhamid.gymyar.data.Me
import dev.imhamid.gymyar.ui.common.ProfileScreen
import dev.imhamid.gymyar.ui.components.*
import dev.imhamid.gymyar.ui.theme.gym

private fun NavController.goTab(route: String) = navigate(route) {
    popUpTo(graph.findStartDestination().id) { saveState = true }
    launchSingleTop = true
    restoreState = true
}

@Composable
private fun SubScreen(title: String, onBack: () -> Unit, content: @Composable () -> Unit) {
    AppBackground {
        Column(Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                RoundIcon(Icons.AutoMirrored.Rounded.ArrowBack, "بازگشت", onClick = onBack)
                Text(title, style = MaterialTheme.typography.titleLarge)
            }
            Box(Modifier.weight(1f)) { content() }
        }
    }
}

@Composable
private fun MoreRow(icon: ImageVector, tone: androidx.compose.ui.graphics.Color, title: String, hint: String, onClick: () -> Unit) {
    AppCard(onClick = onClick) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            IconBubble(icon, tone)
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(hint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Rounded.ChevronLeft, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun CoachApp(me: Me, app: AppViewModel) {
    val vm: CoachViewModel = viewModel()
    val st by vm.state.collectAsStateWithLifecycle()
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route
    val data = st.data
    val gym = data.gym
    val g = MaterialTheme.gym

    // Attendance is the daily action, so it is the big lime button in the middle of the bar.
    val specs = if (gym != null) listOf(
        NavSpec("overview", "نمای کلی", Icons.Rounded.Dashboard),
        NavSpec("members", "اعضا", Icons.Rounded.Groups),
        NavSpec("attendance", "حضور و غیاب", Icons.Rounded.EventAvailable, center = true),
        NavSpec("tuition", "شهریه", Icons.Rounded.Payments),
        NavSpec("more", "بیشتر", Icons.Rounded.Menu),
    ) else listOf(
        NavSpec("overview", "نمای کلی", Icons.Rounded.Dashboard),
        NavSpec("profile", "پروفایل", Icons.Rounded.Person),
    )
    val showBar = specs.any { it.route == route }
    val motion = remember(specs.size) { NavMotion(specs.map { it.route }.toSet()) }

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
            else -> Column(Modifier.fillMaxSize()) {
                Box(Modifier.weight(1f)) {
                    RefreshBox(refreshing = st.refreshing, onRefresh = { vm.refresh() }) {
                        NavHost(
                            navController = nav, startDestination = "overview", modifier = Modifier.fillMaxSize(),
                            enterTransition = motion.enter, exitTransition = motion.exit,
                            popEnterTransition = motion.popEnter, popExitTransition = motion.popExit,
                        ) {
                            composable("overview") { OverviewScreen(me, data, st.stale, vm) }
                            composable("members") { MembersScreen(data, st.stale, vm) }
                            composable("tuition") { TuitionScreen(data, st.stale, vm) }
                            composable("attendance") { AttendanceScreen(data, st.stale, vm) }
                            composable("more") {
                                ScreenColumn(spacing = 12.dp) {
                                    PageHeader("بیشتر", gym?.name, Icons.Rounded.Menu)
                                    MoreRow(Icons.Rounded.QrCode2, g.attendance, "QR ثبت حضور", "نمایش، چاپ و اشتراک‌گذاری") { nav.navigate("qr") }
                                    if (gym?.isOwner == true) {
                                        MoreRow(Icons.Rounded.Settings, g.tuition, "تنظیمات باشگاه", "روزها، ساعت، کد دعوت، پلن‌ها و مربیان") { nav.navigate("settings") }
                                    }
                                    MoreRow(Icons.Rounded.Person, g.members, "پروفایل", "اطلاعات حساب، تم و خروج") { nav.navigate("profile") }
                                }
                            }
                            composable("qr") { SubScreen("QR ثبت حضور", { nav.popBackStack() }) { QrScreen(vm) } }
                            composable("settings") { SubScreen("تنظیمات باشگاه", { nav.popBackStack() }) { SettingsScreen(data, vm) } }
                            composable("profile") {
                                if (gym == null) ProfileScreen(me, app, null, Modifier.fillMaxSize())
                                else SubScreen("پروفایل", { nav.popBackStack() }) { ProfileScreen(me, app, gym.name, Modifier.fillMaxSize()) }
                            }
                        }
                    }
                }
                if (showBar) {
                    FloatingNavBar(items = specs, selected = route, onSelect = { nav.goTab(it) })
                }
            }
        }
    }
}
