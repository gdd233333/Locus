# Locus 项目交接文档（给 Zcode）

> 更新时间：2026-09-29（第三次交接）。接手前请先读 `docs/00-项目总览.md`（项目宪法），再以本文为准了解当前进度。

---

## 一句话状态

**Stage 1~10 全部交付（6~10 为牛马执行、尚未 commit）；「潮汐」动效节拍体系已落地，桌面小组件设计已优化。下一步：人工验收 → 编译 → commit。**

## 当前进度

| 阶段 | 状态 | 说明 |
|------|------|------|
| Stage 1~5 | ✅ 完成并验收 | 三 Tab + 排版修正 + 动态眩光改造 |
| Stage 6 数据层 | ✅ 牛马交付，已 commit（e6398ae） | Room + DataStore + AppContainer 手动 DI |
| Stage 7~10 | ✅ 牛马交付，**未 commit** | 复盘页 / Glance 小组件 / 通知 / 守护页交互填充，工作区待验收提交 |
| 动效节拍体系 | ✅ 完成 | `.zcode/plans/plan-动效节拍体系.md` 已执行完毕 |

## 动效体系（2026-09 重构，新 UI 必须遵守）

**潮汐节拍**：所有装饰性循环动画周期 = 4s 基频整数倍，定义在 `LocusRhythm`
（`designsystem/component/Aurora.kt`）：BREATH 4s / GLOW 8s / SHIMMER 16s / AMBIENT 32s。
禁止自创周期。分层预算：环境层每屏恰 1 个 AuroraBackground；容器层 ≤ 2 张卡带
CardShimmer；焦点层每屏恰 1 个主角；反馈层（bounceClick/转场/入场）不受节拍约束。

**无障碍**：`LocalReduceMotion` 跟随系统动画时长缩放，true 时装饰动画静态化
（相位取 0 / 透明度取中值），SOS 保留静态环。新装饰动画必须接这个开关——
用 `rememberAuroraPhase` / `rememberBreathingAlpha` 就自动获得。

**已修复的差评（勿回退）：**
- 卡片流光 = 湿琥珀漫射软光，禁止窄条扫光带
- AuroraBackground 带边缘压暗暗角，透明度上限 0.30
- 底部导航图标：选中弹跳 spring / 取消平滑 tween（分方向，取消禁止弹跳）
- 筛选 chip 选中态全动画过渡（底色/文字/描边/光晕）

## 桌面小组件设计（Glance 限制：无动画、无自定义字体）

- 背景统一 `res/drawable/widget_bg.xml`（斜向渐变 #242120→#12100F + 24dp 圆角），
  用 `GlanceModifier.background(ImageProvider(R.drawable.widget_bg))` 引用
- 左侧 3dp 饰线是状态第一信号：活跃=实心琥珀，空闲/未打卡=StoneDark
- 配色集中在 `widget/WidgetSupport.kt`（WidgetColors），不许硬编码

## 数据层架构（Stage 6 产物）

- `LocusApplication` 持有 `AppContainer`（单例懒加载），小组件/通知/ViewModel 全从这里拿 Repository
- Fake 仓库保留在 `core/data/fake/` 仅作参考，勿再引用
- 通知：三渠道（CHANNEL_REMINDER / CHANNEL_ONGOING / CHANNEL_SOS），WorkManager 调度，
  权限申请时机在首次点打卡/开始记录时

## 待办（验收清单）

- [ ] `./gradlew assembleDebug` 编译通过（牛马或人工跑，本机 Git Bash 无 JAVA_HOME）
- [ ] 目视验收：卡片无扫光带、背景眩光可见、导航切换五次无跳变、chip 过渡渐变
- [ ] 开发者选项关闭动画缩放后：装饰动画静止，交互反馈保留
- [ ] Stage 7~10 功能逐项验收（对照 docs/09~12 各自验证清单）
- [ ] commit：`feat: Stage 7~10 + 动效节拍体系`
