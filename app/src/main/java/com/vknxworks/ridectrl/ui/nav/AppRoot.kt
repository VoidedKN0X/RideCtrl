package com.vknxworks.ridectrl.ui.nav

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.annotation.StringRes
import com.vknxworks.ridectrl.R
import androidx.compose.ui.res.stringResource
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.TwoWheeler
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.vknxworks.ridectrl.container
import com.vknxworks.ridectrl.ui.components.LocalBottomClearance
import com.vknxworks.ridectrl.ui.components.CtrlBottomBar
import com.vknxworks.ridectrl.ui.components.TabItem
import com.vknxworks.ridectrl.ui.theme.CtrlColors
import androidx.navigation.toRoute
import com.vknxworks.ridectrl.ui.fuel.FuelEditScreen
import com.vknxworks.ridectrl.ui.fuel.FuelScreen
import com.vknxworks.ridectrl.ui.fuel.PremixScreen
import com.vknxworks.ridectrl.ui.garage.GarageScreen
import com.vknxworks.ridectrl.ui.garage.VehicleEditScreen
import com.vknxworks.ridectrl.ui.garage.VehicleScreen
import com.vknxworks.ridectrl.ui.logs.CostsScreen
import com.vknxworks.ridectrl.ui.logs.DocEditScreen
import com.vknxworks.ridectrl.ui.logs.DocsScreen
import com.vknxworks.ridectrl.ui.logs.NoteEditScreen
import com.vknxworks.ridectrl.ui.logs.NotesScreen
import com.vknxworks.ridectrl.ui.logs.OdometerEditScreen
import com.vknxworks.ridectrl.ui.logs.OdometerScreen
import com.vknxworks.ridectrl.ui.logs.TempEditScreen
import com.vknxworks.ridectrl.ui.logs.TempsScreen
import com.vknxworks.ridectrl.ui.logs.TripEditScreen
import com.vknxworks.ridectrl.ui.maintenance.MaintenanceScreen
import com.vknxworks.ridectrl.ui.maintenance.ServiceEditScreen
import com.vknxworks.ridectrl.ui.maintenance.ServiceItemEditScreen
import com.vknxworks.ridectrl.ui.parts.ModEditScreen
import com.vknxworks.ridectrl.ui.parts.ModsScreen
import com.vknxworks.ridectrl.ui.parts.PartEditScreen
import com.vknxworks.ridectrl.ui.parts.PartsScreen
import com.vknxworks.ridectrl.ui.parts.SpecsScreen
import com.vknxworks.ridectrl.ui.settings.BackupScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vknxworks.ridectrl.ui.settings.SettingsScreen
import com.vknxworks.ridectrl.ui.tools.ToolsScreen
import com.vknxworks.ridectrl.ui.settings.YouScreen

@Composable
fun AppRoot() {
    val ready by LocalContext.current.container.logs.ready.collectAsStateWithLifecycle()
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        // The logs load from disk in a moment; screens read them as soon as they open.
        if (ready) MainScaffold()
    }
}

private data class TopDest(val route: Any, @StringRes val label: Int, val icon: ImageVector, val selectedIcon: ImageVector)

/** The tab's icon and its name in the phone's language. */
@Composable
private fun TopDest.item(): TabItem = TabItem(stringResource(label), icon, selectedIcon)

/** Only content tabs: Settings, Backup and About open from the profile picture. */
private val topDestinations = listOf(
    TopDest(GarageRoute, R.string.tab_garage, Icons.Outlined.TwoWheeler, Icons.Filled.TwoWheeler),
    TopDest(ToolsRoute, R.string.tab_tools, Icons.Outlined.Calculate, Icons.Filled.Calculate),
)

@Composable
private fun MainScaffold() {
    val navController = rememberNavController()
    val navigator = remember(navController) { Navigator(navController) }
    val backStack by navController.currentBackStackEntryAsState()
    val dest = backStack?.destination
    val selectedIndex = topDestinations.indexOfFirst { top -> dest?.hierarchy?.any { it.hasRoute(top.route::class) } == true }

    fun selectTab(i: Int) {
        navController.navigate(topDestinations[i].route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    CompositionLocalProvider(LocalNavigator provides navigator) {
        PendingActions(navigator)
        BoxWithConstraints(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            if (maxWidth >= 600.dp) {
                // Tablets and unfolded foldables: a side rail.
                Row(Modifier.fillMaxSize()) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.background,
                        modifier = Modifier.fillMaxHeight().statusBarsPadding(),
                    ) {
                        Spacer(Modifier.weight(1f))
                        topDestinations.forEachIndexed { i, dest ->
                            val top = dest.item()
                            val selected = i == selectedIndex
                            NavigationRailItem(
                                selected = selected,
                                onClick = { selectTab(i) },
                                icon = { Icon(if (selected) top.selectedIcon else top.icon, contentDescription = top.label) },
                                label = { Text(top.label) },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = CtrlColors.OnAccent,
                                    selectedTextColor = CtrlColors.Text,
                                    indicatorColor = CtrlColors.Accent,
                                    unselectedIconColor = CtrlColors.Muted,
                                    unselectedTextColor = CtrlColors.Muted,
                                ),
                            )
                        }
                        Spacer(Modifier.weight(1f))
                    }
                    Box(Modifier.weight(1f).fillMaxHeight().navigationBarsPadding()) { AppNavHost(navController) }
                }
            } else {
                PhoneLayout(
                    navController = navController,
                    showTabs = selectedIndex >= 0,
                    selectedIndex = selectedIndex,
                    onSelect = ::selectTab,
                )
            }
        }
    }
}

/**
 * Phones: content runs edge to edge and scrolls under the floating bottom bar (20dp from the sides, 28dp
 * above the navigation bar). The bar shows on the main tabs; vehicles and other pushed screens get the
 * full height.
 */
@Composable
private fun PhoneLayout(
    navController: NavHostController,
    showTabs: Boolean,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
) {
    val density = LocalDensity.current
    var overlayHeight by remember { mutableStateOf(0.dp) }

    CompositionLocalProvider(LocalBottomClearance provides if (showTabs) overlayHeight else 0.dp) {
        Box(Modifier.fillMaxSize()) {
            AppNavHost(navController)
            AnimatedVisibility(
                visible = showTabs,
                enter = fadeIn() + slideInVertically { it / 2 },
                exit = fadeOut() + slideOutVertically { it / 2 },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .onSizeChanged { overlayHeight = with(density) { it.height.toDp() } },
            ) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(start = 20.dp, end = 20.dp, bottom = 28.dp),
                ) {
                    CtrlBottomBar(
                        items = topDestinations.map { it.item() },
                        selectedIndex = selectedIndex,
                        onSelect = onSelect,
                    )
                }
            }
        }
    }
}

@Composable
private fun AppNavHost(navController: NavHostController) {
    NavHost(navController, startDestination = GarageRoute) {
        composable<GarageRoute> { GarageScreen() }
        composable<ToolsRoute> { ToolsScreen() }
        composable<YouRoute> { YouScreen() }
        composable<SettingsRoute> { SettingsScreen() }
        composable<BackupRoute> { BackupScreen() }

        composable<VehicleRoute> { VehicleScreen(it.toRoute<VehicleRoute>().id) }
        composable<VehicleEditRoute> { VehicleEditScreen(it.toRoute<VehicleEditRoute>().id) }

        composable<FuelRoute> { FuelScreen(it.toRoute<FuelRoute>().vehicleId) }
        composable<FuelEditRoute> { val r = it.toRoute<FuelEditRoute>(); FuelEditScreen(r.vehicleId, r.id) }
        composable<PremixRoute> { PremixScreen(it.toRoute<PremixRoute>().vehicleId) }
        composable<EconomyRoute> { com.vknxworks.ridectrl.ui.fuel.EconomyScreen() }

        composable<MaintenanceRoute> { MaintenanceScreen(it.toRoute<MaintenanceRoute>().vehicleId) }
        composable<ServiceItemEditRoute> { val r = it.toRoute<ServiceItemEditRoute>(); ServiceItemEditScreen(r.vehicleId, r.id) }
        composable<ServiceEditRoute> { val r = it.toRoute<ServiceEditRoute>(); ServiceEditScreen(r.vehicleId, r.id, r.itemId) }

        composable<PartsRoute> { PartsScreen() }
        composable<PartEditRoute> { val r = it.toRoute<PartEditRoute>(); PartEditScreen(r.id, r.vehicleId) }
        composable<ModsRoute> { ModsScreen(it.toRoute<ModsRoute>().vehicleId) }
        composable<ModEditRoute> { val r = it.toRoute<ModEditRoute>(); ModEditScreen(r.vehicleId, r.id) }
        composable<SpecsRoute> { SpecsScreen(it.toRoute<SpecsRoute>().vehicleId) }

        composable<TempsRoute> { TempsScreen(it.toRoute<TempsRoute>().vehicleId) }
        composable<TempEditRoute> { val r = it.toRoute<TempEditRoute>(); TempEditScreen(r.vehicleId, r.id) }
        composable<OdometerRoute> { OdometerScreen(it.toRoute<OdometerRoute>().vehicleId) }
        composable<OdometerEditRoute> { val r = it.toRoute<OdometerEditRoute>(); OdometerEditScreen(r.vehicleId, r.id) }
        composable<TripEditRoute> { val r = it.toRoute<TripEditRoute>(); TripEditScreen(r.vehicleId, r.id) }

        composable<DocsRoute> { DocsScreen(it.toRoute<DocsRoute>().vehicleId) }
        composable<DocEditRoute> { val r = it.toRoute<DocEditRoute>(); DocEditScreen(r.vehicleId, r.id) }
        composable<NotesRoute> { NotesScreen(it.toRoute<NotesRoute>().vehicleId) }
        composable<NoteEditRoute> { val r = it.toRoute<NoteEditRoute>(); NoteEditScreen(r.vehicleId, r.id) }
        composable<CostsRoute> { CostsScreen(it.toRoute<CostsRoute>().vehicleId) }
    }
}

/**
 * Opens what a launcher shortcut, the widget or a reminder asked for ("fuel:<id>", "premix", "temp",
 * "maintenance:<id>"...). Shortcuts without a vehicle use the main vehicle.
 */
@Composable
private fun PendingActions(navigator: Navigator) {
    val c = LocalContext.current.container
    val action by c.pendingAction.collectAsStateWithLifecycle()
    val ready by c.logs.ready.collectAsStateWithLifecycle()
    LaunchedEffect(action, ready) {
        val a = action ?: return@LaunchedEffect
        if (!ready) return@LaunchedEffect
        c.pendingAction.value = null
        val data = c.logs.data.value
        val mainId = c.settings.settings.value.mainVehicleId
        val main = (data.vehicles.firstOrNull { it.id == mainId } ?: data.vehicles.minByOrNull { it.created })?.id
        val kind = a.substringBefore(':')
        val vid = a.substringAfter(':', "").ifBlank { null }?.takeIf { id -> data.vehicles.any { it.id == id } } ?: main
        when {
            kind == "premix" -> navigator.navigate(PremixRoute(vid))
            vid == null -> navigator.navigate(GarageRoute)
            kind == "fuel" -> { navigator.navigate(VehicleRoute(vid)); navigator.navigate(FuelEditRoute(vid)) }
            kind == "temp" -> { navigator.navigate(VehicleRoute(vid)); navigator.navigate(TempEditRoute(vid)) }
            kind == "maintenance" -> { navigator.navigate(VehicleRoute(vid)); navigator.navigate(MaintenanceRoute(vid)) }
            kind == "docs" -> { navigator.navigate(VehicleRoute(vid)); navigator.navigate(DocsRoute(vid)) }
            kind == "vehicle" -> navigator.navigate(VehicleRoute(vid))
            else -> Unit
        }
    }
}
