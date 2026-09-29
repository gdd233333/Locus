# Stage 7 — 复盘 Tab 完整实现

> 把占位的「复盘」Tab 变成真正的数据复盘页：时间去向统计 + 戒断趋势 + 打卡日历。

## 前置条件

- Stage 6 已完成：Room 数据层可用，真实 Repository 在跑，杀进程数据不丢
- `MainNavigation.kt` 中 `"review"` 路由目前指向 `PlaceholderScreen("复盘 · 敬请期待")`
- 设计系统组件已就绪：`AuroraBackground`（动态眩光背景）、`CardShimmer`（卡片流光）、
  `bounceClick`（按压回弹）都在 `designsystem/component/Aurora.kt`
- 视觉规范见 `docs/01-设计系统-墨石.md`，全部配色/字体/间距只能用 `designsystem/theme/` 的 tokens

## 目标

- 新屏幕 `feature/review/ReviewScreen.kt` + `ReviewViewModel.kt`，替换占位页
- 三个统计区块：本周时间分布、戒断趋势、打卡日历
- 周/月切换
- 数据聚合查询下沉到 Repository（SQL 层做，不要在 UI 层手算）

---

## 提示词（直接复制给牛马 Agent）

```
你是 Locus 项目的功能工程师。这是一个 Android 自律应用（Kotlin + Jetpack Compose +
Room，包名 com.locus.app）。第四个 Tab「复盘」目前是占位页，你负责把它完整实现。

## 项目现状（先读再动手）

- 设计 tokens：designsystem/theme/（Color.kt / Type.kt / Dimens.kt / Motion.kt），
  只用这些值，禁止出现硬编码色值
- 动效组件：designsystem/component/Aurora.kt 里有 AuroraBackground（放屏幕根 Box
  最底层）、CardShimmer（卡片内流光，matchParentSize 使用）、bounceClick（替代
  clickable，所有可点元素都要用）
- 现有屏幕参考写法：feature/timelog/TimeLogScreen.kt（卡片、时间线、入场动画的范式）
- 数据层：core/data/ 下的 Room 实现（Stage 6 产物），模型在 core/model/
- 导航入口：MainNavigation.kt 的 "review" 路由，把 PlaceholderScreen 换成你的 ReviewScreen

## 屏幕结构（自上而下）

1. 标题区（左对齐，padding 与守护页一致）：
   - 标题「复盘」（displaySmall）+ 副标题「数据不说谎」（bodySmall, Stone）
   - 右侧：周 / 月 切换 segmented 控件（两枚 pill，选中态 Amber 底 InkBackground 字）

2. 本周时间分布卡（InkSurface 圆角卡 + CardShimmer）：
   - 标题「时间去向」（labelMedium）
   - 横向柱状图：7 根柱（周一~周日），高度 = 当天记录总时长，Amber 纯色，
     选中柱高亮 + 顶部显示时长文案；柱子入场时从 0 长到目标高度（stagger 60ms）
   - 柱下标注星期（一/二/.../日，labelTiny, StoneDark）
   - 数据为空的日期柱子显示 2dp 高的底槽（InkSurface2），不能消失

3. 戒断趋势卡：
   - 标题「冲动曲线」（labelMedium）
   - 折线波浪图（复用守护页 UrgeSurfCard 的贝塞尔曲线画法）：近 7 日 / 近 30 日
     冲动次数，Amber 描边 + 渐变填充，入场时描边从左到右绘制（clipRect 进度动画）
   - 下方一行统计文字：「本周 N 次冲动，M 次成功平复」（bodySmall, Stone）

4. 打卡日历卡：
   - 标题「守护日历」（labelMedium）
   - 本月日历网格（7 列），打卡日显示 Amber 实心圆点，今天加描边圈，
     未来日期灰色禁用态；月视图下左右滑动切月（简单起见可用 < > 按钮切月）

5. 底部留白 LocusSpacing.contentBottomPadding

## UiState 契约（先定数据类再写 UI，与现有三屏同范式）

    data class ReviewUiState(
        val period: Period,                    // WEEK / MONTH
        val dailyDurations: List<Pair<LocalDate, Long>>,  // 每天总秒数
        val urgeCounts: List<Pair<LocalDate, Int>>,       // 每天冲动次数
        val resolvedCount: Int,
        val checkInDates: Set<LocalDate>,
        val displayedMonth: YearMonth,
        val isLoading: Boolean = true,
    )
    enum class Period { WEEK, MONTH }

## Repository 需要补的聚合查询（加在 Stage 6 的实现里，SQL 聚合）

- TimeLogRepository：按日期范围查询 time_logs，按 date 分组 SUM(end-start)
- StreakRepository：按日期范围统计 urge_events 每日 COUNT 与 resolved COUNT；
  查询 daily_check_ins 在指定月份的所有 date

## 动效要求（华丽但克制，与全 app 一致）

- 屏幕根 Box：InkBackground + AuroraBackground(intensity = 0.5f)
- 三张卡片交错入场：fadeUp + 轻微 scale(0.97→1)，stagger 100ms
- 柱状图、折线图的入场动画如上所述
- 周/月切换时数据区交叉淡入淡出（Crossfade 或 AnimatedContent，300ms）
- 所有可点元素用 bounceClick

## 验证

1. ./gradlew assembleDebug 通过
2. 复盘 Tab 不再是占位页，三个区块齐全
3. 有数据的日子柱子/圆点正确；无数据的日子有底槽、不塌陷
4. 周/月切换动画流畅，数据随之变化
5. 杀进程重开数据仍在（走的是 Stage 6 的真实数据层）

## 遇到错误怎么办

编译失败把完整错误贴回来。不要改 designsystem/theme 的任何 token 值，
不要动其他三个 Tab 的代码。发现 Stage 6 的 Repository 缺少你需要的查询时，
直接在该 Repository 里补方法，并在回复中说明新增的方法签名。
```

---

## 验证清单（牛马执行完后你自己过一遍）

- [ ] `./gradlew assembleDebug` 通过
- [ ] 复盘 Tab 三个区块（时间分布 / 冲动曲线 / 打卡日历）完整显示
- [ ] 周/月切换正常工作，空数据日期不塌陷
- [ ] 全部颜色来自 design tokens，无硬编码色值
- [ ] 数据来自 Room 真实数据层（改一条记录，复盘页跟着变）
- [ ] `git commit` 完成，message 形如 `feat: Stage 7 复盘 Tab 完整实现`
