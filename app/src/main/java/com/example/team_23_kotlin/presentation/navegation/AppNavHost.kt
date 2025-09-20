package com.example.team_23_kotlin.presentation.navegation

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.example.team_23_kotlin.presentation.auth.AuthScreen
import com.example.team_23_kotlin.presentation.auth.LoginScreen
import com.example.team_23_kotlin.presentation.editprofile.EditProfileScreen
import com.example.team_23_kotlin.presentation.home.HomeScreen
import com.example.team_23_kotlin.presentation.profile.ProfileScreen
import com.example.team_23_kotlin.presentation.categories.CategoriesScreen
import com.example.team_23_kotlin.presentation.post.PostScreen
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.unit.sp
import android.net.Uri
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.team_23_kotlin.presentation.chat.ChatScreen

/** ===================== Rutas ===================== **/
object Routes {
    const val HOME = "home"
    const val AUTH = "auth"
    const val PROFILE = "profile"
    const val EDIT_PROFILE = "edit_profile"
    const val CATEGORIES = "categories"
    const val POST = "post"
    const val CHAT = "chat/{chatId}"
    fun chat(chatId: String) = "chat/${Uri.encode(chatId)}"

}

/** ===================== Bottom Destinations ===================== **/

private data class BottomDest(
    val route: String,
    val label: String,
    val iconUnselected: ImageVector,
    val iconSelected: ImageVector
)


private val bottomDestinations = listOf(
    BottomDest(
        route = Routes.HOME,
        label = "Home",
        iconUnselected = Icons.Outlined.Home,
        iconSelected = Icons.Filled.Home
    ),
    BottomDest(
        route = Routes.CATEGORIES,
        label = "Categories",
        iconUnselected = Icons.Outlined.List,
        iconSelected = Icons.Filled.List
    ),
    BottomDest(
        route = Routes.POST,
        label = "Post",
        iconUnselected = Icons.Outlined.AddCircle,
        iconSelected = Icons.Filled.AddCircle
    ),
    BottomDest(
        route = Routes.CHAT,
        label = "Messages",
        iconUnselected = Icons.Outlined.Email,
        iconSelected = Icons.Filled.Email
    ),
    BottomDest(
        route = Routes.PROFILE,
        label = "Profile",
        iconUnselected = Icons.Outlined.Person,
        iconSelected = Icons.Filled.Person
    ),
)

/** ===================== AppNavHost con BottomBar ===================== **/
@Composable
fun AppNavHost() {
    val nav = rememberNavController()

    val noBottomBarRoutes = setOf(Routes.AUTH, Routes.EDIT_PROFILE)

    val backStackEntry by nav.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            if (currentRoute !in noBottomBarRoutes) {
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

            composable(
                route = Routes.CHAT,
                arguments = listOf(navArgument("chatId") { type = NavType.StringType })
            ) { backStackEntry ->
                val chatId = backStackEntry.arguments!!.getString("chatId")!!
                ChatScreen(
                    chatId = chatId,
                    onBack = { nav.popBackStack() }

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
                    onGoToSignUp = {/* Todo */}
                )
            }
            composable(Routes.PROFILE) {
                ProfileScreen(onGoToEdit = { nav.navigate(Routes.EDIT_PROFILE) })
            }
            composable(Routes.EDIT_PROFILE) {
                EditProfileScreen(onBack = { nav.popBackStack() })
            }
            composable(Routes.CATEGORIES) {
                CategoriesScreen {
                }
            }

            composable(Routes.POST) {
                PostScreen(
                    onBack = {},
                    onAddPhotos = {},
                    onSubmit = { _, _, _ -> }
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
                        imageVector = if (selected) dest.iconSelected else dest.iconUnselected,
                        contentDescription = dest.label
                    )
                },
                label = {
                    Text(
                        dest.label,
                        fontSize = 12.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        maxLines = 1
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
