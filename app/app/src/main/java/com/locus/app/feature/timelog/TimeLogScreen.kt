package com.locus.app.feature.timelog

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.locus.app.core.model.FrequentActivity
import com.locus.app.core.model.TimeLog
import com.locus.app.designsystem.component.AuroraBackground
import com.locus.app.designsystem.component.CardShimmer
import com.locus.app.designsystem.component.LocalReduceMotion
import com.locus.app.designsystem.component.LocusRhythm
import com.locus.app.designsystem.component.WeekDaySelector
import com.locus.app.designsystem.component.bounceClick
import com.locus.app.designsystem.component.rememberAuroraPhase
import com.locus.app.designsystem.component.rememberBreathingAlpha
import com.locus.app.designsystem.theme.*
import com.locus.app.notification.rememberNotificationPermissionRequester
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val timeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

/** 累计秒数 → "MM:SS" 或 "H:MM:SS" */
private fun formatElapsed(totalSeconds: Long): String {
    val h = totalSeconds / 3600
    val m = (totalSeconds % 3600) / 60
    val s = totalSeconds % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}

/** 时长秒数 → "N 分钟" / "N 时 M 分" */
private fun formatDuration(totalSeconds: Long): String {
    val totalMinutes = (totalSeconds + 30) / 60   // 四舍五入到分钟
    val h = totalMinutes / 60
    val m = totalMinutes % 60
    return when {
        h > 0 && m > 0 -> "${h} 时 ${m} 分"
        h > 0 -> "${h} 小时"
        m > 0 -> "${m} 分钟"
        else -> "不足 1 分钟"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeLogScreen(
    viewModel: TimeLogViewModel = viewModel(),
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsState()
    val isToday = uiState.selectedDate == LocalDate.now()
    // 通知权限在用户第一次点「开始记录」时再申请
    val requestNotificationPermission = rememberNotificationPermissionRequester()

    Box(modifier = modifier.fillMaxSize().background(InkBackground)) {
        AuroraBackground(modifier = Modifier.fillMaxSize(), intensity = 0.7f)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = LocusSpacing.contentBottomPadding),
        ) {
            // 头部：标题 + 日期 + 汇总 chip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 28.dp)
                    .padding(top = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text("时间记录", style = LocusTypography.displaySmall, color = Smoke)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "${uiState.selectedDate.monthValue}.${uiState.selectedDate.dayOfMonth} " +
                            dayOfWeekLabel(uiState.selectedDate),
                        style = LocusTypography.bodySmall,
                        color = Stone,
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(LocusRadius.full))
                        .background(AmberDim)
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                ) {
                    Text(
                        text = "已记录 ${uiState.logsForDate.size} 段",
                        style = LocusTypography.labelSmall,
                        color = Amber,
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // 周日历药丸
            WeekDaySelector(
                dates = uiState.weekDates,
                selectedDate = uiState.selectedDate,
                onDateSelected = viewModel::selectDate,
            )

            Spacer(Modifier.height(20.dp))

            // 正在记录 / 空闲卡（仅今天显示操作卡，历史日期只读）
            if (isToday) {
                val active = uiState.activeLog
                if (active != null) {
                    RecordingCard(
                        log = active,
                        elapsedSeconds = uiState.elapsedSeconds,
                        onEndClick = viewModel::endLog,
                        modifier = Modifier.padding(horizontal = 24.dp),
                    )
                } else {
                    IdleCard(
                        onStartClick = {
                            requestNotificationPermission()
                            viewModel.showStartSheet()
                        },
                        modifier = Modifier.padding(horizontal = 24.dp),
                    )
                }
            }

            // 当日记录时间线
            if (uiState.logsForDate.isNotEmpty()) {
                Column(modifier = Modifier.padding(horizontal = 24.dp).padding(top = 24.dp)) {
                    Text(
                        text = if (isToday) "今日已记录" else "当日已记录",
                        style = LocusTypography.labelTiny,
                        color = StoneDark,
                    )
                    Spacer(Modifier.height(16.dp))
                    uiState.logsForDate.forEachIndexed { index, log ->
                        TimeLogEntry(log = log, index = index)
                    }
                }
            } else if (!isToday) {
                Text(
                    text = "这一天还没有记录",
                    style = LocusTypography.bodySmall,
                    color = StoneDark,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 40.dp),
                )
            }
        }

        // 开始记录底部弹层
        if (uiState.showStartSheet) {
            ModalBottomSheet(
                onDismissRequest = viewModel::hideStartSheet,
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                containerColor = InkSurface,
            ) {
                StartLogSheet(
                    suggestions = uiState.suggestedActivities,
                    onConfirm = viewModel::startLog,
                )
            }
        }
    }
}

private fun dayOfWeekLabel(date: LocalDate): String =
    "周" + listOf("一", "二", "三", "四", "五", "六", "日")[date.dayOfWeek.value - 1]

/** 正在记录大卡片：流动虚线边框 + 呼吸圆点 + 发光大计时器 + 卡片流光 */
@Composable
private fun RecordingCard(
    log: TimeLog,
    elapsedSeconds: Long,
    onEndClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // 边框虚线流动：相位持续推进，虚线沿边框爬行（8s 潮汐节拍）
    val borderPhase = rememberAuroraPhase(LocusRhythm.GLOW, label = "recordingBorder") * 30f
    // 边框整体呼吸（4s）
    val borderAlpha = rememberBreathingAlpha(0.25f, 0.55f, LocusRhythm.BREATH, label = "borderBreathe")
    // 圆点脉动：缩放 1f ~ 1.8f（减弱动态时静止）
    val reduceMotion = LocalReduceMotion.current
    val transition = rememberInfiniteTransition(label = "recordingDot")
    val dotScale by transition.animateFloat(
        initialValue = 1f,
        targetValue = if (reduceMotion) 1f else 1.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(LocusRhythm.BREATH / 2, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "dotScale",
    )
    // 计时器光晕呼吸（4s）
    val glowAlpha = rememberBreathingAlpha(0.25f, 0.55f, LocusRhythm.BREATH, label = "timerGlow")
    // 入场
    var appeared by remember { mutableStateOf(false) }
    val enterAlpha by animateFloatAsState(if (appeared) 1f else 0f, tween(700, easing = LocusMotion.EaseOut), label = "enterAlpha")
    val enterOffset by animateFloatAsState(if (appeared) 0f else 24f, tween(700, easing = LocusMotion.EaseOut), label = "enterOffset")
    LaunchedEffect(Unit) { appeared = true }

    val startTimeText = log.startTime.atZone(ZoneId.systemDefault()).format(timeFormatter)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .alpha(enterAlpha)
            .offset(y = enterOffset.dp)
            .clip(RoundedCornerShape(LocusRadius.xxl))
            .background(InkSurface)
            .drawBehind {
                // 底层：常亮细边框
                drawRoundRect(
                    color = Amber.copy(alpha = 0.12f),
                    cornerRadius = CornerRadius(28.dp.toPx()),
                    style = Stroke(width = 1.dp.toPx()),
                )
                // 上层：流动虚线高光（相位推进 → 沿边框爬行）
                drawRoundRect(
                    color = Amber.copy(alpha = borderAlpha),
                    cornerRadius = CornerRadius(28.dp.toPx()),
                    style = Stroke(
                        width = 1.5.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(
                            floatArrayOf(28f, 42f),
                            borderPhase,
                        ),
                    ),
                )
            },
    ) {
        // 动态眩光：替代原右上大理石贴图
        CardShimmer(modifier = Modifier.matchParentSize())

        Column(modifier = Modifier.padding(28.dp)) {
            // 状态行：呼吸圆点 + 正在记录
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .offset(x = ((dotScale - 1f) * -4).dp, y = ((dotScale - 1f) * -4).dp)
                        .clip(CircleShape)
                        .background(Amber.copy(alpha = 2f - dotScale)),
                )
                Spacer(Modifier.width(8.dp))
                Text("正在记录", style = LocusTypography.labelSmall, color = Amber)
            }
            Spacer(Modifier.height(16.dp))
            Text(log.activityName, style = LocusTypography.displaySmall, color = Smoke)
            Spacer(Modifier.height(8.dp))
            Text("$startTimeText 开始", style = LocusTypography.labelLarge, color = Stone)
            Spacer(Modifier.height(20.dp))
            Text(
                text = formatElapsed(elapsedSeconds),
                style = LocusTypography.displayLarge.copy(
                    fontSize = 64.sp,
                    lineHeight = 64.sp,
                    shadow = androidx.compose.ui.graphics.Shadow(
                        color = Amber.copy(alpha = glowAlpha),
                        blurRadius = 32f,
                    ),
                ),
                color = Smoke,
            )
            Spacer(Modifier.height(24.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .bounceClick(onClick = onEndClick)
                    .clip(RoundedCornerShape(LocusRadius.md))
                    .background(Amber)
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text("结束记录", style = LocusTypography.bodyMedium, color = InkBackground)
            }
        }
    }
}

/** 空闲状态卡：虚线边框 + 呼吸流光 + 开始按钮 */
@Composable
private fun IdleCard(
    onStartClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // 虚线缓慢爬行，呼应 RecordingCard 的流动边框（16s 潮汐节拍）
    val dashPhase = rememberAuroraPhase(LocusRhythm.SHIMMER, label = "idleDash") * 22f

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(LocusRadius.xxl))
            .background(InkSurface)
            .drawBehind {
                drawRoundRect(
                    color = StoneDark,
                    cornerRadius = CornerRadius(28.dp.toPx()),
                    style = Stroke(
                        width = 1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(
                            floatArrayOf(12f, 10f),
                            dashPhase,
                        ),
                    ),
                )
            },
    ) {
        CardShimmer(modifier = Modifier.matchParentSize(), intensity = 0.5f)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("此刻没有在记录任何事", style = LocusTypography.bodyMedium, color = Stone)
            Spacer(Modifier.height(20.dp))
            Box(
                modifier = Modifier
                    .bounceClick(onClick = onStartClick)
                    .clip(RoundedCornerShape(LocusRadius.full))
                    .border(1.dp, AmberDim, RoundedCornerShape(LocusRadius.full))
                    .padding(horizontal = 32.dp, vertical = 14.dp),
            ) {
                Text("开始记录", style = LocusTypography.bodyMedium, color = Amber)
            }
        }
    }
}

/** 时间线条目：左侧时间列 + 竖线圆点 + 右侧内容卡 */
@Composable
private fun TimeLogEntry(log: TimeLog, index: Int) {
    var appeared by remember { mutableStateOf(false) }
    val alpha by animateFloatAsState(if (appeared) 1f else 0f, tween(700, easing = LocusMotion.EaseOut), label = "alpha")
    val offsetY by animateFloatAsState(if (appeared) 0f else 16f, tween(700, easing = LocusMotion.EaseOut), label = "offsetY")
    LaunchedEffect(log.id) {
        delay((index * 80).toLong())
        appeared = true
    }

    val startText = log.startTime.atZone(ZoneId.systemDefault()).format(timeFormatter)
    val endText = log.endTime?.atZone(ZoneId.systemDefault())?.format(timeFormatter) ?: "进行中"
    val active = log.isActive

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(alpha)
            .offset(y = offsetY.dp)
            .height(IntrinsicSize.Min),
    ) {
        // 时间列：72dp 右对齐
        Column(
            modifier = Modifier.width(72.dp).padding(top = 14.dp),
            horizontalAlignment = Alignment.End,
        ) {
            Text(startText, style = LocusTypography.labelMedium, color = Stone)
            Spacer(Modifier.height(2.dp))
            Text(endText, style = LocusTypography.labelTiny, color = StoneDark)
        }
        Spacer(Modifier.width(16.dp))
        // 竖线 + 圆点
        Box(
            modifier = Modifier
                .width(1.dp)
                .fillMaxHeight()
                .background(Line),
        ) {
            Box(
                modifier = Modifier
                    .padding(top = 18.dp)
                    .offset(x = (-3).dp)
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(InkSurface2)
                    .border(1.5.dp, if (active) Amber else StoneDark, CircleShape),
            )
        }
        Spacer(Modifier.width(16.dp))
        // 内容卡
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = 10.dp)
                .clip(RoundedCornerShape(LocusRadius.md))
                .background(InkSurface)
                .border(
                    1.dp,
                    if (active) Amber.copy(alpha = 0.2f) else Line,
                    RoundedCornerShape(LocusRadius.md),
                ),
        ) {
            if (active) {
                CardShimmer(
                    modifier = Modifier.matchParentSize(),
                    intensity = 0.8f,
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = log.activityName,
                    style = LocusTypography.bodyMedium,
                    color = if (active) Amber else Smoke,
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (active) AmberDim else InkSurface2)
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                ) {
                    Text(
                        text = formatDuration(log.durationSeconds()),
                        style = LocusTypography.labelMedium,
                        color = if (active) Amber else Stone,
                    )
                }
            }
        }
    }
}

/** 开始记录弹层：输入框 + 常用建议 chips + 确认按钮 */
@Composable
private fun StartLogSheet(
    suggestions: List<FrequentActivity>,
    onConfirm: (String) -> Unit,
) {
    var name by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 28.dp)
            .padding(bottom = 40.dp),
    ) {
        Text("开始记录", style = LocusTypography.headlineSmall, color = Smoke)
        Spacer(Modifier.height(6.dp))
        Text("接下来这段时间，你打算做什么？", style = LocusTypography.bodySmall, color = Stone)
        Spacer(Modifier.height(20.dp))

        // 输入框
        BasicTextField(
            value = name,
            onValueChange = { name = it },
            textStyle = LocusTypography.bodyLarge.copy(color = Smoke),
            cursorBrush = SolidColor(Amber),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(LocusRadius.md))
                .background(InkSurface2)
                .border(1.dp, Line, RoundedCornerShape(LocusRadius.md))
                .padding(horizontal = 18.dp, vertical = 14.dp),
            decorationBox = { inner ->
                if (name.isEmpty()) {
                    Text("例如：刷手机、学习、吃饭…", style = LocusTypography.bodyLarge, color = StoneDark)
                }
                inner()
            },
        )

        Spacer(Modifier.height(16.dp))

        // 常用建议
        Text("常用", style = LocusTypography.labelTiny, color = StoneDark)
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            suggestions.take(3).forEach { suggestion ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(LocusRadius.full))
                        .background(InkSurface2)
                        .border(1.dp, Line, RoundedCornerShape(LocusRadius.full))
                        .bounceClick { name = suggestion.name }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                ) {
                    Text(suggestion.name, style = LocusTypography.bodySmall, color = Stone)
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            suggestions.drop(3).take(3).forEach { suggestion ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(LocusRadius.full))
                        .background(InkSurface2)
                        .border(1.dp, Line, RoundedCornerShape(LocusRadius.full))
                        .bounceClick { name = suggestion.name }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                ) {
                    Text(suggestion.name, style = LocusTypography.bodySmall, color = Stone)
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // 确认按钮：输入为空时禁用
        val enabled = name.isNotBlank()
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(LocusRadius.md))
                .background(if (enabled) Amber else InkSurface2)
                .bounceClick(scaleDown = 0.97f) { if (enabled) onConfirm(name) }
                .padding(vertical = 16.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "开始",
                style = LocusTypography.bodyMedium,
                color = if (enabled) InkBackground else StoneDark,
            )
        }
    }
}
