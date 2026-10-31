package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val AegisDarkColorScheme = darkColorScheme(
    primary = EmeraldPrimary,
    onPrimary = Color(0xFF022C22),
    primaryContainer = EmeraldContainer,
    onPrimaryContainer = OnEmeraldContainer,
    secondary = CyanPulse,
    onSecondary = Color(0xFF042F2E),
    secondaryContainer = CyanContainer,
    onSecondaryContainer = OnCyanContainer,
    tertiary = AmberGuardian,
    onTertiary = Color(0xFF271202),
    tertiaryContainer = AmberContainer,
    onTertiaryContainer = OnAmberContainer,
    error = CrimsonLock,
    onError = Color.White,
    errorContainer = CrimsonContainer,
    onErrorContainer = OnCrimsonContainer,
    background = ObsidianBackground,
    onBackground = TextPrimaryDark,
    surface = ObsidianSurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = ObsidianSurfaceElevated,
    onSurfaceVariant = TextSecondaryDark,
    outline = ObsidianGlassBorder,
    outlineVariant = Color(0xFF1E293B)
)

private val AegisLightColorScheme = lightColorScheme(
    primary = Color(0xFF059669),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD1FAE5),
    onPrimaryContainer = Color(0xFF064E3B),
    secondary = Color(0xFF0891B2),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFCFFAFE),
    onSecondaryContainer = Color(0xFF164E63),
    tertiary = Color(0xFFD97706),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFEF3C7),
    onTertiaryContainer = Color(0xFF78350F),
    error = Color(0xFFE11D48),
    onError = Color.White,
    errorContainer = Color(0xFFFFE4E6),
    onErrorContainer = Color(0xFF881337),
    background = AlabasterBackground,
    onBackground = TextPrimaryLight,
    surface = AlabasterSurface,
    onSurface = TextPrimaryLight,
    surfaceVariant = AlabasterSurfaceElevated,
    onSurfaceVariant = TextSecondaryLight,
    outline = Color(0xFFCBD5E1)
)

val AegisShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(26.dp),
    extraLarge = RoundedCornerShape(34.dp)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Default to sleek Obsidian Dark iOS aesthetic
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) AegisDarkColorScheme else AegisLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = AegisShapes,
        content = content
    )
}
