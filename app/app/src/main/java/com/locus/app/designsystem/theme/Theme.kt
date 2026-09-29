package com.locus.app.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

data class LocusColors(
    val background: Color = InkBackground,
    val surface: Color = InkSurface,
    val surfaceVariant: Color = InkSurface2,
    val primary: Color = Amber,
    val primaryContainer: Color = AmberDim,
    val onBackground: Color = Smoke,
    val onSurface: Color = Smoke,
    val onSurfaceVariant: Color = Stone,
    val outline: Color = StoneDark,
    val outlineVariant: Color = Line,
)

val LocalLocusColors = staticCompositionLocalOf { LocusColors() }

@Composable
fun LocusTheme(
    content: @Composable () -> Unit
) {
    // 墨石只有深色主题，不跟随系统
    CompositionLocalProvider(
        LocalLocusColors provides LocusColors()
    ) {
        content()
    }
}

// 便捷访问
object LocusTheme {
    val colors: LocusColors
        @Composable get() = LocalLocusColors.current
}
