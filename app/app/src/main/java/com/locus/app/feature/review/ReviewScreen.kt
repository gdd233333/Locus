package com.locus.app.feature.review

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.locus.app.designsystem.component.AuroraBackground
import com.locus.app.designsystem.component.CardShimmer
import com.locus.app.designsystem.component.bounceClick
import com.locus.app.designsystem.theme.*
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.YearMonth

private val weekdayLabels = listOf("一", "二", "三", "四", "五", "六", "日")

/** 秒数 → "N 时 M 分" / "N 分钟" */
private fun formatDuration(seconds: Long): String {
    val minutes = (seconds + 30) / 60
    val h = minutes / 60
    val m = minutes % 60
    return when {
        h > 0 && m > 0 -> "${h} 时 ${m} 分"
        h > 0 -> "${h} 小时"
        m > 0 -> "${m} 分钟"
        seconds > 0 -> "不足 1 分钟"
        else -> "无记录"
    }
}

@Composable
fun ReviewScreen(
    viewModel: ReviewViewModel = viewModel(),
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsState()
    val today = remember { LocalDate.now() }
    var selectedDay by remember { mutableStateOf<LocalDate?>(null) }

    Box(modifier = modifier.fillMaxSize().background(InkBackground)) {
        AuroraBackground(modifier = Modifier.matchParentSize(), intensity = 0.6f)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(bottom = LocusSpacing.contentBottomPadding),
        ) {
            ReviewHeader(period = uiState.period, onPeriodChange = viewModel::setPeriod)

            // 周/月切换：数据区交叉淡入淡出
            Crossfade(
                targetState = uiState.period,
                animationSpec = tween(300),
                label = "periodData",
            ) { period ->
                Column(
                    modifier = Modifier.padding(top = LocusSpacing.xl),
                    verticalArrangement = Arrangement.spacedBy(LocusSpacing.lg),
                ) {
                    StaggeredCard(index = 0) {
                        TimeDistributionCard(
                            durations = uiState.dailyDurations,
                            selectedDay = selectedDay,
                            onSelectDay = { selectedDay = it },
                        )
                    }
                    StaggeredCard(index = 1) {
                        UrgeTrendCard(
                            period = period,
                            urgeCounts = uiState.urgeCounts,
                            resolvedCount = uiState.resolvedCount,
                        )
                    }
                    StaggeredCard(index = 2) {
                        CheckInCalendarCard(
                            month = uiState.displayedMonth,
                            checkInDates = uiState.checkInDates,
                            showMonthSwitch = period == Period.MONTH,
                            onShiftMonth = viewModel::shiftMonth,
                            today = today,
                        )
                    }
                }
            }
        }
    }
}

// ---------------- 标题区 ----------------

@Composable
private fun ReviewHeader(period: Period, onPeriodChange: (Period) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 28.dp)
            .padding(top = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text("复盘", style = LocusTypography.displaySmall, color = Smoke)
            Spacer(Modifier.height(4.dp))
            Text("数据不说谎", style = LocusTypography.bodySmall, color = Stone)
        }
        PeriodSwitch(period = period, onChange = onPeriodChange)
    }
}

@Composable
private fun PeriodSwitch(period: Period, onChange: (Period) -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(LocusRadius.full))
            .background(InkSurface2)
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Period.entries.forEach { entry ->
            val selected = entry == period
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(LocusRadius.full))
                    .background(if (selected) Amber else Color.Transparent)
                    .bounceClick(scaleDown = 0.92f) { onChange(entry) }
                    .padding(horizontal = 16.dp, vertical = 6.dp),
            ) {
                Text(
                    text = if (entry == Period.WEEK) "周" else "月",
                    style = LocusTypography.labelMedium,
                    color = if (selected) InkBackground else Stone,
                )
            }
        }
    }
}

// ---------------- 通用卡片容器 ----------------

/** 卡片入场：fadeUp + scale 0.97 → 1，按 index 交错 100ms */
@Composable
private fun StaggeredCard(index: Int, content: @Composable () -> Unit) {
    var appeared by remember { mutableStateOf(false) }
    val alpha by animateFloatAsState(
        targetValue = if (appeared) 1f else 0f,
        animationSpec = tween(700, easing = LocusMotion.EaseOut),
        label = "cardAlpha$index",
    )
    val scale by animateFloatAsState(
        targetValue = if (appeared) 1f else 0.97f,
        animationSpec = tween(700, easing = LocusMotion.EaseOut),
        label = "cardScale$index",
    )
    val offsetY by animateFloatAsState(
        targetValue = if (appeared) 0f else 24f,
        animationSpec = tween(700, easing = LocusMotion.EaseOut),
        label = "cardOffset$index",
    )
    LaunchedEffect(Unit) {
        delay(index * 100L)
        appeared = true
    }

    Box(
        modifier = Modifier
            .padding(horizontal = 24.dp)
            .graphicsLayer {
                this.alpha = alpha
                scaleX = scale
                scaleY = scale
                translationY = offsetY.dp.toPx()
            },
    ) {
        content()
    }
}

/** InkSurface 圆角卡 + 流光，ColumnScope 内容 */
@Composable
private fun ReviewCard(content: @Composable ColumnScope.() -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(LocusRadius.xl))
            .background(InkSurface)
            .border(1.dp, Line, RoundedCornerShape(LocusRadius.xl)),
    ) {
        CardShimmer(modifier = Modifier.matchParentSize())
        Column(modifier = Modifier.padding(24.dp), content = content)
    }
}

// ---------------- 卡 1：时间去向柱状图 ----------------

@Composable
private fun TimeDistributionCard(
    durations: List<Pair<LocalDate, Long>>,
    selectedDay: LocalDate?,
    onSelectDay: (LocalDate) -> Unit,
) {
    val today = remember { LocalDate.now() }
    val effectiveSelection = selectedDay ?: durations.firstOrNull { it.first == today }?.first
    val selected = durations.firstOrNull { it.first == effectiveSelection }

    ReviewCard {
        Text("时间去向", style = LocusTypography.labelMedium, color = Stone)
        Spacer(Modifier.height(10.dp))

        // 选中柱的时长文案（固定高度，避免切换时跳动）
        Box(modifier = Modifier.height(20.dp), contentAlignment = Alignment.CenterStart) {
            if (selected != null) {
                Text(
                    text = "${selected.first.monthValue}.${selected.first.dayOfMonth} · ${formatDuration(selected.second)}",
                    style = LocusTypography.labelSmall,
                    color = Amber,
                )
            }
        }

        Spacer(Modifier.height(10.dp))
        WeeklyBars(
            durations = durations,
            selectedDay = effectiveSelection,
            onSelectDay = onSelectDay,
        )
    }
}

@Composable
private fun WeeklyBars(
    durations: List<Pair<LocalDate, Long>>,
    selectedDay: LocalDate?,
    onSelectDay: (LocalDate) -> Unit,
) {
    val maxSeconds = durations.maxOfOrNull { it.second } ?: 0L

    Row(
        modifier = Modifier.fillMaxWidth().height(128.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        durations.forEachIndexed { index, (date, seconds) ->
            val isSelected = date == selectedDay
            val targetFraction = if (maxSeconds <= 0L || seconds <= 0L) {
                0f
            } else {
                (seconds.toFloat() / maxSeconds).coerceAtLeast(0.06f)
            }

            var appeared by remember { mutableStateOf(false) }
            val fraction by animateFloatAsState(
                targetValue = if (appeared) targetFraction else 0f,
                animationSpec = tween(700, delayMillis = index * 60, easing = LocusMotion.EaseOut),
                label = "bar$index",
            )
            LaunchedEffect(Unit) { appeared = true }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .bounceClick(scaleDown = 0.94f) { onSelectDay(date) },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    if (seconds <= 0L) {
                        // 空数据：2dp 底槽，柱子不消失
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.55f)
                                .height(2.dp)
                                .clip(RoundedCornerShape(1.dp))
                                .background(InkSurface2),
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.55f)
                                .fillMaxHeight(fraction)
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (isSelected) Amber else Amber.copy(alpha = 0.55f)),
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = weekdayLabels[index % 7],
                    style = LocusTypography.labelTiny,
                    color = if (isSelected) Amber else StoneDark,
                )
            }
        }
    }
}

// ---------------- 卡 2：冲动曲线 ----------------

@Composable
private fun UrgeTrendCard(
    period: Period,
    urgeCounts: List<Pair<LocalDate, Int>>,
    resolvedCount: Int,
) {
    val counts = urgeCounts.map { it.second }
    val maxCount = counts.maxOrNull() ?: 0
    val normalized = if (maxCount <= 0) {
        List(counts.size) { 0.05f }
    } else {
        counts.map { (it.toFloat() / maxCount).coerceAtLeast(0.05f) }
    }
    val totalCount = counts.sum()

    // 入场：描边从左到右绘制
    val drawProgress = remember { Animatable(0f) }
    LaunchedEffect(period, urgeCounts.size) {
        drawProgress.snapTo(0f)
        drawProgress.animateTo(
            1f,
            tween(LocusMotion.DURATION_WAVE, 300, LocusMotion.EaseOut),
        )
    }

    ReviewCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("冲动曲线", style = LocusTypography.labelMedium, color = Stone)
            Text(
                text = if (period == Period.WEEK) "近 7 日" else "近 30 日",
                style = LocusTypography.labelSmall,
                color = StoneDark,
            )
        }
        Spacer(Modifier.height(16.dp))

        Canvas(modifier = Modifier.fillMaxWidth().height(72.dp)) {
            val w = size.width
            val h = size.height
            if (normalized.size < 2) return@Canvas
            val step = w / (normalized.size - 1)
            val path = Path().apply {
                normalized.forEachIndexed { i, v ->
                    val x = i * step
                    val y = h - v * h * 0.85f - h * 0.05f
                    if (i == 0) moveTo(x, y)
                    else {
                        val prevX = (i - 1) * step
                        val prevY = h - normalized[i - 1] * h * 0.85f - h * 0.05f
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
                if (drawProgress.value > 0.05f) {
                    val tipIndex = ((normalized.size - 1) * drawProgress.value)
                        .toInt().coerceIn(0, normalized.size - 1)
                    val tipX = tipIndex * step
                    val tipY = h - normalized[tipIndex] * h * 0.85f - h * 0.05f
                    drawCircle(
                        color = Amber.copy(alpha = 0.35f),
                        radius = 9.dp.toPx(),
                        center = Offset(tipX, tipY),
                    )
                    drawCircle(color = Amber, radius = 3.dp.toPx(), center = Offset(tipX, tipY))
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        Text(
            text = (if (period == Period.WEEK) "本周 " else "本月 ") +
                "$totalCount 次冲动，$resolvedCount 次成功平复",
            style = LocusTypography.bodySmall,
            color = Stone,
        )
    }
}

// ---------------- 卡 3：守护日历 ----------------

@Composable
private fun CheckInCalendarCard(
    month: YearMonth,
    checkInDates: Set<LocalDate>,
    showMonthSwitch: Boolean,
    onShiftMonth: (Long) -> Unit,
    today: LocalDate,
) {
    ReviewCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("守护日历", style = LocusTypography.labelMedium, color = Stone)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (showMonthSwitch) {
                    MonthArrow("‹") { onShiftMonth(-1) }
                }
                Text(
                    text = "${month.year} 年 ${month.monthValue} 月",
                    style = LocusTypography.labelMedium,
                    color = Smoke,
                )
                if (showMonthSwitch) {
                    MonthArrow("›") { onShiftMonth(1) }
                }
            }
        }

        Spacer(Modifier.height(18.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            weekdayLabels.forEach { label ->
                Text(
                    text = label,
                    style = LocusTypography.labelTiny,
                    color = StoneDark,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        val leadingBlanks = month.atDay(1).dayOfWeek.value - 1
        val daysInMonth = month.lengthOfMonth()
        val rowCount = (leadingBlanks + daysInMonth + 6) / 7

        repeat(rowCount) { row ->
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                repeat(7) { col ->
                    val dayNumber = row * 7 + col - leadingBlanks + 1
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        if (dayNumber in 1..daysInMonth) {
                            val date = month.atDay(dayNumber)
                            DayCell(
                                date = date,
                                checkedIn = date in checkInDates,
                                isToday = date == today,
                                isFuture = date.isAfter(today),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(date: LocalDate, checkedIn: Boolean, isToday: Boolean, isFuture: Boolean) {
    Box(modifier = Modifier.size(34.dp), contentAlignment = Alignment.Center) {
        if (checkedIn) {
            // 打卡日：琥珀实心圆
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(Amber),
            )
        }
        if (isToday) {
            // 今天：描边圈
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .border(1.5.dp, Amber, CircleShape),
            )
        }
        Text(
            text = date.dayOfMonth.toString(),
            style = LocusTypography.labelSmall,
            color = when {
                isFuture -> StoneDark
                checkedIn -> InkBackground
                else -> Stone
            },
        )
    }
}

@Composable
private fun MonthArrow(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(24.dp)
            .clip(CircleShape)
            .background(InkSurface2)
            .bounceClick(scaleDown = 0.9f, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = label, style = LocusTypography.labelMedium, color = Stone)
    }
}
