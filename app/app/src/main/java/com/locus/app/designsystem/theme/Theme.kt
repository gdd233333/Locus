package com.locus.app.designsystem.theme

import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.locus.app.designsystem.component.LocalReduceMotion

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
    // 跟随系统「关闭动画」设置（开发者选项里的动画时长缩放为 0）
    val context = LocalContext.current
    val reduceMotion = remember {
        Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f,
        ) == 0f
    }
    CompositionLocalProvider(
        LocalLocusColors provides LocusColors(),
        LocalReduceMotion provides reduceMotion,
    ) {
        content()
    }
}

// 便捷访问
object LocusTheme {
    val colors: LocusColors
        @Composable get() = LocalLocusColors.current
}
