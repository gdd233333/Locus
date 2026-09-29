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
import androidx.compose.material.icons.filled.Check
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
import com.locus.app.core.model.UrgeEvent
import com.locus.app.core.model.UrgeIntensity
import com.locus.app.designsystem.component.AuroraBackground
import com.locus.app.designsystem.component.CardShimmer
import com.locus.app.designsystem.component.LocalReduceMotion
import com.locus.app.designsystem.component.LocusRhythm
import com.locus.app.designsystem.component.SosButton
import com.locus.app.designsystem.component.bounceClick
import com.locus.app.designsystem.component.rememberAuroraPhase
import com.locus.app.designsystem.theme.*
import com.locus.app.notification.rememberNotificationPermissionRequester
import kotlin.math.sin
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun SoberScreen(
    viewModel: SoberViewModel = viewModel(),
    onNavigateToSurfing: () -> Unit = {},
    onNavigateToInspireEmergency: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsState()
    // 通知权限在用户第一次点「打卡守护」时再申请
    val requestNotificationPermission = rememberNotificationPermissionRequester()

    Box(modifier = modifier.fillMaxSize().background(InkBackground)) {
        // 动态眩光背景：替代原静态大理石贴图
        AuroraBackground(modifier = Modifier.fillMaxSize())

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(bottom = LocusSpacing.contentBottomPadding),
        ) {
            // 问候行
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 28.dp)
                    .padding(top = 12.dp),
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
                    Text(uiState.userInitial, style = LocusTypography.labelLarge, color = Amber)
                }
            }

            // Streak Hero：大数字 + 脉动环 + 旋转弧光 + 背后光晕 + 打卡庆祝粒子
            StreakHero(
                days = uiState.currentStreakDays,
                isPersonalBest = uiState.isPersonalBest,
                celebrationTrigger = uiState.celebrationTrigger,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 40.dp),
            )

            // 冲动冲浪卡片
            UrgeSurfCard(
                modifier = Modifier.padding(horizontal = 24.dp),
                waveData = uiState.urgeWaveData,
                title = urgeSurfTitle(uiState.latestUrge),
                hintText = urgeSurfHint(uiState.latestUrge),
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
                    title = if (uiState.todayCheckedIn) "今日已守护" else "打卡守护",
                    subtitle = if (uiState.todayCheckedIn) "已经打过卡了" else "记录今夜状态",
                    icon = {
                        Icon(
                            imageVector = if (uiState.todayCheckedIn) Icons.Filled.Check else Icons.Filled.Shield,
                            contentDescription = null,
                            tint = Amber,
                            modifier = Modifier.size(18.dp),
                        )
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    if (!uiState.todayCheckedIn) {
                        requestNotificationPermission()
                        viewModel.showCheckInSheet()
                    }
                }
                QuickActionCard(
                    title = "冲浪练习",
                    subtitle = "10分钟冥想",
                    icon = { Icon(Icons.Filled.Waves, null, tint = Amber, modifier = Modifier.size(18.dp)) },
                    modifier = Modifier.weight(1f),
                ) { onNavigateToSurfing() }
            }
        }

        // SOS 按钮：悬浮于底部导航上方一点，不压内容（内容区底部已留 110dp）
        SosButton(
            onClick = viewModel::showEmergencySheet,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp),
        )

        // 打卡弹层
        if (uiState.showCheckInSheet) {
            CheckInSheet(
                onDismiss = viewModel::hideCheckInSheet,
                onConfirm = { mood, note -> viewModel.checkIn(mood, note) },
            )
        }

        // SOS 急救弹层：三个出口都会先记录冲动（弹层自身负责先 hide 再关闭）
        if (uiState.showEmergencySheet) {
            UrgeEmergencySheet(
                onDismiss = viewModel::hideEmergencySheet,
                onRecordOnly = { intensity -> viewModel.logUrge(intensity) },
                onStartSurfing = { intensity ->
                    viewModel.logUrge(intensity)
                    onNavigateToSurfing()
                },
                onFindActivity = { intensity ->
                    viewModel.logUrge(intensity)
                    onNavigateToInspireEmergency()
                },
            )
        }
    }
}

/** Streak Hero：中心大数字 + 三层脉动环 + 双旋转弧光 + 呼吸光晕 + 庆祝粒子 */
@Composable
private fun StreakHero(
    days: Int,
    isPersonalBest: Boolean,
    celebrationTrigger: Long,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        // 数字背后的呼吸光晕（8s，潮汐节拍）
        val haloPhase = rememberAuroraPhase(LocusRhythm.GLOW, label = "heroHalo")
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

        // 打卡庆祝：一圈琥珀粒子从中心飞散渐隐（每次打卡触发一次）
        CelebrationBurst(trigger = celebrationTrigger)
    }
}

private class BurstParticle(val angle: Float, val speed: Float, val radius: Float)

/** 庆祝粒子：20~30 个小圆点从中心向外飞散渐隐，约 800ms 一次性 */
@Composable
private fun CelebrationBurst(trigger: Long) {
    if (trigger <= 0L) return
    val progress = remember { Animatable(1f) }
    val particles = remember(trigger) {
        val random = kotlin.random.Random(trigger)
        List(26) {
            BurstParticle(
                angle = random.nextFloat() * (Math.PI * 2).toFloat(),
                speed = 0.55f + random.nextFloat() * 0.65f,
                radius = (3f + random.nextFloat() * 5f) * 3.5f,
            )
        }
    }

    LaunchedEffect(trigger) {
        progress.snapTo(0f)
        progress.animateTo(1f, tween(800, easing = LocusMotion.EaseOut))
    }

    Canvas(modifier = Modifier.size(240.dp)) {
        val p = progress.value
        if (p >= 1f) return@Canvas
        val maxDistance = size.minDimension / 2 * 0.92f
        particles.forEach { particle ->
            val distance = maxDistance * particle.speed * p
            drawCircle(
                color = Amber.copy(alpha = (1f - p) * 0.9f),
                radius = particle.radius * (1f - p * 0.35f),
                center = Offset(
                    x = center.x + kotlin.math.cos(particle.angle) * distance,
                    y = center.y + sin(particle.angle) * distance,
                ),
            )
        }
    }
}

/** 脉动环：scale 涟漪扩散 + alpha 渐隐（4s 潮汐节拍；减弱动态时为静态环） */
@Composable
private fun PulseRing(diameterDp: Int, delayMillis: Int) {
    if (LocalReduceMotion.current) {
        Box(
            modifier = Modifier
                .size(diameterDp.dp)
                .alpha(0.25f)
                .border(1.dp, Amber, CircleShape),
        )
        return
    }
    val transition = rememberInfiniteTransition(label = "pulse")
    val scale by transition.animateFloat(
        initialValue = 0.92f, targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            tween(LocusRhythm.BREATH, delayMillis, EaseInOutSine), RepeatMode.Reverse
        ), label = "pulseScale",
    )
    val alpha by transition.animateFloat(
        initialValue = 0.55f, targetValue = 0.12f,
        animationSpec = infiniteRepeatable(
            tween(LocusRhythm.BREATH, delayMillis, EaseInOutSine), RepeatMode.Reverse
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

/** 双旋转弧光：三条圆弧沿圆周反向旋转（32s 潮汐节拍，减弱动态时静止） */
@Composable
private fun OrbitArcs(diameterDp: Int) {
    val phase = rememberAuroraPhase(LocusRhythm.AMBIENT, label = "orbitArcs")
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
    title: String,
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
            Text(title, style = LocusTypography.headlineMedium, color = Smoke)
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

/** 冲动卡片标题：跟随最近一次冲动的状态（不再写死） */
private fun urgeSurfTitle(urge: UrgeEvent?): String = when {
    urge == null -> "还没有冲动记录"
    urge.resolved -> "冲动峰值已过"
    else -> "冲动还没过去"
}

/** 冲动卡片提示：由最近一次冲动的真实数据（时间 / 强度 / 时长 / 平复方式）拼出 */
private fun urgeSurfHint(urge: UrgeEvent?): String {
    if (urge == null) return "真来了就按下面的急救按钮：记录下来，或者冲浪十分钟。"

    val zone = ZoneId.systemDefault()
    val date = urge.timestamp.atZone(zone).toLocalDate()
    val today = LocalDate.now(zone)
    val day = when (date) {
        today -> "今天"
        today.minusDays(1) -> "昨天"
        else -> "${date.monthValue}.${date.dayOfMonth}"
    }
    val time = urge.timestamp.atZone(zone).format(DateTimeFormatter.ofPattern("HH:mm"))
    val intensity = when (urge.intensity) {
        UrgeIntensity.MILD -> "轻度"
        UrgeIntensity.MODERATE -> "中度"
        UrgeIntensity.STRONG -> "强烈"
    }
    val duration = urge.durationMinutes?.let { "持续约 $it 分钟，" }.orEmpty()
    val ending = if (urge.resolved) {
        "已通过${urge.resolutionMethod ?: "冲浪练习"}平复。"
    } else {
        "还没标记平复。"
    }
    return "最近一次：$day $time ${intensity}冲动，$duration$ending"
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
