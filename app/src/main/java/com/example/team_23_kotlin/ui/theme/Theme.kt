package com.example.team_23_kotlin.ui.theme


import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = BrandPrimary,
    onPrimary = Color.White,
    secondary = BrandSecondary,
    background = Bg,
    surface = SurfaceLight,
    onBackground = TextPrimary,
    onSurface = TextPrimary,

    primaryContainer = PrimaryContainer,
    error = Error
)

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        content = content
    )
}
