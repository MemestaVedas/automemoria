package com.automemoria.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// ─── Palette ──────────────────────────────────────────────────────────────────

object AppColors {
    val Background       = Color(0xFF0F0F0F)
    val BackgroundLight  = Color(0xFFFAFAFA)
    val Surface          = Color(0xFF1A1A1A)
    val SurfaceLight     = Color(0xFFFFFFFF)
    val Primary          = Color(0xFF7C3AED)   // Deep purple
    val PrimaryVariant   = Color(0xFF9F67FF)
    val OnPrimary        = Color(0xFFFFFFFF)
    val Secondary        = Color(0xFF3B82F6)   // Blue — goals
    val Success          = Color(0xFF22C55E)   // Green — habit complete
    val Warning          = Color(0xFFF97316)   // Orange — streak
    val Error            = Color(0xFFEF4444)   // Red — high priority
    val TextPrimary      = Color(0xFFF1F1F1)
    val TextSecondary    = Color(0xFF9CA3AF)
    val Outline          = Color(0xFF2D2D2D)
    val OutlineLight     = Color(0xFFE5E7EB)
}

private val DarkColorScheme = darkColorScheme(
    primary          = AppColors.Primary,
    onPrimary        = AppColors.OnPrimary,
    primaryContainer = Color(0xFF4C1D95),
    secondary        = AppColors.Secondary,
    background       = AppColors.Background,
    surface          = AppColors.Surface,
    surfaceVariant   = Color(0xFF222222),
    onBackground     = AppColors.TextPrimary,
    onSurface        = AppColors.TextPrimary,
    onSurfaceVariant = AppColors.TextSecondary,
    outline          = AppColors.Outline,
    error            = AppColors.Error
)

private val LightColorScheme = lightColorScheme(
    primary          = AppColors.Primary,
    onPrimary        = AppColors.OnPrimary,
    primaryContainer = Color(0xFFEDE9FE),
    secondary        = AppColors.Secondary,
    background       = AppColors.BackgroundLight,
    surface          = AppColors.SurfaceLight,
    surfaceVariant   = Color(0xFFF3F4F6),
    onBackground     = Color(0xFF111827),
    onSurface        = Color(0xFF111827),
    onSurfaceVariant = Color(0xFF6B7280),
    outline          = AppColors.OutlineLight,
    error            = AppColors.Error
)

// ─── Typography ───────────────────────────────────────────────────────────────

val AppTypography = Typography(
    displayLarge  = TextStyle(fontWeight = FontWeight.Bold,   fontSize = 32.sp, lineHeight = 40.sp),
    headlineLarge = TextStyle(fontWeight = FontWeight.Bold,   fontSize = 24.sp, lineHeight = 32.sp),
    headlineMedium= TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 28.sp),
    titleLarge    = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 18.sp, lineHeight = 26.sp),
    titleMedium   = TextStyle(fontWeight = FontWeight.Medium,  fontSize = 16.sp, lineHeight = 24.sp),
    bodyLarge     = TextStyle(fontWeight = FontWeight.Normal,  fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium    = TextStyle(fontWeight = FontWeight.Normal,  fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall     = TextStyle(fontWeight = FontWeight.Normal,  fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge    = TextStyle(fontWeight = FontWeight.Medium,  fontSize = 14.sp, lineHeight = 20.sp),
    labelSmall    = TextStyle(fontWeight = FontWeight.Medium,  fontSize = 11.sp, lineHeight = 16.sp)
)

// ─── Theme ────────────────────────────────────────────────────────────────────

@Composable
fun AutomemoriaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,  // Material You on Android 12+
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = AppTypography,
        content     = content
    )
}
