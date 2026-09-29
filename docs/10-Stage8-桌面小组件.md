# Stage 8 — 桌面小组件（Glance）

> 两个桌面小组件：Streak 守护组件 + 时间记录组件。让用户不用打开 app 就能看到 streak、一键打卡、看到正在计时。

## 前置条件

- Stage 6 已完成：Room 数据层 + AppContainer 手动 DI 可用
- Stage 7 建议已完成（非硬性，小组件不依赖复盘页）
- 小组件读数据必须走 AppContainer 里的 Repository，不许另起炉灶连数据库

## 目标

- `StreakWidget`：显示当前 streak 天数 + 今日打卡状态，未打卡时可一键打卡
- `RecordingWidget`：显示进行中的记录名 + 已计时，可一键结束；无记录时显示快捷入口
- 点击小组件主体跳转 app 对应页面（deep link）
- 数据变化时小组件自动刷新

---

## 提示词（直接复制给牛马 Agent）

```
你是 Locus 项目的功能工程师。这是一个 Android 自律应用（Kotlin + Jetpack Compose +
Room，包名 com.locus.app，minSdk 26）。请用 Jetpack Glance 实现两个桌面小组件。

## 项目现状（先读再动手）

- 数据层入口：AppContainer（LocusApplication 持有），从中拿 StreakRepository /
  TimeLogRepository，小组件必须通过它们读写数据
- 配色（Glance 里用 ColorProvider，数值）：背景 #12100F，卡片 #1C1A18，
  主色琥珀 #D9A566，主文字 #E8E3DA，次文字 #8A857C
- 字体：Glance 不支持自定义字体，用系统默认即可，靠字号和颜色拉开层次
- app 主入口：MainActivity；路由 "sober" / "timelog"（NavHost 在 MainNavigation.kt，
  deep link 需要你在 manifest 和 NavHost 里配好）

## 任务 1：StreakWidget（4x2 尺寸为主）

显示内容：
- 大号数字：当前 streak 天数（48sp 以上，琥珀色）
- 「天」后缀 + 状态行：今日已打卡显示「✓ 今日已守护」（琥珀），
  未打卡显示「还没打卡」（次文字色）
- 右侧/下方按钮：未打卡时显示「打卡」按钮（琥珀圆角底 + 深色字），
  点击 → 直接写入一条今日打卡（走 StreakRepository.checkIn）→ 刷新小组件；
  已打卡后按钮隐藏或变为禁用态
- 点击小组件空白处：deep link 打开 app 的守护页

## 任务 2：RecordingWidget（4x1 尺寸为主）

显示内容：
- 有进行中记录：活动名 + 已计时时长（HH:MM:SS 格式）+ 「结束」按钮
  （点击走 TimeLogRepository.endLog → 刷新）
- 无进行中记录：显示「此刻空闲」+「开始记录」入口（点击 deep link 打开
  app 记录页，由 app 内弹层完成输入，小组件不做输入框）

## 任务 3：刷新机制（关键，最易翻车）

- 数据驱动刷新：Room Flow 无法在 Glance 里直接 collect，用 WorkManager
  周期性任务（15 分钟，系统下限）+ 写操作后主动 GlanceAppWidget.update 双保险
- 计时的秒级刷新：小组件每分钟刷新一次即可（显示到分钟 HH:MM 也行，
  在回复中说明你选了哪种及原因）；不要试图每秒更新，会被系统限流
- 打卡、结束记录等按钮动作执行后必须立即 update 小组件
- 开机自启：BOOT_COMPLETED receiver 恢复刷新任务

## 约束

- 小组件是静态展示，不要试图在 Glance 里做动画（不支持）
- minSdk 26：注意 Glance 版本兼容性，用稳定版
- 不要修改 core/model 的模型定义
- app 内 UI 代码（feature/ 目录）不要动，deep link 配置除外

## 验证

1. ./gradlew assembleDebug 通过
2. 长按桌面 → 添加小组件，两个组件都能添加且布局不塌陷
3. StreakWidget 显示天数正确；未打卡时点「打卡」→ 组件刷新为已打卡，
   打开 app 守护页能看到今日已打卡
4. app 内开始一条记录 → RecordingWidget 一分钟内显示出计时；
   点「结束」→ app 内该记录变为已完成
5. 杀进程后小组件仍能正常刷新和响应按钮

## 遇到错误怎么办

把完整错误堆栈贴回来。Glance 布局不显示时优先检查 ColorProvider 和
尺寸修饰符，不要靠猜。回复中列出你新增的全部文件清单。
```

---

## 验证清单（牛马执行完后你自己过一遍）

- [ ] `./gradlew assembleDebug` 通过
- [ ] 桌面能添加两个小组件，视觉效果符合墨石配色（深底琥珀字）
- [ ] 一键打卡、一键结束记录真实写入数据库，app 内状态同步
- [ ] 点击小组件能 deep link 到对应 Tab
- [ ] 杀进程/重启手机后小组件仍工作
- [ ] `git commit` 完成，message 形如 `feat: Stage 8 桌面小组件 — Glance`
