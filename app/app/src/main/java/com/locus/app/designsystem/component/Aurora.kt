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
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import com.locus.app.designsystem.theme.Amber
import com.locus.app.designsystem.theme.InkBackground
import com.locus.app.designsystem.theme.Smoke
import kotlin.math.cos
import kotlin.math.sin

/**
 * 「潮汐」节拍体系：所有装饰性循环动画的周期必须是 BASE_PERIOD 的整数倍，
 * 相位同源，每 32s 全屏隐性同相一次。交互反馈层（bounceClick、转场、入场）不受约束。
 *
 * 4s  脉动环 / 呼吸点 / 辉光 / SOS 扩散环
 * 8s  光晕 / 流动边框 / 角落光斑
 * 16s 卡片流光
 * 32s 环境眩光 / 旋转弧光
 */
object LocusRhythm {
    const val BASE = 4000
    const val BREATH = BASE          // 4s
    const val GLOW = BASE * 2        // 8s
    const val SHIMMER = BASE * 4     // 16s
    const val AMBIENT = BASE * 8     // 32s
}

/** 系统「减弱动态效果」开关；true 时装饰动画降级为静态渲染（交互反馈保留） */
val LocalReduceMotion = staticCompositionLocalOf { false }

private const val TWO_PI = (Math.PI * 2).toFloat()

/** 无限循环相位：0 → 2π；减弱动态时固定为 0（静态帧） */
@Composable
fun rememberAuroraPhase(durationMillis: Int, label: String = "auroraPhase"): Float {
    if (LocalReduceMotion.current) return 0f
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
 * 动态眩光背景：数团琥珀/烟雾光斑沿利萨茹轨迹缓慢漂移 + 边缘压暗暗角。
 * 放在屏幕根 Box 的最底层。
 */
@Composable
fun AuroraBackground(
    modifier: Modifier = Modifier,
    intensity: Float = 1f,
) {
    val phase = rememberAuroraPhase(LocusRhythm.AMBIENT)
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
                Amber, 0.26f * intensity, 0.75f,
                cx = w * (0.5f + 0.38f * sin(phase)),
                cy = h * (0.22f + 0.16f * cos(phase * 0.7f)),
            ),
            // 副琥珀光：反向漂移，与主光交汇时产生明暗流动
            Blob(
                Amber, 0.16f * intensity, 0.55f,
                cx = w * (0.5f + 0.42f * sin(-phase * 0.8f + 1.3f)),
                cy = h * (0.45f + 0.25f * cos(phase * 0.5f + 2.1f)),
            ),
            // 烟雾冷光：底部托底，避免下半身死黑
            Blob(
                Smoke, 0.07f * intensity, 0.65f,
                cx = w * (0.3f + 0.3f * sin(phase * 0.6f + 4f)),
                cy = h * (0.85f + 0.1f * cos(phase * 0.9f)),
            ),
            // 眩光亮点：小而亮，缓慢划过，"大理石纹"的魂
            Blob(
                Amber, 0.30f * intensity, 0.26f,
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

        // 边缘压暗：暗角让中央光斑显形（电影打光）
        drawRect(
            brush = Brush.radialGradient(
                colorStops = arrayOf(
                    0f to Color.Transparent,
                    0.55f to Color.Transparent,
                    1f to InkBackground.copy(alpha = 0.6f),
                ),
                center = Offset(w / 2, h * 0.42f),
                radius = maxDim * 0.85f,
            ),
        )
    }
}

/**
 * 卡片内流光：「湿琥珀」漫射——一团大面积软光缓慢漂移 + 角落呼吸光斑。
 * 烛光透过琥珀的质感，不是手电筒扫塑料壳。
 * 依赖父级卡片已 clip 圆角。放进卡片 Box 并用 matchParentSize。
 */
@Composable
fun CardShimmer(
    modifier: Modifier = Modifier,
    intensity: Float = 1f,
    phaseOffset: Float = 0f,
) {
    val phase = rememberAuroraPhase(LocusRhythm.SHIMMER, label = "cardShimmer")
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val p = phase + phaseOffset

        // 漫射软光：大半径光斑沿水平方向缓慢往返，像烛光摇曳
        val drift = sin(p) // -1..1
        val glowCx = w * (0.5f + 0.3f * drift)
        val glowCy = h * (0.55f + 0.15f * cos(p * 0.7f))
        val glowAlpha = (0.05f + 0.03f * (drift + 1f) / 2f) * intensity
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Amber.copy(alpha = glowAlpha),
                    Amber.copy(alpha = 0f),
                ),
                center = Offset(glowCx, glowCy),
                radius = w * 0.75f,
            ),
            radius = w * 0.75f,
            center = Offset(glowCx, glowCy),
        )

        // 右上角呼吸光斑
        val breathe = (sin(p * 1.3f) + 1f) / 2f
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Amber.copy(alpha = (0.06f + 0.07f * breathe) * intensity),
                    Amber.copy(alpha = 0f),
                ),
                center = Offset(w * 0.88f, h * 0.15f),
                radius = w * 0.6f,
            ),
            radius = w * 0.6f,
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

/** 呼吸透明度：min..max 之间缓慢往返；减弱动态时取区间中值 */
@Composable
fun rememberBreathingAlpha(
    min: Float,
    max: Float,
    durationMillis: Int = LocusRhythm.BREATH,
    label: String = "breathing",
): Float {
    if (LocalReduceMotion.current) return (min + max) / 2f
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
