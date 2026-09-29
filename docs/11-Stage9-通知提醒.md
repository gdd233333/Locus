# Stage 9 — 通知提醒

> 三条通知线：晚间打卡提醒、记录中常驻通知、冲动急救快捷入口。克制是原则——通知是陪伴，不是骚扰。

## 前置条件

- Stage 6 已完成：Room 数据层 + SettingsRepository（DataStore）可用，
  其中已预留 `eveningReminderEnabled` / `eveningReminderTime` 设置项
- Stage 8 建议已完成（非硬性）
- 通知调度统一走 WorkManager，不用 AlarmManager 直调（国产 ROM 存活率更高）

## 目标

- 晚间守护提醒：每天固定时间提醒打卡，时间可配（设置项已在 DataStore）
- 记录中常驻通知：进行中记录显示实时计时 + 「结束」操作按钮
- 冲动急救入口：常驻静默通知（最低优先级），下拉即达 SOS 弹层
- Android 13+ 通知权限申请流程合规

---

## 提示词（直接复制给牛马 Agent）

```
你是 Locus 项目的功能工程师。这是一个 Android 自律应用（Kotlin + Jetpack Compose +
Room + DataStore，包名 com.locus.app，minSdk 26，targetSdk 最新）。请实现通知系统。

## 项目现状（先读再动手）

- 数据入口：AppContainer（LocusApplication 持有），提供 StreakRepository /
  TimeLogRepository / SettingsRepository
- SettingsRepository 已有：eveningReminderEnabled (Boolean, 默认 true)、
  eveningReminderTime (String, "HH:mm", 默认 "21:30")
- app 路由："sober" / "inspire" / "timelog" / "review"（MainNavigation.kt），
  通知点击需要 deep link（Stage 8 如果已配好 deep link 就复用，没配就你配）
- SOS 急救弹层目前在守护页是 TODO（SosButton 的 onClick 为空），通知入口先
  deep link 到守护页即可，并在回复中说明弹层接线留待后续

## 任务 1：通知渠道（Application onCreate 时注册）

- CHANNEL_REMINDER：晚间提醒，重要性 DEFAULT，允许铃声震动
- CHANNEL_ONGOING：记录中常驻通知，重要性 LOW，无声音，不可滑动清除
- CHANNEL_SOS：急救入口常驻通知，重要性 MIN（折叠到通知栏底部，不打扰）

## 任务 2：晚间守护提醒

- WorkManager 每日任务：按 eveningReminderTime 调度
  （算好到目标时间的 initialDelay，之后 24h 周期）
- 通知文案：标题「守住今夜」，内容「今天还没有打卡，点一下就好」，
  若今日已打卡则当天不发（Worker 里先查 getTodayCheckIn）
- 点击 → deep link 守护页
- 附带一个「直接打卡」action：不打开 app 直接写入打卡并取消当天通知
- 设置项变更（开关/时间）时重新调度；手机重启后 BOOT_COMPLETED 恢复调度

## 任务 3：记录中常驻通知

- TimeLogRepository 有活跃记录期间：显示 ongoing 通知，
  内容 = 活动名 + 开始时间（用 Notification chronometer 显示实时计时，
  不要自己每秒发通知更新）
- 「结束记录」action：走 endLog → 取消通知
- 点击通知主体 → deep link 记录页
- 记录结束时通知自动消失
- 用前台 Service 还是普通 ongoing 通知由你判断，选更省电的方案并说明理由
  （注意 Android 14+ 前台服务类型声明要求）

## 任务 4：权限合规

- Android 13+ (API 33+)：首次进入守护页时用 ActivityResult 申请
  POST_NOTIFICATIONS，被拒时静默降级（app 内功能不受影响，不能弹窗轰炸）
- 申请时机要自然：用户第一次点「打卡守护」或「开始记录」时再申请，
  不要开屏就弹

## 约束

- 全离线，不涉及任何网络推送
- 不要改 core/model 模型定义；Repository 缺方法就自己补并说明
- 通知里所有文字走 strings.xml（现在是中文硬编码为主，新文案统一进 strings.xml）

## 验证

1. ./gradlew assembleDebug 通过
2. 改系统时间到提醒时刻前 1 分钟，等待：今日未打卡则收到通知，
   已打卡则不收到
3. 通知「直接打卡」action：不打开 app，进 app 后守护页显示今日已打卡
4. 开始一条记录：出现常驻通知且计时跳动；点「结束记录」：通知消失，
   app 内记录完成
5. 重启手机：提醒调度仍然有效（改时间再次验证）
6. Android 13+ 设备首次触发权限申请，拒绝后 app 不崩溃不骚扰

## 遇到错误怎么办

把完整错误堆栈贴回来。国产 ROM 后台限制导致 WorkManager 不准时是已知
现象，如实报告延迟时长即可，不要尝试保活黑科技。回复中列出新增加文件清单。
```

---

## 验证清单（牛马执行完后你自己过一遍）

- [ ] `./gradlew assembleDebug` 通过
- [ ] 三个通知渠道注册齐全，优先级符合设计（提醒有声、常驻无声、SOS 折叠）
- [ ] 晚间提醒按时到达，已打卡当天不发
- [ ] 常驻计时通知用 chronometer，不耗电刷屏
- [ ] 重启手机后调度恢复
- [ ] 权限拒绝后 app 功能正常
- [ ] `git commit` 完成，message 形如 `feat: Stage 9 通知提醒`
