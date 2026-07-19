package com.pausenow.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val PauseGreen = Color(0xFF174D3C)
val PauseGreenLight = Color(0xFFDDF2E8)
val PauseMint = Color(0xFF8DD5B7)
val PauseBackground = Color(0xFFF5F7F3)
val PauseText = Color(0xFF17201C)
val PauseTextMuted = Color(0xFF647069)

/** Sprint 1 视觉令牌：低刺激、行动导向的绿色体系。 */
@Composable
fun PauseNowTheme(content: @Composable () -> Unit) {
    val scheme = lightColorScheme(
        primary = PauseGreen,
        onPrimary = Color.White,
        primaryContainer = PauseGreenLight,
        onPrimaryContainer = PauseGreen,
        secondary = Color(0xFF557064),
        secondaryContainer = Color(0xFFE4EEE8),
        onSecondaryContainer = PauseText,
        surface = Color.White,
        onSurface = PauseText,
        onSurfaceVariant = PauseTextMuted,
        background = PauseBackground,
        surfaceVariant = Color(0xFFE9EEEA),
        outline = Color(0xFFB7C1BB),
        error = Color(0xFFB3261E),
        errorContainer = Color(0xFFFFDAD6),
    )
    val typography = Typography(
        headlineMedium = TextStyle(fontSize = 28.sp, lineHeight = 34.sp, fontWeight = FontWeight.Bold),
        titleLarge = TextStyle(fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.Bold),
        titleMedium = TextStyle(fontSize = 17.sp, lineHeight = 24.sp, fontWeight = FontWeight.SemiBold),
        bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 24.sp),
        bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 21.sp),
        labelLarge = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold),
    )
    val shapes = Shapes(
        small = androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
        medium = androidx.compose.foundation.shape.RoundedCornerShape(18.dp),
        large = androidx.compose.foundation.shape.RoundedCornerShape(26.dp),
    )
    MaterialTheme(colorScheme = scheme, typography = typography, shapes = shapes, content = content)
}
