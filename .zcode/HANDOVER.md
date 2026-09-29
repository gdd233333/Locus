# Locus 项目交接文档（给 Zcode）

> 交接时间：2026-09-29。接手前请先读 `docs/00-项目总览.md`（项目宪法），再以本文为准了解当前进度。

---

## 一句话状态

**文档与规划阶段已全部完成，Stage 1（环境搭建）已完成，下一步是 Stage 2（设计系统实现）。**

## 已完成

| 事项 | 位置 | 状态 |
|------|------|------|
| 高保真设计稿（3 版本，已锁定版本A「墨石」） | `design/` | ✅ 版本A 已锁定，B/C 仅存档 |
| 项目总览（愿景、锁定决策、技术栈、路线图） | `docs/00-项目总览.md` | ✅ |
| 设计系统规范（配色/字阶/间距/圆角/动效/组件） | `docs/01-设计系统-墨石.md` | ✅ 从版本A HTML 提取 |
| 信息架构（数据模型、屏幕清单、UiState 契约） | `docs/02-信息架构.md` | ✅ |
| Stage 1~5 开发提示词文档 | `docs/03` ~ `docs/07` | ✅ 每个含自包含可复制提示词 |
| Android 项目初始化（空壳） | `app/` | ✅ 能编译，已提交 git |
| git 初始化 | 仓库根 | ✅ 2 个 commit（最新 `bc9ebb0 chore: 添加 .gitignore`） |

## 当前代码实况（注意与文档的差异）

- 项目目前只有**单一 `app` 模块**的空壳：`app/app/src/main/java/com/locus/app/MainActivity.kt` 只显示一个深色背景 + "Locus" 文字。
- `docs/00` 中规划的 `designsystem/`、`core/`、`feature/` 多模块/包结构**尚未创建**，由 Stage 2 起逐步落地。
- 构建配置：`Gradle KTS`，`app/settings.gradle.kts` 已配置阿里云镜像优先（国内网络环境，勿删）。
- 包名 `com.locus.app`，minSdk 26，targetSdk 34。
- 注意：当前 MainActivity 临时用了 `material3.Text`，Stage 2 实现自定义设计系统后应移除 Material3 依赖。

## 下一步（按此顺序执行）

1. **Stage 2 — 设计系统实现**：打开 `docs/04-Stage2-设计系统实现.md`，按其中「提示词」部分执行。产出：Compose 主题 + tokens + 基础组件（颜色/字体/间距/圆角/动效以 `docs/01` 为唯一标准）。
2. **Stage 3~5**（可并行）：`docs/05`（戒断中心）、`docs/06`（灵感行动库）、`docs/07`（时间记录）。每屏先定 `UiState` + 假 Repository，用假数据跑通 UI。
3. 每个 Stage 完成后验证 `./gradlew assembleDebug` 通过并提交 git。

## 关键约束（来自 docs/00，勿违背）

- Kotlin 唯一语言；Jetpack Compose，**不用 Material3**（自定义设计系统）。
- 视觉以 `design/版本A - 墨石（深色极简）.html` 为准，数值以 `docs/01` 为准。
- 约束策略：软提醒 + 数据复盘，不做硬阻断/VPN 拦截。
- 时间模块是**手动记录**，非计划提醒。
- 存储：Room + DataStore，全离线。

## 工作原则提醒

- UI 状态契约（`docs/02` 中的 UiState 数据类）就是业务层接口规范——前端约束业务。
- `designsystem` 不依赖任何 feature 模块，须可独立编译。
- Stage 文档中的提示词是给执行 Agent 用的，自包含；若需更多上下文，回查 `docs/01` 和 `docs/02`。
