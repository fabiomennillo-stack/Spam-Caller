package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val SpamcallerDarkColorScheme = darkColorScheme(
    primary = ElectricAzure,
    onPrimary = Color.White,
    primaryContainer = RoyalCobalt,
    onPrimaryContainer = IceBlue,
    secondary = LuminousCyan,
    onSecondary = ObsidianBlack,
    secondaryContainer = DeepNavy,
    onSecondaryContainer = IceBlue,
    tertiary = SafeEmerald,
    onTertiary = Color.White,
    error = SpamCrimson,
    onError = Color.White,
    errorContainer = SpamCrimsonDark,
    onErrorContainer = Color(0xFFFEE2E2),
    background = DarkCanvas,
    onBackground = DarkInk,
    surface = DarkSurface,
    onSurface = DarkInk,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkInkSecondary,
    outline = Color(0xFF233A66),
    outlineVariant = Color(0xFF172747)
)

private val SpamcallerLightColorScheme = lightColorScheme(
    primary = TruecallerBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD7E6FF),
    onPrimaryContainer = Color(0xFF091B3D),
    secondary = ElectricAzure,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE0ECFF),
    onSecondaryContainer = Color(0xFF0D2554),
    tertiary = SafeEmerald,
    onTertiary = Color.White,
    error = SpamCrimson,
    onError = Color.White,
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = Color(0xFF7F1D1D),
    background = LightCanvas,
    onBackground = LightInk,
    surface = LightSurface,
    onSurface = LightInk,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightInkSecondary,
    outline = Color(0xFFB5C7E8),
    outlineVariant = Color(0xFFD4E0F5)
)

@Immutable
data class SpamcallerGradients(
    val isDark: Boolean,
    val screenBackground: Brush,
    val heroBlackBlue: Brush,
    val cardBlackBlue: Brush,
    val dialerButtonBrush: Brush,
    val accentBorderBrush: Brush
)

val LocalSpamcallerGradients = staticCompositionLocalOf {
    SpamcallerGradients(
        isDark = true,
        screenBackground = Brush.verticalGradient(listOf(ObsidianBlack, MidnightSlate, DeepNavy)),
        heroBlackBlue = Brush.linearGradient(listOf(ObsidianBlack, DeepNavy, RoyalCobalt)),
        cardBlackBlue = Brush.linearGradient(listOf(MidnightSlate, DeepNavy)),
        dialerButtonBrush = Brush.linearGradient(listOf(RoyalCobalt, TruecallerBlue)),
        accentBorderBrush = Brush.linearGradient(listOf(ElectricAzure, LuminousCyan))
    )
}

@Composable
fun SpamcallerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) SpamcallerDarkColorScheme else SpamcallerLightColorScheme

    val gradients = if (darkTheme) {
        SpamcallerGradients(
            isDark = true,
            screenBackground = Brush.verticalGradient(
                colors = listOf(ObsidianBlack, MidnightSlate, DeepNavy, Color(0xFF0A1B3D))
            ),
            heroBlackBlue = Brush.linearGradient(
                colors = listOf(ObsidianBlack, Color(0xFF0B1C3E), Color(0xFF153A7E))
            ),
            cardBlackBlue = Brush.linearGradient(
                colors = listOf(Color(0xFF081020), Color(0xFF0F2147))
            ),
            dialerButtonBrush = Brush.linearGradient(
                colors = listOf(Color(0xFF0F2E6E), TruecallerBlue)
            ),
            accentBorderBrush = Brush.linearGradient(
                colors = listOf(ElectricAzure.copy(alpha = 0.7f), LuminousCyan.copy(alpha = 0.5f))
            )
        )
    } else {
        SpamcallerGradients(
            isDark = false,
            screenBackground = Brush.verticalGradient(
                colors = listOf(Color(0xFFF6F9FF), Color(0xFFECF2FC), Color(0xFFDFE9FA))
            ),
            heroBlackBlue = Brush.linearGradient(
                colors = listOf(ObsidianBlack, DeepNavy, RoyalCobalt)
            ),
            cardBlackBlue = Brush.linearGradient(
                colors = listOf(Color(0xFF060B17), Color(0xFF102859))
            ),
            dialerButtonBrush = Brush.linearGradient(
                colors = listOf(DeepNavy, TruecallerBlue)
            ),
            accentBorderBrush = Brush.linearGradient(
                colors = listOf(TruecallerBlue.copy(alpha = 0.55f), DeepNavy.copy(alpha = 0.45f))
            )
        )
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                val controller = WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = !darkTheme
                controller.isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    CompositionLocalProvider(LocalSpamcallerGradients provides gradients) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
