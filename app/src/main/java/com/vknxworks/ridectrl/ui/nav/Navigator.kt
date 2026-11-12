package com.vknxworks.ridectrl.ui.nav

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.navigation.NavHostController

/** One place that knows where things open. */
class Navigator(val nav: NavHostController) {

    fun navigate(route: Any) = nav.navigate(route) { launchSingleTop = true }

    fun back() {
        nav.popBackStack()
    }

    /** Opens [route] in place of the current screen (after saving a new vehicle, its dashboard replaces the editor). */
    fun replace(route: Any) {
        val current = nav.currentBackStackEntry?.destination?.id
        nav.navigate(route) {
            launchSingleTop = true
            if (current != null) popUpTo(current) { inclusive = true }
        }
    }

    /** Back to the Garage, closing everything above it (after deleting a vehicle). */
    fun toGarage() {
        nav.popBackStack(GarageRoute, inclusive = false)
    }
}

val LocalNavigator = staticCompositionLocalOf<Navigator> { error("No navigator") }
