# Stage 1 — 环境搭建与项目初始化

## 前置条件

- 一台 Windows 电脑
- 能访问 Google 的网络环境（下载 Android Studio 和 SDK）
- 至少 20GB 可用磁盘空间

## 目标

- Android Studio 安装完毕
- Locus 项目创建，能编译并运行到模拟器/真机（显示空白页面即可）
- git 仓库初始化，首次 commit 完成

---

## 提示词（直接复制给牛马 Agent）

```
你是一个 Android 开发环境搭建助手。请在 Windows 上完成以下任务：

## 任务 1：检查 Java 环境

打开 PowerShell，运行：
    java -version

需要 JDK 17 或更高版本。如果没有或版本太低：
1. 下载 Eclipse Temurin JDK 17：https://adoptium.net/temurin/releases/?version=17
   选择 Windows x64 MSI 安装包
2. 安装时勾选 "Set JAVA_HOME variable"
3. 重新打开 PowerShell 验证：java -version 应显示 17.x.x

## 任务 2：安装 Android Studio

1. 下载 Android Studio（最新稳定版）：https://developer.android.com/studio
2. 安装时使用默认选项，确保勾选：
   - Android SDK
   - Android SDK Command-line Tools
   - Android Virtual Device
3. 首次启动 Android Studio，完成 Setup Wizard：
   - 选择 Standard 安装类型
   - 接受所有 license
   - 等待 SDK 下载完成
4. 验证：打开 SDK Manager（Configure → SDK Manager），确认已安装：
   - Android SDK Platform 34
   - Android SDK Build-Tools 34.0.0
   - Android Emulator
   - Android SDK Platform-Tools

## 任务 3：创建 Locus 项目

在 Android Studio 中：
1. New Project → Empty Activity (Compose)
2. 填写：
   - Name: Locus
   - Package name: com.locus.app
   - Save location: D:\TakeControl\app（注意：不是 D:\TakeControl，是下面的 app 子目录）
   - Minimum SDK: API 26 ("Oreo; Android 8.0")
   - Build configuration language: Kotlin DSL (build.gradle.kts) ✓
3. 等 Gradle sync 完成

## 任务 4：配置项目结构

在 D:\TakeControl\app 目录下，创建以下目录结构（在 Android Studio 中右键 app/src/main/java/com/locus/app → New → Package）：

    com.locus.app
    ├── designsystem
    │   ├── theme
    │   ├── component
    │   └── texture
    ├── core
    │   ├── data
    │   ├── model
    │   └── common
    ├── feature
    │   ├── sober
    │   ├── inspire
    │   ├── timelog
    │   └── review
    └── MainActivity.kt（已存在）

暂时不需要在这些目录里创建任何文件，只要目录结构存在即可。

## 任务 5：修改 MainActivity.kt 验证 Compose 可用

把 MainActivity.kt 的内容替换为：

    package com.locus.app

    import android.os.Bundle
    import androidx.activity.ComponentActivity
    import androidx.activity.compose.setContent
    import androidx.compose.foundation.background
    import androidx.compose.foundation.layout.Box
    import androidx.compose.foundation.layout.fillMaxSize
    import androidx.compose.foundation.layout.padding
    import androidx.compose.material3.Text
    import androidx.compose.runtime.Composable
    import androidx.compose.ui.Alignment
    import androidx.compose.ui.Modifier
    import androidx.compose.ui.graphics.Color
    import androidx.compose.ui.text.font.FontWeight
    import androidx.compose.ui.unit.dp
    import androidx.compose.ui.unit.sp

    class MainActivity : ComponentActivity() {
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            setContent {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF12100F)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Locus",
                        color = Color(0xFFD9A566),
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Light,
                        letterSpacing = 0.1.sp
                    )
                }
            }
        }
    }

注意：暂时用 material3 的 Text 即可，Stage 2 会替换掉。现在只为验证 Compose 能跑。

## 任务 6：创建模拟器并运行

1. Android Studio → Device Manager → Create Virtual Device
2. 选择 Pixel 7 Pro → API 34 系统镜像 → 完成
3. 点击 Run（绿色三角）启动
4. 预期结果：模拟器中显示深黑背景 + 琥珀色 "Locus" 文字

## 任务 7：初始化 git 仓库

打开 PowerShell，执行：

    cd D:\TakeControl
    git init
    git add -A
    git commit -m "feat: 项目初始化 — Compose 空壳 + 目录结构"

注意：在 D:\TakeControl 目录下 init（不是 app 子目录），这样 design/ 和 docs/ 也会纳入版本管理。

创建 .gitignore 文件（放在 D:\TakeControl\ 根目录），内容：

    # Android
    app/build/
    app/.cxx/
    *.apk
    *.aab
    *.keystore
    local.properties

    # IDE
    .idea/
    .gradle/
    *.iml

    # OS
    Thumbs.db
    .DS_Store

然后再执行一次：
    git add -A
    git commit -m "chore: 添加 .gitignore"

## 验证清单

完成后逐项确认：
- [ ] java -version 显示 17.x.x 或更高
- [ ] Android Studio 能正常启动
- [ ] D:\TakeControl\app 目录存在且包含 settings.gradle.kts
- [ ] 模拟器能启动，显示深黑背景 + 琥珀色 "Locus" 文字
- [ ] git log 显示至少 2 个 commit
- [ ] git status 显示 working tree clean

如果任何一步失败，把完整的错误信息贴出来，不要自己猜测修复方案。
```

---

## 验证清单（牛马执行完后你自己过一遍）

- [ ] `D:\TakeControl\app\settings.gradle.kts` 存在
- [ ] `D:\TakeControl\app\app\build.gradle.kts` 存在且包含 `compose` 相关配置
- [ ] 模拟器启动后显示深黑背景 + 琥珀色文字
- [ ] `git log --oneline` 至少 2 条记录
- [ ] 项目目录结构包含 designsystem/core/feature 三个包
