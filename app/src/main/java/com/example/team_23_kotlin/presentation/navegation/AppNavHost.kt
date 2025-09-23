package com.example.team_23_kotlin.presentation.navegation

import android.app.Application
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.team_23_kotlin.presentation.chat.ChatScreen
import com.example.team_23_kotlin.presentation.chatlist.ChatListScreen
import com.example.team_23_kotlin.presentation.product.ProductScreen
import com.example.team_23_kotlin.presentation.seller.SellerScreen
import com.example.team_23_kotlin.presentation.shared.LocationViewModel
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.team_23_kotlin.presentation.auth.LoginAuthViewModel
import com.example.team_23_kotlin.presentation.auth.SignUpAuthViewModel
import com.example.team_23_kotlin.presentation.auth.SignUpScreen
import com.example.team_23_kotlin.presentation.categories.CategoryFeedScreen
import com.example.team_23_kotlin.presentation.confirmpurchase.ConfirmPurchaseScreen
import com.example.team_23_kotlin.presentation.confirmpurchase.ConfirmPurchaseViewModel


/** ===================== Rutas ===================== **/
object Routes {
    const val HOME = "home"
    const val LOGIN = "auth/login"
    const val SIGNUP = "auth/signup"
    const val PROFILE = "profile"
    const val EDIT_PROFILE = "edit_profile"
    const val CATEGORIES = "categories"
    const val CATEGORY_FEED = "category/{categoryId}/{categoryTitle}"
    fun categoryFeed(categoryId: String, categoryTitle: String) =
        "category/$categoryId/${Uri.encode(categoryTitle)}"

    const val POST = "post"
    const val CHAT = "chat/{chatId}"
    fun chat(chatId: String) = "chat/${Uri.encode(chatId)}"
    const val PRODUCT = "product/{productId}"
    fun product(productId: String) = "product/${Uri.encode(productId)}"
    const val SELLER = "seller/{sellerId}"
    fun seller(sellerId: String) = "seller/${Uri.encode(sellerId)}"
    const val CHATLIST = "chatlist"
    const val CONFIRMPURCHASE = "confirmpurchase/{chatId}"
    fun confirmPurchase(chatId: String) = "confirmpurchase/${Uri.encode(chatId)}"
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
        route = Routes.CHATLIST,
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

    val locationViewModel: LocationViewModel = hiltViewModel()

    val noBottomBarRoutes = setOf(Routes.LOGIN, Routes.SIGNUP,Routes.EDIT_PROFILE, Routes.CHAT, Routes.CONFIRMPURCHASE)

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
            startDestination = Routes.LOGIN,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Routes.HOME) {
                HomeScreen(
                    onGoToAuth = { nav.navigate(Routes.LOGIN) },
                    onItemClick = { productId ->
                        nav.navigate("product/$productId")
                    }
                )
            }

            composable(Routes.CHATLIST) {
                ChatListScreen(
                    onOpenChat = { id ->
                        nav.navigate(Routes.chat(id)) {
                            launchSingleTop = true
                        }
                    }
                )
            }

            composable(
                route = Routes.CHAT,
                arguments = listOf(navArgument("chatId") { type = NavType.StringType })
            ) { backStackEntry ->
                val chatId = backStackEntry.arguments?.getString("chatId") ?: return@composable
                ChatScreen(
                    chatId = chatId,
                    onBack = { nav.popBackStack() },
                    onConfirmPurchase = {
                        nav.navigate(Routes.confirmPurchase(chatId))
                    }
                )
            }



            composable(Routes.LOGIN) {
                val vm: LoginAuthViewModel = hiltViewModel()
                var error by remember { mutableStateOf<String?>(null) }

                LoginScreen(
                    onLogin = { email, password, setLoading ->
                        setLoading(true)
                        vm.signIn(
                            email = email,
                            password = password,
                            onSuccess = {
                                setLoading(false)
                                // navega al Home y limpia el backstack de Login
                                nav.navigate(Routes.HOME) {
                                    popUpTo(Routes.LOGIN) { inclusive = true }
                                    launchSingleTop = true
                                }
                            },
                            onError = { msg ->
                                setLoading(false)
                                error = msg
                            }
                        )
                    },
                    onGoToSignUp = { nav.navigate(Routes.SIGNUP) },
                    errorMessage = error
                )
            }



            composable(Routes.SIGNUP) {
                val vm: SignUpAuthViewModel = hiltViewModel()
                var error by remember { mutableStateOf<String?>(null) }
                var showVerifyDialog by remember { mutableStateOf(false) }

                SignUpScreen(
                    onSubmit = { form ->
                        vm.register(
                            form = form,
                            onSuccess = {
                                error = null
                                showVerifyDialog = true
                            },
                            onError = { msg -> error = msg }
                        )
                    },
                    onGoToLogin = { nav.popBackStack() }
                )


                if (showVerifyDialog) {
                    AlertDialog(
                        onDismissRequest = { },
                        title = { Text("Verifica tu correo") },
                        text = {
                            Text(
                                "Hemos creado tu cuenta y enviamos un email de verificación a tu dirección institucional. " +
                                        "Por favor revisa tu bandeja y confirma para poder iniciar sesión."
                            )
                        },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    showVerifyDialog = false
                                    nav.popBackStack()
                                }
                            ) { Text("Entendido") }
                        },
                        dismissButton = {
                            TextButton(
                                onClick = {
                                    showVerifyDialog = false
                                    nav.popBackStack()
                                }
                            ) { Text("Ir a Login") }
                        }
                    )
                }
            }

            composable(Routes.PROFILE) {
                ProfileScreen(
                    locationViewModel= locationViewModel,
                    onGoToEdit = { nav.navigate(Routes.EDIT_PROFILE) }
                )
            }

            composable(Routes.EDIT_PROFILE) {
                EditProfileScreen(onBack = { nav.popBackStack() })
            }

            composable(Routes.CATEGORIES) {
                CategoriesScreen(
                    onCategoryClick = { categoryId ->
                        nav.navigate(Routes.categoryFeed(categoryId, "Category"))
                    }
                )
            }


            composable(
                route = Routes.CATEGORY_FEED,
                arguments = listOf(
                    navArgument("categoryId") { type = NavType.StringType },
                    navArgument("categoryTitle") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val categoryId = backStackEntry.arguments?.getString("categoryId") ?: return@composable
                val categoryTitle = backStackEntry.arguments?.getString("categoryTitle") ?: ""
                CategoryFeedScreen(
                    categoryId = categoryId,
                    categoryTitle = categoryTitle,
                    onBack = { nav.popBackStack() },
                    onItemClick = { productId -> nav.navigate(Routes.product(productId)) }
                )
            }



            composable(
                route = Routes.PRODUCT,
                arguments = listOf(navArgument("productId") { type = NavType.StringType })
            ) { backStackEntry ->
                val productId = backStackEntry.arguments?.getString("productId") ?: return@composable
                ProductScreen(productId = productId, onBack = { nav.popBackStack() }, nav = nav)
            }

            composable(Routes.POST) {
                PostScreen(
                    onBack = {},
                    onAddPhotos = {},
                    onSubmit = { _, _, _ -> }
                )
            }

            composable(
                Routes.SELLER,
                arguments = listOf(navArgument("sellerId") { type = NavType.StringType })
            ) { backStackEntry ->
                val sellerId = backStackEntry.arguments?.getString("sellerId") ?: return@composable
                SellerScreen(
                    sellerId = sellerId,
                    onBack = { nav.popBackStack() },
                    onProductClick = { productId -> nav.navigate("product/$productId") }
                )
            }

            composable(
                route = Routes.CONFIRMPURCHASE,
                arguments = listOf(navArgument("chatId") { type = NavType.StringType })
            ) { backStackEntry ->
                val chatId = backStackEntry.arguments?.getString("chatId") ?: return@composable
                ConfirmPurchaseScreen(
                    chatId = chatId,
                    onCancel = { nav.popBackStack() },
                    onPurchaseSuccess = {
                        // Navegar al chat de nuevo o a home
                        nav.navigate(Routes.chat(chatId)) {
                            popUpTo(Routes.CONFIRMPURCHASE) { inclusive = true }
                        }
                    },
                    viewModel = hiltViewModel<ConfirmPurchaseViewModel>()
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