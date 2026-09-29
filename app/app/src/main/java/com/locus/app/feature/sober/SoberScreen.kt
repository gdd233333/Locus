package com.locus.app.feature.sober

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Waves
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.locus.app.designsystem.component.AuroraBackground
import com.locus.app.designsystem.component.CardShimmer
import com.locus.app.designsystem.component.SosButton
import com.locus.app.designsystem.component.bounceClick
import com.locus.app.designsystem.component.rememberAuroraPhase
import com.locus.app.designsystem.theme.*

@Composable
fun SoberScreen(
    viewModel: SoberViewModel = viewModel(),
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsState()

    Box(modifier = modifier.fillMaxSize().background(InkBackground)) {
        // 动态眩光背景：替代原静态大理石贴图
        AuroraBackground(modifier = Modifier.fillMaxSize())

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = LocusSpacing.contentBottomPadding),
        ) {
            // 问候行
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 28.dp)
                    .padding(top = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("晚上好，守住今夜", style = LocusTypography.bodyMedium, color = Stone)
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(InkSurface2)
                        .border(1.dp, Line, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("K", style = LocusTypography.labelLarge, color = Amber)
                }
            }

            // Streak Hero：大数字 + 脉动环 + 旋转弧光 + 背后光晕
            StreakHero(
                days = uiState.currentStreakDays,
                isPersonalBest = uiState.isPersonalBest,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 40.dp),
            )

            // 冲动冲浪卡片
            UrgeSurfCard(
                modifier = Modifier.padding(horizontal = 24.dp),
                waveData = uiState.urgeWaveData,
                hintText = "今晚 20:30 出现一次中度冲动，持续约 12 分钟，已通过冲浪练习平复。",
            )

            // 快捷操作
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(top = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                QuickActionCard(
                    title = "打卡守护",
                    subtitle = "记录今夜状态",
                    icon = { Icon(Icons.Filled.Shield, null, tint = Amber, modifier = Modifier.size(18.dp)) },
                    modifier = Modifier.weight(1f),
                ) { /* Stage 后续接打卡流程 */ }
                QuickActionCard(
                    title = "冲浪练习",
                    subtitle = "10分钟冥想",
                    icon = { Icon(Icons.Filled.Waves, null, tint = Amber, modifier = Modifier.size(18.dp)) },
                    modifier = Modifier.weight(1f),
                ) { /* Stage 后续接练习页 */ }
            }
        }

        // SOS 按钮：悬浮于底部导航上方一点，不压内容（内容区底部已留 110dp）
        SosButton(
            onClick = { /* Stage 后续接急救弹层 */ },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp),
        )
    }
}

/** Streak Hero：中心大数字 + 三层脉动环 + 双旋转弧光 + 呼吸光晕 */
@Composable
private fun StreakHero(
    days: Int,
    isPersonalBest: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        // 数字背后的呼吸光晕
        val haloPhase = rememberAuroraPhase(6000, label = "heroHalo")
        Canvas(modifier = Modifier.size(240.dp)) {
            val breathe = (kotlin.math.sin(haloPhase) + 1f) / 2f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Amber.copy(alpha = 0.14f + 0.10f * breathe),
                        Amber.copy(alpha = 0f),
                    ),
                    center = center,
                    radius = size.minDimension / 2,
                ),
                radius = size.minDimension / 2,
            )
        }

        // 三层脉动环（scale 真正生效，交错延迟形成涟漪扩散）
        PulseRing(diameterDp = 200, delayMillis = 0)
        PulseRing(diameterDp = 236, delayMillis = 700)
        PulseRing(diameterDp = 272, delayMillis = 1400)

        // 双旋转弧光：两条琥珀圆弧沿相反方向绕数字缓慢旋转
        OrbitArcs(diameterDp = 292)

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = "$days",
                    style = LocusTypography.displayLarge,
                    color = Smoke,
                )
                Text(
                    text = "天",
                    style = LocusTypography.titleLarge,
                    color = Stone,
                    modifier = Modifier.padding(bottom = 18.dp, start = 4.dp),
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = if (isPersonalBest) "连续守护 · 个人最佳" else "连续守护",
                style = LocusTypography.titleMedium,
                color = Stone,
            )
        }
    }
}

/** 脉动环：scale 1.0→1.08 涟漪扩散，alpha 渐隐 */
@Composable
private fun PulseRing(diameterDp: Int, delayMillis: Int) {
    val transition = rememberInfiniteTransition(label = "pulse")
    val scale by transition.animateFloat(
        initialValue = 0.92f, targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            tween(2600, delayMillis, EaseInOutSine), RepeatMode.Reverse
        ), label = "pulseScale",
    )
    val alpha by transition.animateFloat(
        initialValue = 0.55f, targetValue = 0.12f,
        animationSpec = infiniteRepeatable(
            tween(2600, delayMillis, EaseInOutSine), RepeatMode.Reverse
        ), label = "pulseAlpha",
    )
    Box(
        modifier = Modifier
            .size(diameterDp.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                this.alpha = alpha
            }
            .border(1.dp, Amber, CircleShape),
    )
}

/** 双旋转弧光：一对圆弧沿圆周三轨反向旋转，营造"环绕守护"感 */
@Composable
private fun OrbitArcs(diameterDp: Int) {
    val phase = rememberAuroraPhase(11000, label = "orbitArcs")
    Canvas(modifier = Modifier.size(diameterDp.dp)) {
        val strokeWidth = 1.5.dp.toPx()
        val inset = strokeWidth
        val arcSize = androidx.compose.ui.geometry.Size(
            size.width - inset * 2, size.height - inset * 2,
        )
        val topLeft = Offset(inset, inset)
        val degrees = phase * 360f / (Math.PI.toFloat() * 2f)

        // 主弧：顺时针，琥珀
        rotate(degrees) {
            drawArc(
                color = Amber.copy(alpha = 0.7f),
                startAngle = 0f,
                sweepAngle = 70f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
            )
        }
        // 副弧：逆时针，更淡更长
        rotate(-degrees * 0.6f + 120f) {
            drawArc(
                color = Amber.copy(alpha = 0.3f),
                startAngle = 0f,
                sweepAngle = 120f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
            )
        }
        // 微弧：点缀
        rotate(degrees * 1.4f + 240f) {
            drawArc(
                color = Smoke.copy(alpha = 0.25f),
                startAngle = 0f,
                sweepAngle = 30f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
            )
        }
    }
}

/** 冲动冲浪卡片：标题 + 波浪线绘制动画 + 提示文案 + 卡片流光 */
@Composable
private fun UrgeSurfCard(
    waveData: List<Float>,
    hintText: String,
    modifier: Modifier = Modifier,
) {
    val drawProgress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        drawProgress.animateTo(
            1f,
            tween(LocusMotion.DURATION_WAVE, 500, LocusMotion.EaseOut),
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(LocusRadius.xl))
            .background(InkSurface)
            .border(1.dp, Line, RoundedCornerShape(LocusRadius.xl)),
    ) {
        CardShimmer(modifier = Modifier.matchParentSize())

        Column(modifier = Modifier.padding(24.dp)) {
            Text("URGE SURFING · 冲动冲浪", style = LocusTypography.labelMedium, color = Stone)
            Spacer(Modifier.height(4.dp))
            Text("冲动峰值已过", style = LocusTypography.headlineMedium, color = Smoke)
            Spacer(Modifier.height(16.dp))

            Canvas(modifier = Modifier.fillMaxWidth().height(64.dp)) {
                val w = size.width
                val h = size.height
                if (waveData.size < 2) return@Canvas
                val step = w / (waveData.size - 1)
                val path = Path().apply {
                    waveData.forEachIndexed { i, v ->
                        val x = i * step
                        val y = h - v * h * 0.85f - h * 0.05f
                        if (i == 0) moveTo(x, y)
                        else {
                            val prevX = (i - 1) * step
                            val prevY = h - waveData[i - 1] * h * 0.85f - h * 0.05f
                            val cx = (prevX + x) / 2
                            cubicTo(cx, prevY, cx, y, x, y)
                        }
                    }
                }
                clipRect(right = w * drawProgress.value) {
                    val fillPath = Path().apply {
                        addPath(path)
                        lineTo(w, h); lineTo(0f, h); close()
                    }
                    drawPath(
                        fillPath,
                        Brush.verticalGradient(
                            listOf(Amber.copy(alpha = 0.25f), Amber.copy(alpha = 0f)),
                            startY = 0f, endY = h,
                        ),
                    )
                    drawPath(
                        path,
                        color = Amber,
                        style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round),
                    )
                    // 波峰发光圆点：跟随绘制进度
                    if (drawProgress.value > 0.05f) {
                        val tipIndex = ((waveData.size - 1) * drawProgress.value)
                            .toInt().coerceIn(0, waveData.size - 1)
                        val tipX = tipIndex * step
                        val tipY = h - waveData[tipIndex] * h * 0.85f - h * 0.05f
                        drawCircle(
                            color = Amber.copy(alpha = 0.35f),
                            radius = 10.dp.toPx(),
                            center = Offset(tipX, tipY),
                        )
                        drawCircle(
                            color = Amber,
                            radius = 3.5.dp.toPx(),
                            center = Offset(tipX, tipY),
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            Text(hintText, style = LocusTypography.bodySmall, color = Stone)
        }
    }
}

/** 快捷操作卡片：图标 + 标题 + 副标题，按压回弹 + 流光 */
@Composable
private fun QuickActionCard(
    title: String,
    subtitle: String,
    icon: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .bounceClick(onClick = onClick)
            .clip(RoundedCornerShape(LocusRadius.lg))
            .background(InkSurface)
            .border(1.dp, Line, RoundedCornerShape(LocusRadius.lg)),
    ) {
        CardShimmer(modifier = Modifier.matchParentSize(), intensity = 0.7f)

        Column(modifier = Modifier.padding(18.dp)) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(AmberDim),
                contentAlignment = Alignment.Center,
            ) {
                icon()
            }
            Spacer(Modifier.height(14.dp))
            Text(title, style = LocusTypography.bodyMedium, color = Smoke)
            Spacer(Modifier.height(2.dp))
            Text(subtitle, style = LocusTypography.labelSmall, color = Stone)
        }
    }
}
