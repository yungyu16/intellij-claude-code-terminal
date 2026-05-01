# AGENTS.md — AI 协作指南

本文件为 AI 编程助手（Claude Code、Copilot、Cursor 等）提供项目上下文，帮助其做出正确的决策。

## 项目概述

**intellij-claude-code-terminal** 是一个 JetBrains IDE 插件，提供一键聚焦或创建 Claude Code Terminal tab 的功能。

## 技术栈

| 技术 | 版本 | 说明 |
|------|------|------|
| Kotlin | 2.1.20 | 主语言 |
| IntelliJ Platform Gradle Plugin | 2.5.0 | 构建插件 |
| Target IDE | IDEA Community 2024.2 (build 242) | 最低兼容版本 |
| JDK | 17 | 构建和运行时要求 |

## 关键文件

```
src/main/kotlin/io/github/yungyu16/claudecodeterminal/
├── ClaudeTerminalAction.kt                  # 核心 Action：聚焦或创建 Terminal tab
├── ClaudeTerminalSettings.kt                # 应用级用户设置 + 命令构建
└── ClaudeTerminalSettingsConfigurable.kt    # Settings UI

src/main/resources/META-INF/plugin.xml       # 插件结构声明
build.gradle.kts                             # 构建配置 + 插件元数据
Makefile                                     # 构建快捷命令
```

## 运行行为

- 启动时不传 `--name` 参数，全新对话
- 首次触发时自动创建 **Local Term Tab**（默认 zsh shell），不参与 toggle / health-check 逻辑

## 构建与运行

```bash
make build    # 构建插件 ZIP
make run      # 在沙箱 IDEA 中运行
make clean    # 清理产物
```

Makefile 通过 `$(shell jenv javahome)` 动态获取 JAVA_HOME，项目目录已设置 `.java-version=17`。

## 并发安全设计

`ClaudeTerminalAction` 利用 EDT（事件分发线程）的单线程特性，用多个 `WeakHashMap` 作状态标记，防止重复创建和重复检测。Project / Content 关闭后自动 GC 回收。

### 核心标记

| 标记 | 作用 |
|------|------|
| `creationInProgress: WeakHashMap<Project, Boolean>` | Claude tab 创建中，防重复点击 |
| `localTabCreationInProgress: WeakHashMap<Project, Boolean>` | Local tab 创建中，防重复创建 |
| `tabCreatedAt: WeakHashMap<Content, Long>` | tab 创建时间戳，用于冷却期判断 |
| `healthCheckInProgress: WeakHashMap<Content, Boolean>` | 进程存活检测中，防重复检测 |

### 冷却期

创建后 `CREATION_COOLDOWN_MS = 3000ms` 内跳过存活检测，覆盖 Claude 启动到 shell integration 识别之间的窗口。

### 逻辑路径

1. 确保 Local Term Tab 存在（不存在则创建默认 zsh shell）
2. 找到已有 Claude tab + 进程存活 → toggle 聚焦/隐藏
3. 找到已有 Claude tab + 进程已死 → 关闭旧 tab → 重建
4. 无 Claude tab → `creationInProgress` guard → 创建 tab + 发命令 → tab 标记后释放 guard

> `hasRunningCommands()` 要求后台线程调用，所以存活检测通过 `executeOnPooledThread` offload，结果回 EDT 执行 UI 操作。

## 命名规范

- Tab 名称常量：`TAB_NAME = "Claude Code"`（tab 显示标题，不用于 tab 查找——查找依赖 `CLAUDE_TAB_KEY` UserData）
- Notification group id 必须与 `plugin.xml` 中的 `notificationGroup id` 完全一致
- Plugin id：`io.github.yungyu16.claude-code-terminal-tab`

## 注意事项

- `untilBuild` 设为 `null`（无上限），不要设置具体版本号
- `getActionUpdateThread()` 必须返回 `ActionUpdateThread.EDT`（update() 访问 UI）
- Settings 的 `commandName` getter 有 fallback 逻辑（空值返回 `"claude"`），修改时注意保留
