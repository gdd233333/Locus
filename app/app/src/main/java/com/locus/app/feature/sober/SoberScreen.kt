package com.locus.app.feature.sober

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.locus.app.R
import com.locus.app.designsystem.component.SosButton
import com.locus.app.designsystem.theme.*

@Composable
fun SoberScreen(
    viewModel: SoberViewModel = viewModel(),
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsState()

    Box(modifier = modifier.fillMaxSize().background(InkBackground)) {
        // 大理石氛围背景：顶部区域，极低透明度
        Image(
            painter = painterResource(R.drawable.marble_texture_1),
            contentDescription = null,
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.6f)
                .align(Alignment.TopCenter)
                .alpha(0.25f),
            contentScale = ContentScale.Crop,
        )

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

            // Streak Hero：大数字 + 双脉动环
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 48.dp),
                contentAlignment = Alignment.Center,
            ) {
                PulseRing(diameterDp = 220, delayMillis = 0)
                PulseRing(diameterDp = 260, delayMillis = 1000)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "${uiState.currentStreakDays}",
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
                        text = if (uiState.isPersonalBest) "连续守护 · 个人最佳" else "连续守护",
                        style = LocusTypography.titleMedium,
                        color = Stone,
                    )
                }
            }

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
                    modifier = Modifier.weight(1f),
                ) { /* Stage 后续接打卡流程 */ }
                QuickActionCard(
                    title = "冲浪练习",
                    subtitle = "10分钟冥想",
                    modifier = Modifier.weight(1f),
                ) { /* Stage 后续接练习页 */ }
            }
        }

        // SOS 按钮：悬浮于内容之上、底部导航之上
        SosButton(
            onClick = { /* Stage 后续接急救弹层 */ },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = LocusSpacing.bottomNavHeight + 12.dp),
        )
    }
}

/** 双脉动环：scale 1.0→1.06，alpha 0.6→0.2，4s 循环 */
@Composable
private fun PulseRing(diameterDp: Int, delayMillis: Int) {
    val transition = rememberInfiniteTransition(label = "pulse")
    val scale by transition.animateFloat(
        initialValue = 1f, targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            tween(2000, delayMillis, EaseInOutSine), RepeatMode.Reverse
        ), label = "pulseScale",
    )
    val alpha by transition.animateFloat(
        initialValue = 0.6f, targetValue = 0.2f,
        animationSpec = infiniteRepeatable(
            tween(2000, delayMillis, EaseInOutSine), RepeatMode.Reverse
        ), label = "pulseAlpha",
    )
    Box(
        modifier = Modifier
            .size(diameterDp.dp)
            .alpha(alpha)
            .border(1.dp, AmberDim, CircleShape)
            .then(Modifier),
    )
}

/** 冲动冲浪卡片：标题 + 波浪线绘制动画 + 提示文案 */
@Composable
private fun UrgeSurfCard(
    waveData: List<Float>,
    hintText: String,
    modifier: Modifier = Modifier,
) {
    // 波浪线绘制进度：0→1，2.5s EaseOut，模拟 dash 描边
    val drawProgress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        drawProgress.animateTo(
            1f,
            tween(LocusMotion.DURATION_WAVE, 500, LocusMotion.EaseOut),
        )
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(LocusRadius.xl))
            .background(InkSurface)
            .border(1.dp, Line, RoundedCornerShape(LocusRadius.xl))
            .padding(24.dp),
    ) {
        Text("URGE SURFING · 冲动冲浪", style = LocusTypography.labelMedium, color = Stone)
        Spacer(Modifier.height(4.dp))
        Text("冲动峰值已过", style = LocusTypography.headlineMedium, color = Smoke)
        Spacer(Modifier.height(16.dp))

        // 波浪线：用 waveData 的 7 个归一化值生成平滑三次贝塞尔曲线
        androidx.compose.foundation.Canvas(
            modifier = Modifier.fillMaxWidth().height(64.dp)
        ) {
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
                // 线下渐变填充
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
            }
        }

        Spacer(Modifier.height(12.dp))
        Text(hintText, style = LocusTypography.bodySmall, color = Stone)
    }
}

/** 快捷操作卡片：图标 + 标题 + 副标题 */
@Composable
private fun QuickActionCard(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(LocusRadius.lg))
            .background(InkSurface)
            .border(1.dp, Line, RoundedCornerShape(LocusRadius.lg))
            .padding(18.dp),
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(AmberDim),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.AccessTime,
                contentDescription = null,
                tint = Amber,
                modifier = Modifier.size(18.dp),
            )
        }
        Spacer(Modifier.height(14.dp))
        Text(title, style = LocusTypography.bodyMedium, color = Smoke)
        Spacer(Modifier.height(2.dp))
        Text(subtitle, style = LocusTypography.labelSmall, color = Stone)
    }
}
