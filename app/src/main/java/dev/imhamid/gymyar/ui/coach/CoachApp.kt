package dev.imhamid.gymyar.ui.coach

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

private data class NavTab(val route: String, val label: String, val icon: ImageVector)

private fun NavController.goTab(route: String) = navigate(route) {
    popUpTo(graph.findStartDestination().id) { saveState = true }
    launchSingleTop = true
    restoreState = true
}

@Composable
private fun SubScreen(title: String, onBack: () -> Unit, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "بازگشت") }
            Text(title, style = MaterialTheme.typography.titleMedium)
        }
        Box(Modifier.weight(1f)) { content() }
    }
}

@Composable
private fun MoreRow(icon: ImageVector, tone: androidx.compose.ui.graphics.Color, title: String, hint: String, onClick: () -> Unit) {
    AppCard(onClick = onClick) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
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

    val tabs = if (gym != null) listOf(
        NavTab("overview", "نمای کلی", Icons.Rounded.Dashboard),
        NavTab("members", "اعضا", Icons.Rounded.Groups),
        NavTab("tuition", "شهریه", Icons.Rounded.Payments),
        NavTab("attendance", "حضور", Icons.Rounded.EventAvailable),
        NavTab("more", "بیشتر", Icons.Rounded.Menu),
    ) else listOf(
        NavTab("overview", "نمای کلی", Icons.Rounded.Dashboard),
        NavTab("profile", "پروفایل", Icons.Rounded.Person),
    )
    val showBar = tabs.any { it.route == route }

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

    Column(Modifier.fillMaxSize()) {
        Box(Modifier.weight(1f)) {
            RefreshBox(refreshing = st.refreshing, onRefresh = { vm.refresh() }) {
                NavHost(navController = nav, startDestination = "overview", modifier = Modifier.fillMaxSize()) {
                    composable("overview") { OverviewScreen(me, data, st.stale, vm) { nav.goTab(it) } }
                    composable("members") { MembersScreen(data, st.stale, vm) }
                    composable("tuition") { TuitionScreen(data, st.stale, vm) }
                    composable("attendance") { AttendanceScreen(data, st.stale, vm) }
                    composable("more") {
                        Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            PageHeader("بیشتر", gym?.name, Icons.Rounded.Menu)
                            MoreRow(Icons.Rounded.QrCode2, g.attendance, "QR ثبت حضور", "نمایش، چاپ و اشتراک‌گذاری") { nav.navigate("qr") }
                            if (gym?.isOwner == true) {
                                MoreRow(Icons.Rounded.Settings, MaterialTheme.colorScheme.primary, "تنظیمات باشگاه", "روزها، ساعت، کد دعوت، پلن‌ها و مربیان") { nav.navigate("settings") }
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
