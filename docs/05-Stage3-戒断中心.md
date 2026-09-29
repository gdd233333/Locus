# Stage 3 — 戒断中心（守护 Tab）

## 前置条件

- Stage 1 完成：项目能编译运行
- Stage 2 完成：`designsystem/theme/` 下有 Color.kt、Type.kt、Dimens.kt、Motion.kt、Theme.kt；`designsystem/component/` 下有 LocusBottomNav.kt、SosButton.kt；`res/drawable-nodpi/` 下有 5 张 `marble_texture_*.png`

## 目标

- 底部四 Tab 导航搭建完成（守护 / 灵感 / 记录 / 复盘，后三个为占位页）
- 守护 Tab 完整呈现：大理石氛围背景、streak 大数字 + 脉动环、冲动冲浪波浪卡、快捷操作、SOS 呼吸按钮
- 全部用假数据驱动，不碰 Room

## 视觉参照

打开 `design/版本A - 墨石（深色极简）.html`，左侧第一个手机屏即本阶段要实现的画面。

---

## 提示词（直接复制给牛马 Agent）

````
请为 D:\TakeControl\app 这个 Android 项目实现「戒断中心」屏幕和全局导航。这是一个 Jetpack Compose 项目，包名 com.locus.app，设计系统主题已存在于 com.locus.app.designsystem.theme（提供 InkBackground、InkSurface、InkSurface2、Amber、AmberDim、Smoke、Stone、StoneDark、Line、LocusTypography、LocusSpacing、LocusRadius、LocusMotion、LocusTheme），组件已存在于 com.locus.app.designsystem.component（提供 LocusBottomNav、NavItem、SosButton）。res/drawable-nodpi/ 下有 marble_texture_1.png ~ marble_texture_5.png。

本阶段全部使用假数据，不接入数据库。所有新代码都要能编译通过。

## 任务 1：添加依赖

打开 app/build.gradle.kts，在 dependencies 块中添加（compose BOM 已存在，不要重复添加）：

    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.compose.material:material-icons-extended")

然后 Gradle sync。

## 任务 2：创建数据模型

创建 app/src/main/java/com/locus/app/core/model/SoberModels.kt，完整内容：

    package com.locus.app.core.model

    import java.time.Instant
    import java.time.LocalDate

    data class StreakRecord(
        val id: Long = 0,
        val startDate: LocalDate,
        val endDate: LocalDate? = null,
        val isActive: Boolean = true,
    )

    enum class UrgeIntensity { MILD, MODERATE, STRONG }

    data class UrgeEvent(
        val id: Long = 0,
        val timestamp: Instant,
        val intensity: UrgeIntensity,
        val durationMinutes: Int? = null,
        val triggerNote: String? = null,
        val resolved: Boolean = false,
        val resolutionMethod: String? = null,
    )

    data class DailyCheckIn(
        val id: Long = 0,
        val date: LocalDate,
        val mood: Int,
        val note: String? = null,
        val createdAt: Instant,
    )

## 任务 3：创建假数据 Repository

创建 app/src/main/java/com/locus/app/core/data/FakeStreakRepository.kt，完整内容：

    package com.locus.app.core.data

    import com.locus.app.core.model.DailyCheckIn
    import com.locus.app.core.model.UrgeEvent
    import com.locus.app.core.model.UrgeIntensity
    import kotlinx.coroutines.flow.Flow
    import kotlinx.coroutines.flow.flowOf
    import java.time.Instant
    import java.time.LocalDate
    import java.time.temporal.ChronoUnit

    /**
     * 假数据仓库：Stage 3 阶段驱动 UI，Stage 6 之后替换为 Room 实现。
     * 接口签名即业务契约，替换实现时签名不得改变。
     */
    class FakeStreakRepository {

        fun getCurrentStreakDays(): Flow<Int> = flowOf(23)

        fun isPersonalBest(): Flow<Boolean> = flowOf(true)

        fun getTodayUrgeCount(): Flow<Int> = flowOf(1)

        fun getLatestUrge(): Flow<UrgeEvent?> = flowOf(
            UrgeEvent(
                id = 1,
                timestamp = Instant.now().minus(80, ChronoUnit.MINUTES),
                intensity = UrgeIntensity.MODERATE,
                durationMinutes = 12,
                resolved = true,
                resolutionMethod = "冲浪练习",
            )
        )

        /** 近 7 日冲动强度，0.0~1.0 归一化，用于波浪线 */
        fun getRecentUrgeIntensities(): Flow<List<Float>> = flowOf(
            listOf(0.2f, 0.5f, 0.8f, 0.4f, 0.35f, 0.6f, 0.25f)
        )

        fun getTodayCheckIn(): Flow<DailyCheckIn?> = flowOf(null)
    }

## 任务 4：创建 ViewModel 与 UI 状态

创建 app/src/main/java/com/locus/app/feature/sober/SoberViewModel.kt，完整内容：

    package com.locus.app.feature.sober

    import androidx.lifecycle.ViewModel
    import androidx.lifecycle.viewModelScope
    import com.locus.app.core.data.FakeStreakRepository
    import com.locus.app.core.model.UrgeEvent
    import kotlinx.coroutines.flow.MutableStateFlow
    import kotlinx.coroutines.flow.StateFlow
    import kotlinx.coroutines.flow.combine
    import kotlinx.coroutines.launch

    data class SoberUiState(
        val currentStreakDays: Int = 0,
        val isPersonalBest: Boolean = false,
        val todayUrgeCount: Int = 0,
        val latestUrge: UrgeEvent? = null,
        val urgeWaveData: List<Float> = emptyList(),
        val todayCheckedIn: Boolean = false,
        val isLoading: Boolean = true,
    )

    class SoberViewModel(
        private val repository: FakeStreakRepository = FakeStreakRepository(),
    ) : ViewModel() {

        private val _uiState = MutableStateFlow(SoberUiState())
        val uiState: StateFlow<SoberUiState> = _uiState

        init {
            viewModelScope.launch {
                combine(
                    repository.getCurrentStreakDays(),
                    repository.isPersonalBest(),
                    repository.getTodayUrgeCount(),
                    repository.getLatestUrge(),
                    repository.getRecentUrgeIntensities(),
                    repository.getTodayCheckIn(),
                ) { values ->
                    SoberUiState(
                        currentStreakDays = values[0] as Int,
                        isPersonalBest = values[1] as Boolean,
                        todayUrgeCount = values[2] as Int,
                        latestUrge = values[3] as UrgeEvent?,
                        urgeWaveData = values[4] as List<Float>,
                        todayCheckedIn = values[5] != null,
                        isLoading = false,
                    )
                }.collect { _uiState.value = it }
            }
        }
    }

注意：combine 对 6 个 Flow 需要使用数组形式的重载，上面的写法中 values 是 Array<Any?>，需要按此处理。如果编译器报错，把 combine 改为嵌套 combine 或使用 combine(a,b,c) 再 combine(d,e,f) 后合并，保持最终输出 SoberUiState 不变。

## 任务 5：创建守护屏幕

创建 app/src/main/java/com/locus/app/feature/sober/SoberScreen.kt。这是本阶段的核心，严格按以下结构与样式实现（视觉参照：design/版本A - 墨石（深色极简）.html 的第一个手机屏）：

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

注意：
- 点击事件先留空实现（`{}`），本阶段只做静态呈现
- Icons.Filled.Layers 如果编译报错说明该图标不在基础集中，全部改用 Icons.Filled.AccessTime 或 Icons.Filled.Star 即可
- 不要给快捷卡片实现悬浮/按下动画，Stage 后续统一加

## 任务 6：搭建全局导航

创建 app/src/main/java/com/locus/app/MainNavigation.kt，完整内容：

    package com.locus.app

    import androidx.compose.foundation.background
    import androidx.compose.foundation.layout.Box
    import androidx.compose.foundation.layout.fillMaxSize
    import androidx.compose.foundation.layout.padding
    import androidx.compose.material.icons.Icons
    import androidx.compose.material.icons.filled.AccessTime
    import androidx.compose.material.icons.filled.BarChart
    import androidx.compose.material.icons.filled.Shield
    import androidx.compose.material.icons.filled.Star
    import androidx.compose.material3.Scaffold
    import androidx.compose.material3.Text
    import androidx.compose.runtime.Composable
    import androidx.compose.ui.Alignment
    import androidx.compose.ui.Modifier
    import androidx.navigation.compose.NavHost
    import androidx.navigation.compose.composable
    import androidx.navigation.compose.currentBackStackEntryAsState
    import androidx.navigation.compose.rememberNavController
    import com.locus.app.designsystem.component.LocusBottomNav
    import com.locus.app.designsystem.component.NavItem
    import com.locus.app.designsystem.theme.InkBackground
    import com.locus.app.designsystem.theme.LocusTheme
    import com.locus.app.designsystem.theme.LocusTypography
    import com.locus.app.designsystem.theme.Stone
    import com.locus.app.feature.sober.SoberScreen

    @Composable
    fun MainNavigation() {
        val navController = rememberNavController()
        val navItems = listOf(
            NavItem("sober", "守护", Icons.Filled.Shield),
            NavItem("inspire", "灵感", Icons.Filled.Star),
            NavItem("timelog", "记录", Icons.Filled.AccessTime),
            NavItem("review", "复盘", Icons.Filled.BarChart),
        )

        LocusTheme {
            Scaffold(
                containerColor = InkBackground,
                bottomBar = {
                    val currentRoute = navController.currentBackStackEntryAsState()
                        .value?.destination?.route ?: "sober"
                    LocusBottomNav(
                        items = navItems,
                        selectedRoute = currentRoute,
                        onItemSelected = { item ->
                            navController.navigate(item.route) {
                                popUpTo("sober") { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                    )
                },
            ) { innerPadding ->
                NavHost(
                    navController = navController,
                    startDestination = "sober",
                    modifier = Modifier.padding(innerPadding),
                ) {
                    composable("sober") { SoberScreen() }
                    composable("inspire") { PlaceholderScreen("灵感") }
                    composable("timelog") { PlaceholderScreen("记录") }
                    composable("review") { PlaceholderScreen("复盘 · 敬请期待") }
                }
            }
        }
    }

    @Composable
    private fun PlaceholderScreen(name: String) {
        Box(
            modifier = Modifier.fillMaxSize().background(InkBackground),
            contentAlignment = Alignment.Center,
        ) {
            Text(name, style = LocusTypography.displaySmall, color = Stone)
        }
    }

如果 Icons.Filled.Shield / BarChart 编译报错（基础图标集没有这两个），有两个解决方案任选：
1. 确认已添加 material-icons-extended 依赖（任务 1）
2. 换成基础集里有的图标：守护→Icons.Filled.Favorite，复盘→Icons.Filled.List

## 任务 7：接入 MainActivity

把 MainActivity.kt 的 setContent 块改为调用 MainNavigation()：

    package com.locus.app

    import android.os.Bundle
    import androidx.activity.ComponentActivity
    import androidx.activity.compose.setContent

    class MainActivity : ComponentActivity() {
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            setContent {
                MainNavigation()
            }
        }
    }

## 任务 8：编译运行并验证

1. 运行 ./gradlew assembleDebug，必须编译通过
2. 安装到模拟器运行
3. 预期画面（守护 Tab）：
   - 深黑暖墨背景，顶部有极淡的大理石纹理
   - 中央超大数字 "23" + "天" + "连续守护 · 个人最佳"，外有两圈缓慢脉动的圆环
   - "冲动冲浪"卡片，波浪线从左到右逐渐绘制出来
   - 两张快捷操作卡片并排
   - 琥珀色 "冲动急救" 胶囊按钮悬浮在底部导航上方，带呼吸光晕
   - 底部四个 Tab：守护（激活，琥珀色）/ 灵感 / 记录 / 复盘
4. 点击其他三个 Tab，应显示对应的占位文字页
5. 全部确认后执行：
       cd D:\TakeControl
       git add -A
       git commit -m "feat: 戒断中心屏幕 + 全局底部导航（假数据驱动）"

## 遇到错误怎么办

- 编译失败：把完整报错信息贴出来，不要自行猜测修改设计系统文件的值
- 图标找不到：按任务 6 中的备选方案换图标
- 预览/运行崩溃：检查 Logcat，把崩溃堆栈贴出来
````

---

## 验证清单

- [ ] `./gradlew assembleDebug` 编译通过
- [ ] 守护 Tab 画面与设计稿第一屏一致（大理石背景 / 脉动环 / 波浪线 / SOS 按钮）
- [ ] 波浪线有从左到右的绘制动画
- [ ] 底部四 Tab 可切换，激活态为琥珀色 + 圆点指示
- [ ] git 有新 commit
