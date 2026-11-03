package com.sendlater.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.sendlater.ui.edit.EditScreen
import com.sendlater.ui.home.HomeScreen
import com.sendlater.ui.onboarding.OnboardingScreen
import com.sendlater.ui.settings.SettingsScreen

@Composable
fun AppNav(onReplayIntro: () -> Unit) {
    val nav = rememberNavController()
    NavHost(nav, startDestination = "home") {
        composable("home") {
            HomeScreen(
                onAdd = { nav.navigate("edit/0") },
                onEdit = { id -> nav.navigate("edit/$id") },
                onSettings = { nav.navigate("settings") }
            )
        }
        composable("edit/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) { entry ->
            EditScreen(
                id = entry.arguments?.getLong("id") ?: 0L,
                onDone = { nav.popBackStack() }
            )
        }
        composable("settings") {
            SettingsScreen(
                onBack = { nav.popBackStack() },
                onOnboarding = { nav.navigate("onboarding") },
                onReplayIntro = onReplayIntro
            )
        }
        composable("onboarding") { OnboardingScreen(onBack = { nav.popBackStack() }) }
    }
}
