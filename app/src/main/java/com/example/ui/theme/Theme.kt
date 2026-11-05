package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val VitalColorScheme = lightColorScheme(
  primary = VitalRed,
  onPrimary = VitalCard,
  secondary = VitalGreen,
  onSecondary = VitalCard,
  tertiary = VitalAmber,
  onTertiary = VitalCard,
  background = VitalBg,
  onBackground = VitalText,
  surface = VitalCard,
  onSurface = VitalText,
  surfaceVariant = VitalPanel,
  onSurfaceVariant = VitalTextMuted,
  outline = VitalBorder,
  error = VitalRed,
  onError = VitalCard,
)

@Composable
fun VitalTheme(
  content: @Composable () -> Unit,
) {
  // Vital strictly uses the prescribed clean soft off-white design system (no dark mode per spec)
  MaterialTheme(
    colorScheme = VitalColorScheme,
    typography = Typography,
    content = content
  )
}
