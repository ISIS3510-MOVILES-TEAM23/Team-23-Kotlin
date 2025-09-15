package com.example.team_23_kotlin.presentation.navegation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

import com.example.team_23_kotlin.presentation.home.HomeScreen
import com.example.team_23_kotlin.presentation.auth.AuthScreen


object Routes { const val HOME = "home"; const val AUTH = "auth" }

@Composable
fun AppNavHost() {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = Routes.HOME) {
        composable(Routes.HOME) { HomeScreen(onGoToAuth = { nav.navigate(Routes.AUTH) }) }
        composable(Routes.AUTH) { AuthScreen(onBack = { nav.popBackStack() }) }
    }
}

