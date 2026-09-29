# 动效体系重构计划 — 「潮汐」节拍系统 + 细节修复

> 用户已拍板四项决策：① 收敛为谐波同步的华丽 ② SOS 扩散环常开 ③ 灵感页筛选加流光过渡 ④ 纳入系统减弱动态效果。
> 另有三个细节差评必须修：卡片扫光带掉价、背景氛围层看不清、底部导航图标弹跳有生硬断裂。

---

## 一、谐波节拍表（所有无限动画的唯一标准）

基频 **4s**（呼吸节奏）。所有循环周期必须是 4s 的整数倍，相位同源（全部从 `rememberAuroraPhase` 派生或同参数自建），保证每 32s 全屏同相一次。

| 周期 | 分配给 |
|------|--------|
| **4s** | 脉动环、呼吸圆点、计时器辉光、SOS 扩散环（常开）、SOS 投影呼吸、边框呼吸 |
| **8s** | 数字背后光晕、流动虚线边框爬行、角落呼吸光斑 |
| **16s** | 卡片流光、空闲卡虚线爬行 |
| **32s** | 环境眩光 AuroraBackground、旋转弧光 OrbitArcs |

交互反馈层（bounceClick、转场、入场交错、描边进度）不受节拍约束，保持现有手感。

### 具体参数改动（旧 → 新）

| 位置 | 参数 | 旧 | 新 |
|------|------|----|----|
| `Aurora.kt` AuroraBackground | phase 周期 | 16000 | **32000** |
| `Aurora.kt` CardShimmer | phase 周期 | 9000 | **16000** |
| `SoberScreen` PulseRing | tween | 2600 | **4000** |
| `SoberScreen` StreakHero 光晕 | rememberAuroraPhase | 6000 | **8000** |
| `SoberScreen` OrbitArcs | rememberAuroraPhase | 11000 | **32000** |
| `TimeLogScreen` RecordingCard 边框爬行 | phase 周期 | 6000 | **8000** |
| `TimeLogScreen` RecordingCard 计时器辉光 | rememberBreathingAlpha | 2600 | **4000** |
| `TimeLogScreen` IdleCard 虚线爬行 | phase 周期 | 10000 | **16000** |
| `SosButton` 双层扩散环 | tween | 1800 | **4000** |
| `SosButton` 投影呼吸 | rememberBreathingAlpha | 3000 | **4000** |

---

## 二、细节修复（三个差评）

### 2.1 卡片扫光带掉价 → 换成「湿琥珀」漫射光

`CardShimmer` 重写：
- **删掉对角线扫光带**（`drawRect` + `linearGradient` 那段整个移除）——窄高光带在深色卡片上就是廉价感的来源
- 替换为**一团大面积软光缓慢漂移**：径向渐变光斑（半径 = 卡片宽度的 0.6~0.8 倍），中心沿水平方向缓慢往返（sin(phase) 驱动，幅度为卡片宽度的 30%），alpha 峰值 0.08
- 保留右上角呼吸光斑，但 alpha 上限从 0.11 提到 0.13，半径放大到 0.6 倍宽
- 效果目标：像烛光透过湿琥珀，而不是手电筒扫过塑料壳

### 2.2 背景氛围层看不清 → 提亮 + 拉开对比

`AuroraBackground` 调整：
- 主琥珀光 alpha 0.16 → **0.26**，副琥珀光 0.10 → **0.16**，眩光亮点 0.20 → **0.30**（亮点半径 0.22 → 0.26，亮度和存在感都要够）
- 烟雾冷光 0.05 → 0.07（微调即可，它是托底不是主角）
- 新增**边缘压暗**：Canvas 最后叠一层从边缘向中心的 radialGradient（透明 → InkBackground 60% 透明），暗角让中央光斑立刻"显形"，这是电影打光的基本功
- 各屏 intensity 微调：守护 1.0、灵感 0.8、记录 0.7（现在 0.7/0.6 太保守）

### 2.3 底部导航图标弹跳断裂 → 分方向用不同动画规格

`LocusBottomNav` 的 `iconScale` 修复：
- 病因：选中/取消都走 MediumBouncy spring，取消选中时从 1.15 回弹会下冲到 0.95 以下再弹回，视觉上就是"弹跳结束后突然缩小"
- 处方：**选中时**用 spring（DampingRatioMediumBouncy）冲到 1.15；**取消选中时**用 tween(250, EaseOut) 平滑回到 1f，不弹跳
- 实现：`animateFloatAsState(target, if (isSelected) bouncySpring else gentleTween)`
- 顺带检查 glow 光晕的 `if (glowAlpha > 0f)` 条件渲染：淡出不彻底时 Box 突然消失可能造成 1 帧跳变，改为始终渲染、用 alpha 控制可见性

---

## 三、灵感页筛选过渡（决策③）

`FilterChipRow` 目前选中态背景是瞬时切换（无动画）。修复：
- chip 背景色：Amber ↔ InkSurface 用 `animateColorAsState(tween 300)` 过渡
- chip 文字色：InkBackground ↔ Stone 同步过渡
- 选中 chip 额外加一圈 `AmberDim` 光晕淡入（animateFloatAsState 300ms）
- 不改布局结构（滑动指示器工程量大收益低，不做）

---

## 四、减弱动态效果（决策④）

在 `designsystem/component/Aurora.kt`（或新建 `Motion.kt` 同级文件）加：

```kotlin
val LocalReduceMotion = staticCompositionLocalOf { false }
```

- `LocusTheme` 中读取系统设置：API 26~32 读 `Settings.Global.ANIMATOR_DURATION_SCALE == 0f`；API 33+ 可用 `ContentResolver` 同值判断（无单独"减弱动态"开关，这是 Android 现状）
- 所有无限动画的入口处判断：`if (reduceMotion) 静态渲染（相位固定取 0、alpha 取区间中值）else 现有动画`
- 改造优先级：AuroraBackground / CardShimmer / PulseRing / OrbitArcs / SOS 扩散环 / 虚线爬行；bounceClick 和入场动画保留（交互反馈不属于装饰）
- SOS 扩散环在 reduceMotion 下保留一个低透明度静态环（功能暗示不能丢）

---

## 五、执行顺序与验证

1. 先改 `Aurora.kt`（节拍参数 + 扫光带重写 + 眩光提亮 + LocalReduceMotion）——它是所有人的依赖
2. 再改 `SoberScreen` / `TimeLogScreen` / `SosButton` 的周期参数（对照第一节表格逐个过）
3. 改 `LocusBottomNav`（分方向动画 + glow 始终渲染）
4. 改 `FilterChipRow`（颜色过渡 + 光晕淡入）
5. 逐屏接 reduceMotion 判断
6. 验证：编译通过；目视检查守护/灵感/记录三屏——卡片无扫光带、背景眩光肉眼可见、
   底部导航来回切换五次无跳变、筛选 chip 颜色渐变过渡；系统开发者选项关掉
   动画时长缩放后全屏装饰动画静止

完成后更新 `docs/01-设计系统-墨石.md` 的动效章节（写入节拍表），并更新 `.zcode/HANDOVER.md`。
