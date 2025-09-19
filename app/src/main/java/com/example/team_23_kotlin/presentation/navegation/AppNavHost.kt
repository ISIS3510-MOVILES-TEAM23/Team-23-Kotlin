package com.example.team_23_kotlin.presentation.navegation

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.team_23_kotlin.R
import com.example.team_23_kotlin.presentation.auth.LoginScreen
import com.example.team_23_kotlin.presentation.editprofile.EditProfileScreen
import com.example.team_23_kotlin.presentation.home.HomeScreen
import com.example.team_23_kotlin.presentation.profile.ProfileScreen
import com.example.team_23_kotlin.presentation.categories.CategoriesScreen
import com.example.team_23_kotlin.presentation.chatlist.ChatListScreen
import com.example.team_23_kotlin.presentation.post.PostScreen
import com.example.team_23_kotlin.presentation.chat.ChatScreen

/** ===================== Rutas ===================== **/
object Routes {
    const val HOME = "home"
    const val AUTH = "auth"
    const val PROFILE = "profile"
    const val EDIT_PROFILE = "edit_profile"
    const val CATEGORIES = "categories"
    const val POST = "post"
    const val MESSAGES = "messages"
    const val CHAT = "chat/{chatId}" // ruta dinámica
}

// Helper para construir la ruta de chat con ID
fun chatRoute(chatId: String) = "chat/$chatId"

/** ===================== Bottom Destinations ===================== **/
private data class BottomDest(
    val route: String,
    val label: String,
    @DrawableRes val iconUnselected: Int,
    @DrawableRes val iconSelected: Int
)

// Usa tus .webp en res/drawable
private val bottomDestinations = listOf(
    BottomDest(
        route = Routes.HOME,
        label = "Home",
        iconUnselected = R.drawable.home,
        iconSelected   = R.drawable.home_select
    ),
    BottomDest(
        route = Routes.CATEGORIES,
        label = "Categories",
        iconUnselected = R.drawable.categories,
        iconSelected   = R.drawable.categories_select
    ),
    BottomDest(
        route = Routes.POST,
        label = "Post",
        iconUnselected = R.drawable.post,
        iconSelected   = R.drawable.post_select
    ),
    BottomDest(
        route = Routes.MESSAGES,
        label = "Messages",
        iconUnselected = R.drawable.messages,
        iconSelected   = R.drawable.messages_select
    ),
    BottomDest(
        route = Routes.PROFILE,
        label = "Profile",
        iconUnselected = R.drawable.profile,
        iconSelected   = R.drawable.profile_select
    ),
)

/** ===================== AppNavHost con BottomBar ===================== **/
@Composable
fun AppNavHost() {
    val nav = rememberNavController()

    // Rutas donde NO queremos mostrar la bottom bar
    val noBottomBarRoutes = setOf(Routes.AUTH, Routes.EDIT_PROFILE)

    val backStackEntry by nav.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    // Cuando estás en chat/123, la route real es "chat/123" (no coincide con Routes.CHAT).
    // Por eso ocultamos si comienza por "chat/".
    val hideBottomBar = (currentRoute in noBottomBarRoutes) || (currentRoute?.startsWith("chat/") == true)

    Scaffold(
        bottomBar = {
            if (!hideBottomBar) {
                BottomBar(navController = nav, items = bottomDestinations)
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = nav,
            startDestination = Routes.AUTH,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Routes.HOME) {
                HomeScreen(
                    onGoToAuth = { nav.navigate(Routes.AUTH) },
                )
            }

            composable(Routes.AUTH) {
                LoginScreen(
                    onLoginSuccess = {
                        nav.navigate(Routes.HOME) {
                            popUpTo(Routes.AUTH) { inclusive = true } // borra el login de la pila
                            launchSingleTop = true
                        }
                    },
                    onGoToSignUp = { /* TODO */ }
                )
            }

            composable(Routes.PROFILE) {
                ProfileScreen(onGoToEdit = { nav.navigate(Routes.EDIT_PROFILE) })
            }

            composable(Routes.POST) {
                PostScreen()
            }

            composable(Routes.EDIT_PROFILE) {
                EditProfileScreen(onBack = { nav.popBackStack() })
            }

            composable(Routes.CATEGORIES) {
                val saveable = rememberSaveableStateHolder()
                saveable.SaveableStateProvider(Routes.CATEGORIES) {
                    CategoriesScreen()
                }
            }

            // Lista de chats
            composable(Routes.MESSAGES) {
                ChatListScreen(
                    onOpenChat = { chatId ->
                        nav.navigate(chatRoute(chatId)) // navegar al detalle del chat
                    }
                )
            }

            // Detalle del chat (recibe chatId)
            composable(Routes.CHAT) { backStackEntry ->
                val chatId = backStackEntry.arguments?.getString("chatId") ?: return@composable
                ChatScreen(
                    chatId = chatId,
                    onBack = { nav.popBackStack() }
                )
            }
        }
    }
}

@Composable
private fun BottomBar(
    navController: NavHostController,
    items: List<BottomDest>
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    NavigationBar(
        containerColor = Color.White,
        tonalElevation = 0.dp
    ) {
        items.forEach { dest ->
            val selected = currentDestination.isOnDestination(dest.route)

            NavigationBarItem(
                selected = selected,


                onClick = {
                    navController.navigate(dest.route) {
                        launchSingleTop = true
                        restoreState = true
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                    }
                },
                icon = {
                    Icon(
                        painter = painterResource(
                            id = if (selected) dest.iconSelected else dest.iconUnselected
                        ),
                        contentDescription = dest.label,
                        tint = Color.Unspecified
                    )
                },
                label = {
                    Text(
                        dest.label,
                        fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.ExtraBold
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = Color.Transparent,
                    selectedIconColor = Color.Black,
                    unselectedIconColor = Color.Unspecified,
                    selectedTextColor = Color.Black,
                    unselectedTextColor = Color.Gray
                ),
            )
        }
    }
}

private fun NavDestination?.isOnDestination(route: String): Boolean {
    return this?.hierarchy?.any { it.route == route } == true
}
