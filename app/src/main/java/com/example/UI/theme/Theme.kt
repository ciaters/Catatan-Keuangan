package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LightColorScheme =
  lightColorScheme(
    primary = EmeraldPrimary,
    onPrimary = Color.White,
    primaryContainer = EmeraldContainer,
    onPrimaryContainer = OnEmeraldContainer,
    secondary = TealSecondary,
    onSecondary = Color.White,
    secondaryContainer = TealSecondaryContainer,
    onSecondaryContainer = OnTealSecondaryContainer,
    tertiary = WarmAmber,
    onTertiary = Color.White,
    tertiaryContainer = WarmAmberContainer,
    onTertiaryContainer = OnWarmAmberContainer,
    background = CanvasBackground,
    onBackground = InkPrimary,
    surface = CardSurface,
    onSurface = InkPrimary,
    surfaceVariant = SurfaceVariantSoft,
    onSurfaceVariant = InkSecondary,
    surfaceContainerLowest = CardSurface,
    surfaceContainerLow = SurfaceTinted,
    surfaceContainer = CardSurface,
    surfaceContainerHigh = SurfaceContainerHigh,
    outline = OutlineSubtle,
    outlineVariant = Color(0xFFE3ECE8),
    error = ExpenseRed,
    onError = Color.White,
    errorContainer = ExpenseRedBg,
    onErrorContainer = Color(0xFF7F1D1D),
  )

val AppShapes =
  Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp),
  )

@Composable
fun CatatanKeuanganTheme(content: @Composable () -> Unit) {
  MaterialTheme(
    colorScheme = LightColorScheme,
    typography = AppTypography,
    shapes = AppShapes,
    content = content,
  )
}
