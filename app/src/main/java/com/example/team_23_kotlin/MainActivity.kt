package com.example.team_23_kotlin

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.navigation.compose.rememberNavController
import com.example.team_23_kotlin.presentation.navegation.AppNavHost
import com.example.team_23_kotlin.presentation.navegation.Routes
import com.example.team_23_kotlin.ui.theme.AppTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    // guardamos referencia al controlador
    private var navControllerHolder: androidx.navigation.NavHostController? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val postIdFromNotification = intent?.getStringExtra("postId")
        Log.d("FCM", "🔗 postId recibido en MainActivity: $postIdFromNotification")

        setContent {
            AppTheme {
                val navController = rememberNavController()
                navControllerHolder = navController  // guardamos referencia
                AppNavHost(
                    startPostId = postIdFromNotification,
                    navController = navController
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val postId = intent.getStringExtra("postId")
        if (postId != null) {
            Log.d("FCM", "🔄 Nuevo postId recibido: $postId")
            navControllerHolder?.navigate(Routes.product(postId)) // 👈 Navegar directamente
        }
    }
}
