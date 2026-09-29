package com.locus.app.feature.sober

import android.widget.Toast
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseInOutSine
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.locus.app.designsystem.component.AuroraBackground
import com.locus.app.designsystem.component.LocusRhythm
import com.locus.app.designsystem.component.bounceClick
import com.locus.app.designsystem.component.rememberAuroraPhase
import com.locus.app.designsystem.theme.Amber
import com.locus.app.designsystem.theme.InkBackground
import com.locus.app.designsystem.theme.InkSurface
import com.locus.app.designsystem.theme.LocusRadius
import com.locus.app.designsystem.theme.LocusTypography
import com.locus.app.designsystem.theme.Smoke
import com.locus.app.designsystem.theme.Stone
import com.locus.app.designsystem.theme.StoneDark
import kotlinx.coroutines.delay
import kotlin.math.sin

private const val SESSION_SECONDS = 10 * 60
private const val INHALE_MS = 4000
private const val HOLD_MS = 2000
private const val EXHALE_MS = 6000
private const val MAX_ORB_SCALE = 1.3f
private const val ORB_DIAMETER_DP = 220
private const val TWO_PI = (Math.PI * 2).toFloat()

private enum class BreathPhase(val label: String) {
    INHALE("吸气"),
    HOLD("屏息"),
    EXHALE("呼气"),
}

/** 冲浪练习全屏页：4-2-6 引导呼吸 + 倒计时 + 底部波浪，结束时确认是否平复 */
@Composable
fun SurfingExerciseScreen(
    viewModel: SurfingViewModel = viewModel(factory = SurfingViewModel.Factory),
    onFinish: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var remaining by remember { mutableIntStateOf(SESSION_SECONDS) }
    var showResult by remember { mutableStateOf(false) }
    var phase by remember { mutableStateOf(BreathPhase.INHALE) }
    val orbScale = remember { Animatable(1f) }

    // 呼吸循环：吸气 4s → 屏息 2s → 呼气 6s
    LaunchedEffect(Unit) {
        while (true) {
            phase = BreathPhase.INHALE
            orbScale.animateTo(MAX_ORB_SCALE, tween(INHALE_MS, easing = EaseInOutSine))
            phase = BreathPhase.HOLD
            delay(HOLD_MS.toLong())
            phase = BreathPhase.EXHALE
            orbScale.animateTo(1f, tween(EXHALE_MS, easing = EaseInOutSine))
        }
    }

    // 10 分钟倒计时
    LaunchedEffect(Unit) {
        while (remaining > 0) {
            delay(1000)
            remaining -= 1
        }
    }
    LaunchedEffect(remaining) {
        if (remaining == 0) showResult = true
    }

    val elapsedMinutes = ((SESSION_SECONDS - remaining) / 60).coerceAtLeast(1)

    Box(modifier = modifier.fillMaxSize().background(InkBackground)) {
        AuroraBackground(modifier = Modifier.matchParentSize(), intensity = 1.2f)
        BottomWaves(modifier = Modifier.align(Alignment.BottomCenter))

        Column(
            modifier = Modifier.fillMaxSize().padding(vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = formatCountdown(remaining),
                style = LocusTypography.labelLarge,
                color = Stone,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "跟着圆的节奏呼吸就好",
                style = LocusTypography.labelTiny,
                color = StoneDark,
            )
            Spacer(Modifier.weight(1f))
            BreathingOrb(scale = orbScale.value, phaseLabel = phase.label)
            Spacer(Modifier.weight(1f))
            Text(
                text = "提前结束",
                style = LocusTypography.labelMedium,
                color = StoneDark,
                modifier = Modifier
                    .bounceClick { showResult = true }
                    .padding(horizontal = 24.dp, vertical = 12.dp),
            )
        }
    }

    if (showResult) {
        AlertDialog(
            onDismissRequest = { /* 必须二选一 */ },
            containerColor = InkSurface,
            title = { Text("冲动平复了吗？", style = LocusTypography.titleLarge, color = Smoke) },
            text = {
                Text(
                    text = "不管选哪个，你已经陪它待了 $elapsedMinutes 分钟。",
                    style = LocusTypography.bodySmall,
                    color = Stone,
                )
            },
            confirmButton = {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(LocusRadius.full))
                        .background(Amber)
                        .bounceClick(scaleDown = 0.92f) {
                            viewModel.resolveLatestUrgeAndFinish(elapsedMinutes, onFinish)
                        }
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                ) {
                    Text("平复了", style = LocusTypography.labelMedium, color = InkBackground)
                }
            },
            dismissButton = {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(LocusRadius.full))
                        .bounceClick(scaleDown = 0.92f) {
                            Toast.makeText(context, "没关系，它自己会退下去", Toast.LENGTH_SHORT).show()
                            onFinish()
                        }
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                ) {
                    Text("还没有", style = LocusTypography.labelMedium, color = Stone)
                }
            },
        )
    }
}

private fun formatCountdown(totalSeconds: Int): String {
    val m = totalSeconds / 60
    val s = totalSeconds % 60
    return "%02d:%02d".format(m, s)
}

/** 中央呼吸圆：径向渐变光球 + 三层错相脉动光环 + 阶段文字 Crossfade */
@Composable
private fun BreathingOrb(scale: Float, phaseLabel: String) {
    val ringPhase = rememberAuroraPhase(4000, label = "orbRings")
    Box(
        modifier = Modifier
            .size(ORB_DIAMETER_DP.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            // 光球本体
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Amber.copy(alpha = 0.30f),
                        Amber.copy(alpha = 0.07f),
                        Color.Transparent,
                    ),
                    center = center,
                    radius = size.minDimension / 2,
                ),
                radius = size.minDimension / 2,
            )
            // 三层错相光环：半径与透明度随相位呼吸
            repeat(3) { index ->
                val k = (sin(ringPhase + index * (TWO_PI / 3f)) + 1f) / 2f
                drawCircle(
                    color = Amber.copy(alpha = 0.30f * (1f - k) + 0.05f),
                    radius = size.minDimension / 2 * (0.70f + 0.28f * k),
                    style = Stroke(width = 1.5.dp.toPx()),
                )
            }
        }
        Crossfade(
            targetState = phaseLabel,
            animationSpec = tween(500),
            label = "breathPhase",
        ) { label ->
            Text(label, style = LocusTypography.displaySmall, color = Smoke)
        }
    }
}

/** 底部双层波浪：两层 sin 波相位错开（8s / 4s 潮汐节拍），无限起伏 */
@Composable
private fun BottomWaves(modifier: Modifier = Modifier) {
    val phaseSlow = rememberAuroraPhase(LocusRhythm.GLOW, label = "waveSlow")
    val phaseFast = rememberAuroraPhase(LocusRhythm.BREATH, label = "waveFast")

    Canvas(modifier = modifier.fillMaxWidth().height(180.dp)) {
        val w = size.width
        val h = size.height

        data class Wave(val phase: Float, val amplitude: Float, val alpha: Float)
        listOf(
            Wave(phaseSlow, 0.16f, 0.10f),
            Wave(phaseFast, 0.24f, 0.16f),
        ).forEach { wave ->
            val baseY = h * (0.45f - wave.amplitude * 0.3f)
            val linePath = Path()
            val fillPath = Path()
            fillPath.moveTo(0f, h)
            var x = 0f
            val stepX = w / 48f
            var first = true
            while (x <= w + stepX) {
                val y = baseY + sin(x / w * TWO_PI * 2f + wave.phase) * h * wave.amplitude
                if (first) {
                    linePath.moveTo(x, y)
                    fillPath.lineTo(x, y)
                    first = false
                } else {
                    linePath.lineTo(x, y)
                    fillPath.lineTo(x, y)
                }
                x += stepX
            }
            fillPath.lineTo(w, h)
            fillPath.close()

            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(Amber.copy(alpha = wave.alpha), Color.Transparent),
                    startY = baseY - h * wave.amplitude,
                    endY = h,
                ),
            )
            drawPath(
                path = linePath,
                color = Amber.copy(alpha = wave.alpha * 0.5f),
                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round),
            )
        }
    }
}
