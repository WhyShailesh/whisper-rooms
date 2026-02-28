package com.vanish.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.vanish.app.ui.screens.chat.ChatScreen
import com.vanish.app.ui.screens.home.HomeScreen
import com.vanish.app.ui.screens.room.RoomChatScreen
import com.vanish.app.ui.screens.setup.UsernameSetupScreen
import com.vanish.app.ui.screens.settings.SettingsScreen
import com.vanish.app.ui.screens.splash.SplashScreen

@Composable
fun NavGraph(
    navController: NavHostController
) {
    NavHost(
        navController = navController,
        startDestination = Route.Splash
    ) {
        composable(Route.Splash) {
            SplashScreen(
                onNavigateToSetup = { navController.navigate(Route.UsernameSetup) { popUpTo(Route.Splash) { inclusive = true } } },
                onNavigateToHome = { navController.navigate(Route.Home) { popUpTo(Route.Splash) { inclusive = true } } }
            )
        }
        composable(Route.UsernameSetup) {
            UsernameSetupScreen(
                onNavigateToHome = { navController.navigate(Route.Home) { popUpTo(Route.UsernameSetup) { inclusive = true } } }
            )
        }
        composable(Route.Home) {
            HomeScreen(
                onOpenChat = { peer -> navController.navigate(Route.Chat(peer)) },
                onOpenRoom = { code, isPending -> navController.navigate(Route.Room(code, isPending)) },
                onOpenSettings = { navController.navigate(Route.Settings) }
            )
        }
        composable(
            route = "chat/{peer}",
            arguments = listOf(navArgument("peer") { type = NavType.StringType })
        ) { backStackEntry ->
            val peer = backStackEntry.arguments?.getString("peer") ?: return@composable
            ChatScreen(
                peerUsername = peer,
                onBack = { navController.popBackStack() }
            )
        }
        composable(
            route = "room/{code}/{isPending}",
            arguments = listOf(
                navArgument("code") { type = NavType.StringType },
                navArgument("isPending") { type = NavType.BoolType; defaultValue = false }
            )
        ) { backStackEntry ->
            val code = backStackEntry.arguments?.getString("code") ?: return@composable
            val isPending = backStackEntry.arguments?.getBoolean("isPending") ?: false
            RoomChatScreen(
                roomCode = code,
                isPending = isPending,
                onBack = { navController.popBackStack() }
            )
        }
        composable(Route.Settings) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
    }
}

object Route {
    const val Splash = "splash"
    const val UsernameSetup = "setup"
    const val Home = "home"
    const val Settings = "settings"
    fun Chat(peer: String) = "chat/$peer"
    fun Room(code: String, isPending: Boolean = false) = "room/$code/$isPending"
}
