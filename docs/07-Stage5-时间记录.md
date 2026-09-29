# Stage 5 — 时间记录（记录 Tab）

## 前置条件

- Stage 1~4 完成：项目能编译运行，守护 / 灵感 Tab 已实现，记录 Tab 当前是占位页
- `designsystem/component/WeekDaySelector.kt` 存在（签名：`WeekDaySelector(dates: List<LocalDate>, selectedDate: LocalDate, onDateSelected: (LocalDate) -> Unit, modifier: Modifier)`）
- `res/drawable-nodpi/` 下有 `marble_texture_5.png`

## 目标

- 记录 Tab 完整呈现：日期头 + 周日历药丸 + 正在记录大卡片（流动边框 + 实时计时）/ 空闲卡片 + 当日记录时间线列表
- 开始 / 结束记录全流程可用，计时精确到秒、每秒刷新
- 切换日期可查看不同日期的记录
- 全部用假数据驱动（内存仓库，含预置历史记录便于验证 UI）

## 视觉参照

`design/版本A - 墨石（深色极简）.html` 最右侧的手机屏（时间记录屏）。

---

## 提示词（直接复制给牛马 Agent）

````
请为 D:\TakeControl\app 这个 Android 项目实现「时间记录」屏幕，替换当前的记录占位页。这是一个 Jetpack Compose 项目，包名 com.locus.app。设计系统主题已存在于 com.locus.app.designsystem.theme（提供 InkBackground、InkSurface、InkSurface2、Amber、AmberDim、Smoke、Stone、StoneDark、Line、LocusTypography、LocusSpacing、LocusRadius、LocusMotion、LocusTheme），组件已存在于 com.locus.app.designsystem.component（提供 WeekDaySelector）。res/drawable-nodpi/ 下有 marble_texture_5.png。MainNavigation.kt 中记录 Tab 目前是 composable("timelog") { PlaceholderScreen("记录") }。

【模块定位】这是"手动时间记录"模块：用户手动点"开始记录"输入正在做的事，App 实时计时，点"结束记录"后该条目进入当日时间线列表。用于事后复盘时间去向，不是计划表。

本阶段全部使用内存假数据，不接入数据库。

## 任务 1：创建数据模型

创建 app/src/main/java/com/locus/app/core/model/TimeLogModels.kt，完整内容：

    package com.locus.app.core.model

    import java.time.Instant
    import java.time.LocalDate

    data class TimeLog(
        val id: Long = 0,
        val activityName: String,
        val startTime: Instant,
        val endTime: Instant?,        // null = 进行中
        val date: LocalDate,          // 归属日期（跨天归入开始日）
    ) {
        val isActive: Boolean get() = endTime == null

        /** 时长秒数；进行中时以 now 计算 */
        fun durationSeconds(now: Instant = Instant.now()): Long =
            java.time.Duration.between(startTime, endTime ?: now).seconds
    }

    data class FrequentActivity(
        val name: String,
        val useCount: Int,
    )

## 任务 2：创建假数据 Repository（内存可变，含预置历史）

创建 app/src/main/java/com/locus/app/core/data/FakeTimeLogRepository.kt，完整内容：

    package com.locus.app.core.data

    import com.locus.app.core.model.FrequentActivity
    import com.locus.app.core.model.TimeLog
    import kotlinx.coroutines.flow.Flow
    import kotlinx.coroutines.flow.MutableStateFlow
    import kotlinx.coroutines.flow.map
    import java.time.Instant
    import java.time.LocalDate
    import java.time.LocalTime
    import java.time.ZoneId
    import java.time.temporal.ChronoUnit

    /**
     * 内存假仓库：预置今天 3 条已完成 + 1 条进行中（45 分钟前开始的"刷手机"），
     * 以及昨天 3 条，方便验证日期切换。
     * 状态保存在 MutableStateFlow 中，start/end/delete 会真实更新 UI（仅本次运行内有效）。
     */
    class FakeTimeLogRepository {

        private val zone: ZoneId = ZoneId.systemDefault()
        private var nextId = 100L

        /** 今天某时刻的 Instant，minuteOffset 为相对该时刻的分钟偏移 */
        private fun todayAt(hour: Int, minute: Int, minuteOffset: Long = 0): Instant =
            LocalDate.now().atTime(LocalTime.of(hour, minute)).atZone(zone)
                .toInstant().plus(minuteOffset, ChronoUnit.MINUTES)

        private val initialLogs: List<TimeLog> = run {
            val today = LocalDate.now()
            val yesterday = today.minusDays(1)
            listOf(
                // 今天已完成的 3 条（与设计稿一致）
                TimeLog(1, "晚餐", todayAt(19, 0), todayAt(19, 45), today),
                TimeLog(2, "图书馆自习", todayAt(19, 45), todayAt(21, 30), today),
                TimeLog(3, "走路回宿舍", todayAt(21, 30), todayAt(22, 3), today),
                // 今天进行中：45 分钟前开始
                TimeLog(4, "刷手机", Instant.now().minus(45, ChronoUnit.MINUTES), null, today),
                // 昨天的 3 条（用于验证日期切换）
                TimeLog(5, "睡懒觉", yesterday.atTime(9, 30).atZone(zone).toInstant(),
                    yesterday.atTime(11, 0).atZone(zone).toInstant(), yesterday),
                TimeLog(6, "打游戏的下午", yesterday.atTime(14, 0).atZone(zone).toInstant(),
                    yesterday.atTime(17, 20).atZone(zone).toInstant(), yesterday),
                TimeLog(7, "夜跑", yesterday.atTime(20, 0).atZone(zone).toInstant(),
                    yesterday.atTime(20, 40).atZone(zone).toInstant(), yesterday),
            )
        }

        private val logs = MutableStateFlow(initialLogs)

        fun getActiveLog(): Flow<TimeLog?> = logs.map { list -> list.firstOrNull { it.isActive } }

        fun getLogsForDate(date: LocalDate): Flow<List<TimeLog>> =
            logs.map { list -> list.filter { it.date == date }.sortedBy { it.startTime } }

        suspend fun startLog(activityName: String): TimeLog {
            // 防御：若已有进行中的记录，先把它结束掉
            logs.value.firstOrNull { it.isActive }?.let { endLog(it.id) }
            val now = Instant.now()
            val log = TimeLog(
                id = nextId++,
                activityName = activityName.trim().ifBlank { "未命名" },
                startTime = now,
                endTime = null,
                date = LocalDate.now(),
            )
            logs.value = logs.value + log
            return log
        }

        suspend fun endLog(logId: Long) {
            val now = Instant.now()
            logs.value = logs.value.map { if (it.id == logId && it.isActive) it.copy(endTime = now) else it }
        }

        suspend fun deleteLog(logId: Long) {
            logs.value = logs.value.filterNot { it.id == logId }
        }

        fun getFrequentActivities(): List<FrequentActivity> = listOf(
            FrequentActivity("刷手机", 12),
            FrequentActivity("学习", 8),
            FrequentActivity("吃饭", 7),
            FrequentActivity("睡觉", 6),
            FrequentActivity("运动", 4),
            FrequentActivity("发呆", 3),
        )
    }

## 任务 3：创建 ViewModel 与 UI 状态

创建 app/src/main/java/com/locus/app/feature/timelog/TimeLogViewModel.kt，完整内容：

    package com.locus.app.feature.timelog

    import androidx.lifecycle.ViewModel
    import androidx.lifecycle.viewModelScope
    import com.locus.app.core.data.FakeTimeLogRepository
    import com.locus.app.core.model.FrequentActivity
    import com.locus.app.core.model.TimeLog
    import kotlinx.coroutines.Job
    import kotlinx.coroutines.delay
    import kotlinx.coroutines.flow.MutableStateFlow
    import kotlinx.coroutines.flow.StateFlow
    import kotlinx.coroutines.launch
    import java.time.Instant
    import java.time.LocalDate

    data class TimeLogUiState(
        val selectedDate: LocalDate = LocalDate.now(),
        val weekDates: List<LocalDate> = emptyList(),
        val activeLog: TimeLog? = null,
        val elapsedSeconds: Long = 0L,
        val logsForDate: List<TimeLog> = emptyList(),
        val suggestedActivities: List<FrequentActivity> = emptyList(),
        val showStartSheet: Boolean = false,
        val isLoading: Boolean = true,
    )

    class TimeLogViewModel(
        private val repository: FakeTimeLogRepository = FakeTimeLogRepository(),
    ) : ViewModel() {

        private val _uiState = MutableStateFlow(TimeLogUiState())
        val uiState: StateFlow<TimeLogUiState> = _uiState

        private var tickerJob: Job? = null

        init {
            // 以今天为锚点的本周（周一 ~ 周日）
            val today = LocalDate.now()
            val monday = today.minusDays((today.dayOfWeek.value - 1).toLong())
            _uiState.value = _uiState.value.copy(
                weekDates = (0..6).map { monday.plusDays(it.toLong()) },
                suggestedActivities = repository.getFrequentActivities(),
            )

            // 监听进行中的记录：出现时启动秒表，消失时归零
            viewModelScope.launch {
                repository.getActiveLog().collect { active ->
                    _uiState.value = _uiState.value.copy(activeLog = active)
                    if (active != null) startTicker(active) else stopTicker()
                }
            }

            observeDate(LocalDate.now())
        }

        /** 切换查看日期：重新订阅该日期的记录列表 */
        private fun observeDate(date: LocalDate) {
            _uiState.value = _uiState.value.copy(selectedDate = date)
            viewModelScope.launch {
                repository.getLogsForDate(date).collect { logs ->
                    _uiState.value = _uiState.value.copy(logsForDate = logs, isLoading = false)
                }
            }
        }

        /** 每秒刷新累计时长 */
        private fun startTicker(log: TimeLog) {
            tickerJob?.cancel()
            tickerJob = viewModelScope.launch {
                while (true) {
                    _uiState.value = _uiState.value.copy(
                        elapsedSeconds = log.durationSeconds(Instant.now())
                    )
                    delay(1000)
                }
            }
        }

        private fun stopTicker() {
            tickerJob?.cancel()
            tickerJob = null
            _uiState.value = _uiState.value.copy(elapsedSeconds = 0L)
        }

        fun selectDate(date: LocalDate) = observeDate(date)

        fun showStartSheet() { _uiState.value = _uiState.value.copy(showStartSheet = true) }
        fun hideStartSheet() { _uiState.value = _uiState.value.copy(showStartSheet = false) }

        fun startLog(activityName: String) {
            viewModelScope.launch {
                repository.startLog(activityName)
                hideStartSheet()
            }
        }

        fun endLog() {
            val id = _uiState.value.activeLog?.id ?: return
            viewModelScope.launch { repository.endLog(id) }
        }
    }

## 任务 4：创建时间记录屏幕

创建 app/src/main/java/com/locus/app/feature/timelog/TimeLogScreen.kt，完整内容：

    package com.locus.app.feature.timelog

    import androidx.compose.animation.core.*
    import androidx.compose.foundation.Image
    import androidx.compose.foundation.background
    import androidx.compose.foundation.border
    import androidx.compose.foundation.clickable
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
    import androidx.compose.ui.geometry.Offset
    import androidx.compose.ui.graphics.Brush
    import androidx.compose.ui.graphics.Color
    import androidx.compose.ui.graphics.PathEffect
    import androidx.compose.ui.graphics.SolidColor
    import androidx.compose.ui.graphics.drawscope.Stroke
    import androidx.compose.ui.layout.ContentScale
    import androidx.compose.ui.res.painterResource
    import androidx.compose.ui.text.style.TextAlign
    import androidx.compose.ui.unit.dp
    import androidx.compose.ui.unit.sp
    import androidx.lifecycle.viewmodel.compose.viewModel
    import com.locus.app.R
    import com.locus.app.core.model.FrequentActivity
    import com.locus.app.core.model.TimeLog
    import com.locus.app.designsystem.component.WeekDaySelector
    import com.locus.app.designsystem.theme.*
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

        Box(modifier = modifier.fillMaxSize().background(InkBackground)) {
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
                            onStartClick = viewModel::showStartSheet,
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

    /** 正在记录大卡片：流动琥珀边框 + 呼吸圆点 + 大计时器 */
    @Composable
    private fun RecordingCard(
        log: TimeLog,
        elapsedSeconds: Long,
        onEndClick: () -> Unit,
        modifier: Modifier = Modifier,
    ) {
        // 边框流动：透明度在 0.4 ~ 0.15 间呼吸（4s 周期）
        val transition = rememberInfiniteTransition(label = "recordingBorder")
        val borderAlpha by transition.animateFloat(
            initialValue = 0.4f,
            targetValue = 0.15f,
            animationSpec = infiniteRepeatable(
                animation = tween(2000, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "borderAlpha",
        )
        // 圆点脉动：缩放 1f ~ 1.8f（2s 周期）
        val dotScale by transition.animateFloat(
            initialValue = 1f,
            targetValue = 1.8f,
            animationSpec = infiniteRepeatable(
                animation = tween(1000, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "dotScale",
        )
        // 入场
        var appeared by remember { mutableStateOf(false) }
        val enterAlpha by animateFloatAsState(if (appeared) 1f else 0f, tween(700, easing = LocusMotion.EaseOut), label = "enterAlpha")
        val enterOffset by animateFloatAsState(if (appeared) 0f else 24f, tween(700, easing = LocusMotion.EaseOut), label = "enterOffset")
        LaunchedEffect(Unit) { appeared = true }

        val borderColor = Amber.copy(alpha = borderAlpha)
        val startTimeText = log.startTime.atZone(ZoneId.systemDefault()).format(timeFormatter)

        Box(
            modifier = modifier
                .fillMaxWidth()
                .alpha(enterAlpha)
                .offset(y = enterOffset.dp)
                .clip(RoundedCornerShape(LocusRadius.xxl))
                .background(InkSurface)
                .drawBehind {
                    // 对角琥珀渐变的流动边框（1.5dp 描边）
                    drawRoundRect(
                        brush = Brush.linearGradient(
                            colors = listOf(borderColor, Color.Transparent, Color.Transparent, borderColor),
                            start = Offset.Zero,
                            end = Offset(size.width, size.height),
                        ),
                        cornerRadius = CornerRadius(28.dp.toPx()),
                        style = Stroke(width = 1.5.dp.toPx()),
                    )
                },
        ) {
            // 右上大理石纹理
            Image(
                painter = painterResource(R.drawable.marble_texture_5),
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .width(140.dp)
                    .fillMaxHeight()
                    .alpha(0.3f),
                contentScale = ContentScale.Crop,
            )

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
                    style = LocusTypography.displayLarge.copy(fontSize = 64.sp, lineHeight = 64.sp),
                    color = Smoke,
                )
                Spacer(Modifier.height(24.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(LocusRadius.md))
                        .background(Amber)
                        .clickable(onClick = onEndClick)
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("结束记录", style = LocusTypography.bodyMedium, color = InkBackground)
                }
            }
        }
    }

    /** 空闲状态卡：虚线边框 + 开始按钮 */
    @Composable
    private fun IdleCard(
        onStartClick: () -> Unit,
        modifier: Modifier = Modifier,
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(LocusRadius.xxl))
                .background(InkSurface)
                .drawBehind {
                    // 虚线圆角边框
                    drawRoundRect(
                        color = StoneDark,
                        cornerRadius = CornerRadius(28.dp.toPx()),
                        style = Stroke(
                            width = 1.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f)),
                        ),
                    )
                }
                .padding(horizontal = 28.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("此刻没有在记录任何事", style = LocusTypography.bodyMedium, color = Stone)
            Spacer(Modifier.height(20.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(LocusRadius.full))
                    .border(1.dp, AmberDim, RoundedCornerShape(LocusRadius.full))
                    .clickable(onClick = onStartClick)
                    .padding(horizontal = 32.dp, vertical = 14.dp),
            ) {
                Text("开始记录", style = LocusTypography.bodyMedium, color = Amber)
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
            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(bottom = 10.dp)
                    .clip(RoundedCornerShape(LocusRadius.md))
                    .background(InkSurface)
                    .border(
                        1.dp,
                        if (active) Amber.copy(alpha = 0.2f) else Line,
                        RoundedCornerShape(LocusRadius.md),
                    )
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
                            .clickable { name = suggestion.name }
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
                            .clickable { name = suggestion.name }
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
                    .clickable(enabled = enabled) { onConfirm(name) }
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

注意：
- RecordingCard 里呼吸圆点用"放大时自身变透明"模拟向外扩散的脉冲，offset 那两行是为了让放大围绕圆心而不是左上角
- ModalBottomSheet 来自 material3，项目 Stage 3 已有 material3 依赖；如果编译报 ExperimentalMaterial3Api 相关错误，确认 @OptIn 注解只标在 TimeLogScreen 上即可
- 如果 WeekDaySelector 的参数名与上面调用不一致，以 designsystem/component/WeekDaySelector.kt 里的实际签名为准微调调用处

## 任务 5：接入导航

打开 MainNavigation.kt，做两处修改：

1. 在 import 中加入：import com.locus.app.feature.timelog.TimeLogScreen
2. 把 composable("timelog") { PlaceholderScreen("记录") } 改为 composable("timelog") { TimeLogScreen() }

## 任务 6：编译运行并验证

1. ./gradlew assembleDebug 必须编译通过
2. 运行到模拟器，切到记录 Tab
3. 预期画面：
   - 标题 "时间记录" + 今天日期 + 右侧琥珀色 "已记录 4 段" chip
   - 七个圆形日期药丸（今天是激活态，琥珀底深字）
   - 正在记录大卡片：琥珀色流动边框（明暗呼吸）、左上角呼吸圆点 + "正在记录"、活动名 "刷手机"、"HH:mm 开始"、超大等宽字体计时器每秒 +1、底部琥珀色 "结束记录" 按钮，卡片右侧有淡淡大理石纹理
   - 下方 "今日已记录" 列表：4 条条目，每条左侧时间列（开始时间 + 结束时间）、中间竖线圆点、右侧卡片（名称 + 时长 chip）；最后一条进行中的条目为琥珀色高亮
4. 点击 "结束记录"：大卡片消失，出现虚线空闲卡；列表中该条目变成普通样式并显示最终时长
5. 点击空闲卡的 "开始记录"：弹出底部弹层，输入 "学习"（或点常用 chip 填充），点 "开始" → 大卡片重新出现并从 00:00 开始计时
6. 点击昨天日期的药丸：大卡片/空闲卡消失，只显示昨天的 3 条记录列表；点回今天恢复
7. 全部确认后执行：
       cd D:\TakeControl
       git add -A
       git commit -m "feat: 时间记录屏幕 — 手动开始/结束记录 + 实时计时 + 周日历切换"

## 遇到错误怎么办

- 编译失败：把完整报错信息贴出来
- 计时器不走：确认 ViewModel 的 startTicker 里 while(true) 循环体内先更新状态再 delay(1000)，且没有忘记 launch
- 弹层不弹出：确认点击 "开始记录" 后 uiState.showStartSheet 变为 true（可在按钮处加日志）
- 切日期后列表不变：确认 observeDate 里订阅的是新日期的 getLogsForDate
- 纹理不显示：确认 res/drawable-nodpi/marble_texture_5.png 存在
````

---

## 验证清单

- [ ] `./gradlew assembleDebug` 编译通过
- [ ] 记录 Tab 画面与设计稿第三屏一致（流动边框大卡片 + 时间线条目）
- [ ] 计时器每秒刷新，格式 `MM:SS`（超 1 小时变 `H:MM:SS`）
- [ ] 结束记录 → 出现空闲卡，条目落回列表显示最终时长
- [ ] 开始记录 → 底部弹层可输入/选建议，确认后新卡片从 00:00 计时
- [ ] 切换日期药丸可查看昨天记录，今天视图恢复操作卡
- [ ] git 有新 commit
