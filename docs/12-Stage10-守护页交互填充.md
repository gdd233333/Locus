# Stage 10 — 守护页交互填充（打卡 / SOS 急救 / 冲浪练习）

> 守护页三个入口目前是空壳：`SosButton`、`打卡守护`、`冲浪练习` 的 onClick 全是 TODO。
> 这一阶段把它们接上真实交互，守护页从"能看"变成"能用"。

## 前置条件

- Stage 6 已完成：真实 Repository 可用，且 `StreakRepository` 已补齐写方法
  （`checkIn(mood, note)` / `logUrge(intensity)` / `resolveLatestUrge(durationMinutes, method)`，
  这三个方法在 Stage 6 文档中已要求实现）
- 交互流程的最终解释权在 `docs/02-信息架构.md` §4.1 / §4.2，提示词只摘了要点，
  有歧义时以信息架构文档为准
- 动效组件就绪：`AuroraBackground` / `CardShimmer` / `bounceClick` /
  `rememberAuroraPhase` / `rememberBreathingAlpha`（都在 `designsystem/component/Aurora.kt`）

## 目标

- 打卡弹层 CheckInSheet：心情 1-5 + 备注 → 写入打卡 → 庆祝动画
- SOS 急救弹层 UrgeEmergencySheet：三个出口（记录 / 冲浪练习 / 找点事做）
- 冲浪练习全屏页 SurfingExerciseScreen：10 分钟引导式呼吸冲浪动画
- 灵感页支持"冲动急救"分类预选（从 SOS 跳转过来时自动筛选）

---

## 提示词（直接复制给牛马 Agent）

```
你是 Locus 项目的功能工程师。这是一个 Android 自律应用（Kotlin + Jetpack Compose +
Room，包名 com.locus.app）。守护页（feature/sober/SoberScreen.kt）有三个入口的
onClick 目前是空的，你负责把它们填上真实功能。

## 项目现状（先读再动手）

- 守护页代码：feature/sober/SoberScreen.kt + SoberViewModel.kt
  三个待接线位置：SosButton 的 onClick、QuickActionCard「打卡守护」、
  QuickActionCard「冲浪练习」
- 数据层：core/data/ 的 StreakRepository 已有写方法
  checkIn(mood, note) / logUrge(intensity) / resolveLatestUrge(durationMinutes, method)
- 交互流程契约：docs/02-信息架构.md §4.1（打卡流程）和 §4.2（冲动急救流程），
  必须照此实现
- 视觉与动效规范：颜色字体只用 designsystem/theme/ tokens；弹层用
  ModalBottomSheet（参考 feature/timelog/TimeLogScreen.kt 里 StartLogSheet 的写法）；
  所有可点元素用 bounceClick；卡片内用 CardShimmer；全屏页根 Box 铺 AuroraBackground
- 导航：MainNavigation.kt 的 NavHost，新全屏页在这里注册路由

## 任务 1：CheckInSheet（打卡弹层）

触发：QuickActionCard「打卡守护」
内容：
- 标题「今日打卡」+ 副标题「今天守住了吗？」
- 心情自评：1~5 五档，用五个圆点/表情符号横排单选，选中项琥珀放大弹跳
  （spring 动画），默认不选
- 备注输入框：可选，placeholder「想记点什么？」（BasicTextField，
  样式参考 StartLogSheet）
- 确认按钮：选了心情才可点（禁用态 InkSurface2 底 StoneDark 字）
行为：
- 确认 → StreakRepository.checkIn(mood, note) → 关闭弹层
- 关闭后播放庆祝动画：守护页 streak 数字处爆发一圈琥珀色粒子
  （Canvas 粒子系统：20~30 个小圆点从中心向外飞散渐隐，约 800ms，一次性）
- SoberUiState.todayCheckedIn 变 true，「打卡守护」卡片此后显示为已打卡态
  （标题变「今日已守护」，图标加勾，不再响应点击）

## 任务 2：UrgeEmergencySheet（SOS 急救弹层）

触发：SosButton（冲动急救）
内容（全屏高度的 ModalBottomSheet，skipPartiallyExpanded = true）：
- 标题「冲动来了？」+ 副标题「很正常，它像海浪，会涨也会退」
- 强度选择：轻 / 中 / 强 三枚 pill 单选（对应 UrgeIntensity 枚举）
- 三个出口按钮（纵向排列的大按钮，每个带图标 + 主文案 + 副文案）：
  1. 「我只是记录一下」— 记下来，我自己扛
     → logUrge(选中强度) → 关闭弹层 → 守护页冲动计数和波浪线立即更新
  2. 「我要做冲浪练习」— 10 分钟引导呼吸
     → logUrge(选中强度) → 跳转 SurfingExerciseScreen
  3. 「给我找点事做」— 推荐替代活动
     → logUrge(选中强度) → 跳转灵感页，自动筛选「冲动急救」分类
- 未选强度时三个按钮全部禁用

## 任务 3：SurfingExerciseScreen（冲浪练习全屏页）

路由 "surfing"，从 NavHost 注册，进入时隐藏底部导航（或用独立路由不经过
Scaffold bottomBar 的方式，自行选择并说明）。

界面与动效（这是全 app 最华丽的一屏，放手做）：
- 全屏 InkBackground + AuroraBackground(intensity = 1.2f)
- 中央一个巨大的呼吸圆：随引导节奏缩放（吸气 4s 放大到 1.3x →
  屏息 2s 保持 → 呼气 6s 缩回 1.0x），圆本身是 Amber 径向渐变 + 多层
  脉动光环（复用守护页 PulseRing 的思路，至少三层错相）
- 圆心文字随阶段切换：「吸气」「屏息」「呼气」（Crossfade 切换）
- 顶部：剩余时间倒计时（从 10 分钟倒数，labelLarge）
- 底部：「提前结束」文字按钮（StoneDark 色，不抢眼）
- 背景波浪：屏幕底部 1/4 区域画两层缓慢起伏的波形（sin 波相位错开，
  Canvas 绘制，无限循环），呼应"冲浪"主题

结束逻辑：
- 10 分钟走完或用户点「提前结束」→ 弹出结果确认：
  「冲动平复了吗？」两个选项：
  - 「平复了」→ resolveLatestUrge(实际时长分钟, "冲浪练习") → 返回守护页
  - 「还没有」→ 返回守护页（不标记 resolved），鼓励文案 Toast 或 Snackbar
- 返回用 navController.popBackStack()

## 任务 4：灵感页分类预选

- InspireScreen 所在路由 "inspire" 加可选参数（如 ?category=EMERGENCY）
- 从 SOS 跳转时携带 EMERGENCY；正常点 Tab 进入不带参数，行为不变
- InspireViewModel.init 或路由参数读取处：有预选参数时
  selectedCategory 初始化为对应分类

## 验证

1. ./gradlew assembleDebug 通过
2. 打卡：选心情 → 确认 → 粒子庆祝动画播放 → 卡片变已打卡态 →
   杀进程重开仍是已打卡
3. SOS：不选强度时三按钮禁用；选「记录一下」后守护页今日冲动数 +1、
   波浪线变化
4. SOS → 冲浪练习：呼吸圆节奏正确（4-2-6）、倒计时跳动、底部波浪在动；
   提前结束选「平复了」→ 守护页最近冲动显示已平复
5. SOS → 找点事做：灵感页打开时已筛选「冲动急救」分类
6. 所有新 UI 无硬编码色值，弹层/按钮均有 bounceClick 手感

## 遇到错误怎么办

编译失败把完整错误贴回来。不要改 designsystem/theme 的 token 值，
不要动 core/model 模型。缺 Repository 方法就补上并在回复中说明。
粒子/波浪动画用 Canvas + rememberAuroraPhase 实现，不引入第三方动画库。
```

---

## 验证清单（牛马执行完后你自己过一遍）

- [ ] `./gradlew assembleDebug` 通过
- [ ] 三个入口全部可用，无空 onClick 残留
- [ ] 打卡庆祝粒子动画播放，已打卡态持久化（杀进程验证）
- [ ] SOS 三出口流程各自走通，数据正确写入
- [ ] 冲浪练习页呼吸节奏 4-2-6，波浪动画在动，两种结束路径都正确
- [ ] SOS → 灵感页自动筛选「冲动急救」
- [ ] 视觉符合墨石规范：无硬编码色值、动效华丽但不刺眼
- [ ] `git commit` 完成，message 形如 `feat: Stage 10 守护页交互填充`
