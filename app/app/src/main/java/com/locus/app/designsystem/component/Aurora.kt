package com.locus.app.designsystem.component

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import com.locus.app.designsystem.theme.Amber
import com.locus.app.designsystem.theme.Smoke
import kotlin.math.cos
import kotlin.math.sin

private const val TWO_PI = (Math.PI * 2).toFloat()

/** 无限循环相位：0 → 2π，供各类光效驱动 */
@Composable
fun rememberAuroraPhase(durationMillis: Int, label: String = "auroraPhase"): Float {
    val transition = rememberInfiniteTransition(label = label)
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = TWO_PI,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "phase",
    )
    return phase
}

/**
 * 动态眩光背景：数团琥珀/烟雾色光斑沿利萨茹轨迹缓慢漂移。
 * 放在屏幕根 Box 的最底层，透明度压得很低，不影响可读性。
 */
@Composable
fun AuroraBackground(
    modifier: Modifier = Modifier,
    intensity: Float = 1f,
) {
    val phase = rememberAuroraPhase(16000)
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val maxDim = maxOf(w, h)

        data class Blob(
            val color: Color,
            val alpha: Float,
            val radiusScale: Float,
            val cx: Float,
            val cy: Float,
        )

        val blobs = listOf(
            // 主琥珀光：顶部大范围游走
            Blob(
                Amber, 0.16f * intensity, 0.75f,
                cx = w * (0.5f + 0.38f * sin(phase)),
                cy = h * (0.22f + 0.16f * cos(phase * 0.7f)),
            ),
            // 副琥珀光：反向漂移，与主光交汇时产生明暗流动
            Blob(
                Amber, 0.10f * intensity, 0.55f,
                cx = w * (0.5f + 0.42f * sin(-phase * 0.8f + 1.3f)),
                cy = h * (0.45f + 0.25f * cos(phase * 0.5f + 2.1f)),
            ),
            // 烟雾冷光：底部托底，避免下半身死黑
            Blob(
                Smoke, 0.05f * intensity, 0.65f,
                cx = w * (0.3f + 0.3f * sin(phase * 0.6f + 4f)),
                cy = h * (0.85f + 0.1f * cos(phase * 0.9f)),
            ),
            // 眩光亮点：小而亮，快速划过，"大理石纹"的魂
            Blob(
                Amber, 0.20f * intensity, 0.22f,
                cx = w * (0.5f + 0.45f * sin(phase * 1.6f + 0.8f)),
                cy = h * (0.3f + 0.22f * sin(phase * 1.1f + 3.6f)),
            ),
        )

        blobs.forEach { blob ->
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        blob.color.copy(alpha = blob.alpha),
                        blob.color.copy(alpha = 0f),
                    ),
                    center = Offset(blob.cx, blob.cy),
                    radius = maxDim * blob.radiusScale,
                ),
                radius = maxDim * blob.radiusScale,
                center = Offset(blob.cx, blob.cy),
            )
        }
    }
}

/**
 * 卡片内局部流光：一条对角高光带缓慢扫过 + 角落呼吸光斑。
 * 依赖父级卡片已 clip 圆角。放进卡片 Box 并用 matchParentSize。
 */
@Composable
fun CardShimmer(
    modifier: Modifier = Modifier,
    intensity: Float = 1f,
    phaseOffset: Float = 0f,
) {
    val phase = rememberAuroraPhase(9000, label = "cardShimmer")
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val p = phase + phaseOffset

        // 扫光带：沿对角线方向平移的窄高光
        val sweepPos = (sin(p) + 1f) / 2f // 0..1
        val bandCenter = (-0.3f + sweepPos * 1.6f).coerceIn(0.02f, 0.98f)
        val bandWidth = 0.22f
        drawRect(
            brush = Brush.linearGradient(
                colorStops = arrayOf(
                    0f to Color.Transparent,
                    (bandCenter - bandWidth).coerceAtLeast(0.01f) to Color.Transparent,
                    bandCenter to Amber.copy(alpha = 0.07f * intensity),
                    (bandCenter + bandWidth).coerceAtMost(0.99f) to Color.Transparent,
                    1f to Color.Transparent,
                ),
                start = Offset.Zero,
                end = Offset(w, h),
            ),
        )

        // 右上角呼吸光斑
        val breathe = (sin(p * 1.3f) + 1f) / 2f
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Amber.copy(alpha = (0.05f + 0.06f * breathe) * intensity),
                    Amber.copy(alpha = 0f),
                ),
                center = Offset(w * 0.88f, h * 0.15f),
                radius = w * 0.5f,
            ),
            radius = w * 0.5f,
            center = Offset(w * 0.88f, h * 0.15f),
        )
    }
}

/** 按压回弹：按下缩到 0.96，松手弹回（弹簧曲线）。替代死板的水波纹。 */
fun Modifier.bounceClick(
    scaleDown: Float = 0.96f,
    onClick: () -> Unit,
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) scaleDown else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "bouncePress",
    )
    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = onClick,
        )
}

/** 呼吸透明度：min..max 之间缓慢往返，用于光晕、边框等 */
@Composable
fun rememberBreathingAlpha(
    min: Float,
    max: Float,
    durationMillis: Int = 3000,
    label: String = "breathing",
): Float {
    val transition = rememberInfiniteTransition(label = label)
    val alpha by transition.animateFloat(
        initialValue = min,
        targetValue = max,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis / 2, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "alpha",
    )
    return alpha
}
