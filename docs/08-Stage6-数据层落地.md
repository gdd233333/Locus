# Stage 6 — 数据层落地（Room + DataStore）

> 把三个模块的假 Repository 换成真实持久化。这一阶段结束后，app 杀掉重开数据还在，可以真正日常使用。

## 前置条件

- Stage 1~5 已完成，三个 Tab 均用假数据跑通，`./gradlew assembleDebug` 编译通过
- 当前 ViewModel 通过构造参数默认实例持有 Fake Repository（如 `FakeTimeLogRepository = FakeTimeLogRepository()`）
- 数据模型已存在于 `core/model/`，字段以代码为准（与 `docs/02-信息架构.md` §1 一致）
- 内置 20 条活动数据已在 `FakeActivityRepository.kt` 中，迁移时原样搬入数据库

## 目标

- Room 数据库落地：5 张表 + TypeConverter + 预置数据
- 三个真实 Repository 实现，**方法签名与现有 Fake Repository 完全一致**
- ViewModel 切换到真实实现（手动 DI，不引入 Hilt）
- DataStore 承载设置项（为 Stage 9 的通知设置预留）
- app 杀进程重开后：streak 天数、时间记录、进行中计时全部正确恢复

---

## 提示词（直接复制给牛马 Agent）

```
你是 Locus 项目的后端（数据层）工程师。这是一个 Android 自律应用（Kotlin + Jetpack Compose，
包名 com.locus.app，minSdk 26，Gradle KTS）。目前三个功能模块全部用内存假数据驱动，
你的任务是把数据层落地为 Room + DataStore 真实持久化。

## 项目现状（请先读这些文件再动手）

- 数据模型：app/app/src/main/java/com/locus/app/core/model/ 下三个文件
  （SoberModels.kt / ActivityModels.kt / TimeLogModels.kt），字段以此为准，不要改模型
- 假仓库（即接口契约）：app/app/src/main/java/com/locus/app/core/data/
  FakeStreakRepository.kt / FakeActivityRepository.kt / FakeTimeLogRepository.kt
- 消费方（不得改它们的调用方式）：feature/sober/SoberViewModel.kt、
  feature/inspire/InspireViewModel.kt、feature/timelog/TimeLogViewModel.kt

## 硬性约束：Repository 签名一字不动

三个 ViewModel 目前调用的方法如下，你的真实实现必须提供完全相同的方法签名
（建议：先为每个仓库定义 interface，让 Fake 和新实现都实现它，ViewModel 只依赖 interface）：

StreakRepository:
    fun getCurrentStreakDays(): Flow<Int>
    fun isPersonalBest(): Flow<Boolean>
    fun getTodayUrgeCount(): Flow<Int>
    fun getLatestUrge(): Flow<UrgeEvent?>
    fun getRecentUrgeIntensities(): Flow<List<Float>>   // 近7日归一化强度，0.0~1.0
    fun getTodayCheckIn(): Flow<DailyCheckIn?>
    // 以下方法当前 Fake 未实现但流程需要，请补齐（签名自定，风格保持一致）：
    // suspend fun checkIn(mood: Int, note: String?)     —— 打卡
    // suspend fun logUrge(intensity: UrgeIntensity)     —— 记录冲动
    // suspend fun resolveLatestUrge(durationMinutes: Int, method: String) —— 标记平复

ActivityRepository:
    fun getActivities(category: ActivityCategory?): Flow<List<Activity>>
    fun getRandomActivities(count: Int, category: ActivityCategory?): List<Activity>

TimeLogRepository:
    fun getActiveLog(): Flow<TimeLog?>
    fun getLogsForDate(date: LocalDate): Flow<List<TimeLog>>
    suspend fun startLog(activityName: String): TimeLog
    suspend fun endLog(logId: Long)
    suspend fun deleteLog(logId: Long)
    fun getFrequentActivities(): List<FrequentActivity>   // 可改为 Flow，同步改 ViewModel

## 任务 1：Room 数据库

1. 在 gradle 中加入 Room（ksp 插件 + room-runtime + room-ktx），版本用当前稳定版
2. 5 张表（schema 见 docs/02-信息架构.md §6，字段类型自行映射）：
   streak_records / urge_events / daily_check_ins / activities / time_logs
3. TypeConverter：LocalDate、Instant、UrgeIntensity、ActivityCategory、List<String>（tags）
4. frequent_activities 不是独立表：对 time_logs 做 GROUP BY activity_name 聚合查询，
   按 COUNT 降序取前 N 条
5. 预置数据：RoomDatabase.Callback.onCreate 中插入 FakeActivityRepository 里的
   20 条内置活动（isBuiltIn = true，内容原样照搬，一条不许少）
6. 数据库名 locus.db，version = 1

## 任务 2：业务规则（写进 Repository 实现，不要漏）

- streak 天数 = 当前活跃 StreakRecord 的 (今天 - startDate + 1)；无活跃记录则为 0
- 打卡：同一天重复打卡只更新原记录；打卡时若无活跃 streak 则新建（startDate = 今天）
- isPersonalBest：当前 streak 天数 >= 历史所有 streak 的最长天数
- getRecentUrgeIntensities：近 7 天每天取当日最强冲动的强度
  （MILD=0.33, MODERATE=0.66, STRONG=1.0），当天无冲动则为 0.05（波浪线不断底）
- startLog 防御：已有进行中记录时先把它结束掉（FakeTimeLogRepository 里已有此逻辑，保留）
- TimeLog 跨天：归属日期 = startTime 所在日期（模型注释已写明）

## 任务 3：依赖注入（手动，不用 Hilt）

创建 Application 子类 LocusApplication，持有 AppContainer（单例懒加载数据库和各
Repository）。三个 ViewModel 改为接收 interface 类型参数；用 viewModelFactory
或 ViewModelProvider.Factory 从 AppContainer 注入。Fake 仓库保留在 core/data/fake/
子包下作为参考，不再被 ViewModel 引用。AndroidManifest 注册 application name。

## 任务 4：DataStore 设置层

用 Preferences DataStore 创建 SettingsRepository，至少包含：
- userInitial: String（默认 "K"，守护页头像字母）
- eveningReminderEnabled: Boolean（默认 true）
- eveningReminderTime: String（默认 "21:30"，HH:mm，Stage 9 通知用）
- seedVersion: Int（默认 1，标记预置数据版本，便于以后追加内置活动）

## 验证（必须全部通过再交付）

1. ./gradlew assembleDebug 编译通过
2. 安装后首次启动：灵感页能看到 20 条内置活动（筛选各类别有数据）
3. 记录页开始一条记录 → 杀进程 → 重开：计时继续、开始时间正确
4. 结束记录 → 杀进程 → 重开：历史条目仍在
5. 打卡 → 杀进程 → 重开：streak 天数正确、今日已打卡状态正确
6. 连续两天（可改系统时间模拟）打卡：streak 天数 +1
7. 设置页暂不存在，DataStore 用代码读写验证即可

## 遇到错误怎么办

编译失败或运行崩溃时，把完整错误堆栈原样贴回来，不要猜。不要改 core/model 的
字段定义，不要改任何 feature 目录下的 Screen 文件（UI 不归你管）。
```

---

## 验证清单（牛马执行完后你自己过一遍）

- [ ] `./gradlew assembleDebug` 通过
- [ ] `core/data/` 下出现 Room 数据库、DAO、entity、真实 Repository 实现
- [ ] ViewModel 不再 import 任何 Fake 类（Fake 文件保留但仅作参考）
- [ ] 杀进程重开后：进行中的计时、历史记录、streak 天数全部正确
- [ ] 灵感页 20 条内置活动完整，分类筛选正常
- [ ] 连续两天打卡 streak +1（可改系统时间验证）
- [ ] `git commit` 完成，message 形如 `feat: Stage 6 数据层落地 — Room + DataStore`
