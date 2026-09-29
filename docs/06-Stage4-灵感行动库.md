# Stage 4 — 灵感行动库（灵感 Tab）

## 前置条件

- Stage 1~3 完成：项目能编译运行，底部导航已搭建，灵感 Tab 当前是占位页
- `designsystem/component/FilterChipRow.kt` 存在

## 目标

- 灵感 Tab 完整呈现：标题、筛选 chips、活动卡片列表（右侧大理石纹理装饰）、换一批交互
- 卡片入场有交错（stagger）动画
- 全部用假数据驱动

## 视觉参照

`design/版本A - 墨石（深色极简）.html` 中间的手机屏。

---

## 提示词（直接复制给牛马 Agent）

````
请为 D:\TakeControl\app 这个 Android 项目实现「灵感行动库」屏幕，替换当前的灵感占位页。项目包名 com.locus.app，设计系统已存在（InkBackground、InkSurface、InkSurface2、Amber、AmberDim、Smoke、Stone、StoneDark、Line、LocusTypography、LocusSpacing、LocusRadius、LocusMotion、LocusTheme），组件已存在（FilterChipRow、ChipItem）。res/drawable-nodpi/ 下有 marble_texture_1.png ~ marble_texture_5.png。

本阶段全部使用假数据，不接入数据库。

## 任务 1：创建数据模型

创建 app/src/main/java/com/locus/app/core/model/ActivityModels.kt，完整内容：

    package com.locus.app.core.model

    data class Activity(
        val id: Long = 0,
        val title: String,
        val description: String,
        val durationMinutes: Int,
        val category: ActivityCategory,
        val tags: List<String> = emptyList(),
        val isBuiltIn: Boolean = true,
    )

    enum class ActivityCategory(val displayName: String) {
        EMERGENCY("冲动急救"),
        QUICK("5 分钟"),
        MEDIUM("30 分钟"),
        OUTDOOR("出门走走"),
        CREATIVE("动手创作"),
        INPUT("输入"),
        ENVIRONMENT("环境整理"),
        EXPRESSION("表达"),
    }

## 任务 2：创建假数据 Repository（含 20 条内置活动）

创建 app/src/main/java/com/locus/app/core/data/FakeActivityRepository.kt，完整内容：

    package com.locus.app.core.data

    import com.locus.app.core.model.Activity
    import com.locus.app.core.model.ActivityCategory
    import kotlinx.coroutines.flow.Flow
    import kotlinx.coroutines.flow.flowOf

    class FakeActivityRepository {

        private val builtInActivities = listOf(
            Activity(1, "冷水洗脸 + 深呼吸", "物理打断当前状态，让大脑从冲动中抽离。冷水刺激迷走神经，快速降低唤醒水平。", 5, ActivityCategory.EMERGENCY, listOf("冲动急救")),
            Activity(2, "做 20 个俯卧撑", "立刻，就在原地。心率上来，冲动下去。身体是最诚实的开关。", 5, ActivityCategory.EMERGENCY, listOf("冲动急救")),
            Activity(3, "出门快走一圈", "不带手机，只带钥匙。让身体动起来，让视线离开屏幕。夜风是最好的清醒剂。", 20, ActivityCategory.OUTDOOR, listOf("户外")),
            Activity(4, "泡一杯热茶", "双手捧杯，感受温度从掌心传到手臂。什么都不想，就看着热气往上飘。", 5, ActivityCategory.EMERGENCY, listOf("感官唤醒")),
            Activity(5, "整理书桌一角", "不需要收拾整个房间，就整理你伸手可及的那一块。环境清爽了，心也会跟着清爽一点。", 15, ActivityCategory.ENVIRONMENT, listOf("环境整理")),
            Activity(6, "读 20 页书", "纸质书优先。如果读不进去，就从最薄的那本开始。读不下去也是正常的，翻页即是胜利。", 45, ActivityCategory.INPUT, listOf("输入")),
            Activity(7, "写一页手账", "不用写得多好，就写今天最强烈的一个感受。写下来的东西，就不会再在心里翻腾了。", 30, ActivityCategory.EXPRESSION, listOf("表达")),
            Activity(8, "画一幅烂画", "不需要好看，需要动手。纸和笔就行，画你此刻脑子里最混乱的那个画面。画完撕掉也行。", 25, ActivityCategory.CREATIVE, listOf("创作")),
            Activity(9, "听一首完整的歌", "不是背景音，是认真听。戴上耳机，闭上眼睛，跟着节奏走。一首歌结束，你已经换了一个状态。", 15, ActivityCategory.EMERGENCY, listOf("感官切换")),
            Activity(10, "拉伸 5 分钟", "床上就能做，放松肌肉也放松神经。", 5, ActivityCategory.QUICK, listOf("身体")),
            Activity(11, "洗一个苹果慢慢吃", "专注在味道和口感上。", 10, ActivityCategory.QUICK, listOf("感官唤醒")),
            Activity(12, "给朋友发一条消息", "不用说什么重要的，就打个招呼。", 5, ActivityCategory.EXPRESSION, listOf("表达")),
            Activity(13, "做 10 分钟冥想", "跟着呼吸走，走神了就拉回来。", 10, ActivityCategory.EMERGENCY, listOf("冥想")),
            Activity(14, "打扫房间地面", "扫地或拖地，让地面反光。", 20, ActivityCategory.ENVIRONMENT, listOf("环境整理")),
            Activity(15, "写日记三行", "今天做了什么，感受如何，明天想做什么。", 10, ActivityCategory.EXPRESSION, listOf("表达")),
            Activity(16, "看一篇长文", "收藏夹里吃灰的那篇，现在就看。", 20, ActivityCategory.INPUT, listOf("输入")),
            Activity(17, "学一个魔术", "硬币、纸牌都行，B 站搜教程。", 30, ActivityCategory.CREATIVE, listOf("创作")),
            Activity(18, "下楼买一瓶水", "就走出去，哪怕只是为了买水。", 15, ActivityCategory.OUTDOOR, listOf("户外")),
            Activity(19, "整理手机相册", "删截图，删废片，留下真正想留的。", 20, ActivityCategory.ENVIRONMENT, listOf("环境整理")),
            Activity(20, "做一道简单的菜", "煎蛋、煮面都行，重点是从头到尾做完。", 40, ActivityCategory.CREATIVE, listOf("创作")),
        )

        fun getActivities(category: ActivityCategory?): Flow<List<Activity>> = flowOf(
            if (category == null) builtInActivities
            else builtInActivities.filter { it.category == category }
        )

        /** 随机抽取 count 个活动（符合筛选条件） */
        fun getRandomActivities(count: Int, category: ActivityCategory?): List<Activity> {
            val pool = if (category == null) builtInActivities
            else builtInActivities.filter { it.category == category }
            return pool.shuffled().take(count)
        }
    }

## 任务 3：创建 ViewModel 与 UI 状态

创建 app/src/main/java/com/locus/app/feature/inspire/InspireViewModel.kt，完整内容：

    package com.locus.app.feature.inspire

    import androidx.lifecycle.ViewModel
    import androidx.lifecycle.viewModelScope
    import com.locus.app.core.data.FakeActivityRepository
    import com.locus.app.core.model.Activity
    import com.locus.app.core.model.ActivityCategory
    import kotlinx.coroutines.delay
    import kotlinx.coroutines.flow.MutableStateFlow
    import kotlinx.coroutines.flow.StateFlow
    import kotlinx.coroutines.launch

    data class InspireUiState(
        val selectedCategory: ActivityCategory? = null,
        val activities: List<Activity> = emptyList(),
        val isShuffling: Boolean = false,
        val isLoading: Boolean = true,
    )

    class InspireViewModel(
        private val repository: FakeActivityRepository = FakeActivityRepository(),
    ) : ViewModel() {

        private val _uiState = MutableStateFlow(InspireUiState())
        val uiState: StateFlow<InspireUiState> = _uiState

        init {
            shuffle()
        }

        fun selectCategory(category: ActivityCategory?) {
            _uiState.value = _uiState.value.copy(selectedCategory = category)
            shuffle()
        }

        /** 换一批：先播放退出动画（400ms），再换数据播放入场动画 */
        fun shuffle() {
            viewModelScope.launch {
                _uiState.value = _uiState.value.copy(isShuffling = true)
                delay(400)
                val newActivities = repository.getRandomActivities(
                    count = 3,
                    category = _uiState.value.selectedCategory,
                )
                _uiState.value = _uiState.value.copy(
                    activities = newActivities,
                    isShuffling = false,
                    isLoading = false,
                )
            }
        }
    }

## 任务 4：创建灵感屏幕

创建 app/src/main/java/com/locus/app/feature/inspire/InspireScreen.kt，完整内容：

    package com.locus.app.feature.inspire

    import androidx.compose.animation.core.Animatable
    import androidx.compose.animation.core.tween
    import androidx.compose.foundation.Image
    import androidx.compose.foundation.background
    import androidx.compose.foundation.border
    import androidx.compose.foundation.clickable
    import androidx.compose.foundation.layout.*
    import androidx.compose.foundation.rememberScrollState
    import androidx.compose.foundation.shape.RoundedCornerShape
    import androidx.compose.foundation.verticalScroll
    import androidx.compose.material.icons.Icons
    import androidx.compose.material.icons.filled.AccessTime
    import androidx.compose.material3.Icon
    import androidx.compose.material3.Text
    import androidx.compose.runtime.*
    import androidx.compose.ui.Alignment
    import androidx.compose.ui.Modifier
    import androidx.compose.ui.draw.alpha
    import androidx.compose.ui.draw.clip
    import androidx.compose.ui.graphics.graphicsLayer
    import androidx.compose.ui.layout.ContentScale
    import androidx.compose.ui.res.painterResource
    import androidx.compose.ui.unit.dp
    import androidx.lifecycle.viewmodel.compose.viewModel
    import com.locus.app.R
    import com.locus.app.core.model.Activity
    import com.locus.app.core.model.ActivityCategory
    import com.locus.app.designsystem.component.ChipItem
    import com.locus.app.designsystem.component.FilterChipRow
    import com.locus.app.designsystem.theme.*

    @Composable
    fun InspireScreen(
        viewModel: InspireViewModel = viewModel(),
        modifier: Modifier = Modifier,
    ) {
        val uiState by viewModel.uiState.collectAsState()

        Column(
            modifier = modifier
                .fillMaxSize()
                .background(InkBackground)
                .verticalScroll(rememberScrollState())
                .padding(bottom = LocusSpacing.contentBottomPadding),
        ) {
            // 标题区
            Column(modifier = Modifier.padding(horizontal = 28.dp).padding(top = 24.dp)) {
                Text("现在，做点别的", style = LocusTypography.displaySmall, color = Smoke)
                Spacer(Modifier.height(6.dp))
                Text("根据当下状态，为你推荐三件事", style = LocusTypography.bodySmall, color = Stone)
            }

            Spacer(Modifier.height(20.dp))

            // 筛选 chips：第一个是"此刻推荐"（即不过滤）
            val chips = listOf(ChipItem("all", "此刻推荐")) +
                ActivityCategory.entries.take(5).map { ChipItem(it.name, it.displayName) }
            FilterChipRow(
                chips = chips,
                selectedChipId = uiState.selectedCategory?.name ?: "all",
                onChipSelected = { chip ->
                    val category = if (chip.id == "all") null
                    else ActivityCategory.valueOf(chip.id)
                    viewModel.selectCategory(category)
                },
            )

            Spacer(Modifier.height(20.dp))

            // 活动卡片列表：交错入场动画
            Column(
                modifier = Modifier.padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                uiState.activities.forEachIndexed { index, activity ->
                    ActivityCard(
                        activity = activity,
                        index = index,
                        visible = !uiState.isShuffling,
                        textureRes = when (index % 3) {
                            0 -> R.drawable.marble_texture_2
                            1 -> R.drawable.marble_texture_3
                            else -> R.drawable.marble_texture_4
                        },
                    )
                }
            }

            // 换一批提示
            Text(
                text = "都不想做？点这里换一批 →",
                style = LocusTypography.bodySmall,
                color = StoneDark,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(24.dp)
                    .clickable { viewModel.shuffle() },
            )
        }
    }

    /** 活动卡片：右侧大理石纹理 + 交错 fadeUp 入场 */
    @Composable
    private fun ActivityCard(
        activity: Activity,
        index: Int,
        visible: Boolean,
        textureRes: Int,
    ) {
        val alpha = remember { Animatable(0f) }
        val translationY = remember { Animatable(24f) }

        LaunchedEffect(visible, activity.id) {
            if (visible) {
                // 交错延迟：每张卡比上一张晚 100ms
                kotlinx.coroutines.delay((index * 100).toLong())
                launch { alpha.animateTo(1f, tween(700, easing = LocusMotion.EaseOut)) }
                launch { translationY.animateTo(0f, tween(700, easing = LocusMotion.EaseOut)) }
            } else {
                launch { alpha.animateTo(0f, tween(300)) }
                launch { translationY.animateTo(12f, tween(300)) }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    this.alpha = alpha.value
                    this.translationY = translationY.value.dp.toPx()
                }
                .clip(RoundedCornerShape(LocusRadius.xl))
                .background(InkSurface)
                .border(1.dp, Line, RoundedCornerShape(LocusRadius.xl)),
        ) {
            // 右侧大理石纹理装饰
            Image(
                painter = painterResource(textureRes),
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .width(120.dp)
                    .fillMaxHeight()
                    .alpha(0.5f),
                contentScale = ContentScale.Crop,
            )

            Column(modifier = Modifier.padding(24.dp)) {
                // 时间标签
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.AccessTime,
                        contentDescription = null,
                        tint = Amber,
                        modifier = Modifier.size(12.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "${activity.durationMinutes} MIN",
                        style = LocusTypography.labelSmall,
                        color = Amber,
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(activity.title, style = LocusTypography.headlineSmall, color = Smoke)
                Spacer(Modifier.height(6.dp))
                Text(
                    text = activity.description,
                    style = LocusTypography.bodySmall,
                    color = Stone,
                    modifier = Modifier.fillMaxWidth(0.75f),
                )
                Spacer(Modifier.height(14.dp))
                // 标签
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    activity.tags.take(2).forEach { tag ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(InkSurface2)
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                        ) {
                            Text(tag, style = LocusTypography.labelTiny, color = Stone)
                        }
                    }
                }
            }
        }
    }

注意：
- launch 需要在 LaunchedEffect 作用域内调用，如果报错 "Unresolved reference: launch"，在 import 中加入 kotlinx.coroutines.launch 并确认 LaunchedEffect 块内的调用方式正确（coroutineScope { launch {...} } 或直接 launch，因为 LaunchedEffect 的 block 是 suspend CoroutineScope.() -> Unit）
- "换一批"在本阶段用文字点击实现，摇一摇传感器在后续阶段再加

## 任务 5：接入导航

打开 MainNavigation.kt，做两处修改：

1. 在 import 中加入：import com.locus.app.feature.inspire.InspireScreen
2. 把 composable("inspire") { PlaceholderScreen("灵感") } 改为 composable("inspire") { InspireScreen() }

## 任务 6：编译运行并验证

1. ./gradlew assembleDebug 必须编译通过
2. 运行到模拟器，切到灵感 Tab
3. 预期画面：
   - 标题 "现在，做点别的" + 副标题
   - 横向可滚动的筛选 chips（此刻推荐为激活态，琥珀色背景）
   - 三张活动卡片依次从下往上淡入（有先后节奏），右侧有大理石纹理
   - 底部 "都不想做？点这里换一批 →"
4. 点击其他筛选 chip，卡片应先淡出再换一批淡入
5. 点击 "换一批"，卡片内容应更换
6. 全部确认后执行：
       cd D:\TakeControl
       git add -A
       git commit -m "feat: 灵感行动库屏幕 — 筛选 + 随机推荐 + 交错入场动画"

## 遇到错误怎么办

- 编译失败：把完整报错信息贴出来
- 动画不播放：确认 LocusMotion.EaseOut 存在于 designsystem/theme/Motion.kt
- 图片不显示：确认 res/drawable-nodpi/ 下 marble_texture_2.png ~ marble_texture_4.png 存在
````

---

## 验证清单

- [ ] `./gradlew assembleDebug` 编译通过
- [ ] 灵感 Tab 画面与设计稿第二屏一致
- [ ] 三张卡片有交错入场动画
- [ ] 筛选 chips 可切换，切换时卡片有过渡动画
- [ ] "换一批"点击后卡片内容更换
- [ ] git 有新 commit
