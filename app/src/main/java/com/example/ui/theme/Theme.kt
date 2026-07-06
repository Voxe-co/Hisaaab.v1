package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

import androidx.compose.ui.graphics.Color

private val DarkColorScheme =
  darkColorScheme(
    primary = RoyalBlue,
    onPrimary = Color.White,
    secondary = RoyalBlueLight,
    onSecondary = Color.White,
    background = RichBlack,
    onBackground = Color.White,
    surface = DeepGraySurface,
    onSurface = Color.White,
    surfaceVariant = CardGrayDark,
    onSurfaceVariant = TextLightSecondary,
    outline = Color(0x0DFFFFFF),
    error = CrimsonRed
  )

private val LightColorScheme =
  lightColorScheme(
    primary = RoyalBlue,
    onPrimary = Color.White,
    secondary = RoyalBlueDark,
    onSecondary = Color.White,
    background = OffWhite,
    onBackground = Color(0xFF1C1C1E),
    surface = SoftGraySurface,
    onSurface = Color(0xFF1C1C1E),
    surfaceVariant = CardLight,
    onSurfaceVariant = TextDarkSecondary,
    outline = Color(0xFFE5E5EA),
    error = CrimsonRed
  )

@Composable
fun HisaabTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Custom branded colors look premium and unified, so we bypass dynamic system colors by default
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
