# Locus 项目交接文档（给 Zcode）

> 更新时间：2026-09-29（第二次交接）。接手前请先读 `docs/00-项目总览.md`（项目宪法），再以本文为准了解当前进度。

---

## 一句话状态

**Stage 1~5（全部 UI）已完成并人工验收，已做排版修正与「动态眩光」动效改造；Stage 6~9 文档已就绪，下一步是 Stage 6（数据层落地）。**

## 当前进度

| 阶段 | 状态 | 说明 |
|------|------|------|
| Stage 1~5 | ✅ 完成并验收 | 三 Tab 齐全，用户已手动测试通过并 commit |
| UI 修正 + 动效改造 | ✅ 完成 | 大理石静态贴图全部删除，替换为动态眩光；排版问题已修 |
| **Stage 6 数据层** | ⬅ 下一步 | `docs/08-Stage6-数据层落地.md` |
| Stage 7 复盘 Tab | 待做（依赖 6） | `docs/09-Stage7-复盘Tab.md` |
| Stage 8 桌面小组件 | 待做（依赖 6） | `docs/10-Stage8-桌面小组件.md` |
| Stage 9 通知提醒 | 待做（依赖 6） | `docs/11-Stage9-通知提醒.md` |

## 当前代码架构（与 docs/00 有出入，以此为准）

- 单模块：`app/app/src/main/java/com/locus/app/`，包结构为 `designsystem/`、`core/`、`feature/`（未拆 Gradle module，够用不拆）
- 数据层：**目前仍是 Fake Repository**（`core/data/Fake*.kt`），ViewModel 构造参数默认实例直接持有；Stage 6 负责替换
- `designsystem/component/Aurora.kt` 是动效核心：`AuroraBackground`（全屏动态眩光）、`CardShimmer`（卡片流光）、`bounceClick`（按压回弹）、`rememberAuroraPhase` / `rememberBreathingAlpha`（相位工具）
- 视觉风格：墨石深色 + 琥珀眩光。所有新 UI 必须：根 Box 铺 `AuroraBackground`、卡片内加 `CardShimmer`、可点元素用 `bounceClick`、颜色只用 `designsystem/theme/` tokens

## 已修复的排版问题（不要再犯）

- SOS 按钮原来悬浮在距底 100dp 处压内容 → 现在贴底部导航上方 16dp
- 灵感卡片描述文字原来被 `fillMaxWidth(0.75f)` 砍掉 1/4 宽度 → 已放开
- 卡片右侧/顶部贴大理石图遮内容 → 已全部替换为卡片内流光
- 守护页脉动环原来计算了 scale 但没应用 → 已修，并加旋转弧光

## 工作原则

- Stage 6~9 的提示词是「需求式」写法：只定契约、规则和验收标准，不直接给代码（后端实现交给执行 Agent 自由发挥）
- Repository 接口签名是硬契约：Stage 6 替换实现时签名一字不动
- 每个 Stage 文档末尾都有双份验证清单（牛马一份、你一份）
- 提示词使用方式不变：整段复制给执行 Agent，失败了把错误原样贴回去

## 设计稿参考

`design/版本A - 墨石（深色极简）.html` 仍是视觉基准，但「大理石纹」的解读已更正为**动态眩光特效**（代码实现在 Aurora.kt），不要再生成或使用静态大理石贴图。
