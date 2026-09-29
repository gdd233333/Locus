package com.locus.app.designsystem.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically

object LocusMotion {
    // 缓动曲线
    val Spring = CubicBezierEasing(0.34f, 1.56f, 0.64f, 1.0f)
    val EaseOut = CubicBezierEasing(0.22f, 1.0f, 0.36f, 1.0f)

    // 时长
    const val DURATION_BUTTON = 150
    const val DURATION_CARD = 350
    const val DURATION_ENTER = 900
    const val DURATION_WAVE = 2500
    const val DURATION_PROGRESS = 1500
    const val DURATION_BREATHE = 3000
    const val DURATION_BORDER_FLOW = 4000
    const val DURATION_DOT_PULSE = 2000

    // 交错延迟
    val staggerDelays = listOf(0, 100, 250, 400, 550)

    // 标准入场动画
    fun fadeUpEnter(delayMillis: Int = 0) = fadeIn(
        animationSpec = tween(DURATION_ENTER, delayMillis, EaseOut)
    ) + slideInVertically(
        initialOffsetY = { it / 10 },
        animationSpec = tween(DURATION_ENTER, delayMillis, EaseOut)
    )
}
