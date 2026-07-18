package com.pausenow.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** 应用主题：基于停一下品牌色（深青 #2E6B62）。各 Screen 复用。 */
@Composable
fun PauseNowTheme(content: @Composable () -> Unit) {
    val scheme = lightColorScheme(
        primary = Color(0xFF2E6B62),
        onPrimary = Color.White,
        primaryContainer = Color(0xFF9CF1E2),
        onPrimaryContainer = Color(0xFF00201B),
        secondary = Color(0xFF4A6363),
        surface = Color(0xFFFBFDFA),
        background = Color(0xFFFBFDFA),
    )
    MaterialTheme(colorScheme = scheme, content = content)
}
