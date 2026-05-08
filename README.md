# Claude Code Terminal Tab

[![JetBrains Plugins](https://img.shields.io/jetbrains/plugin/v/io.github.yungyu16.claude-code-terminal-tab)](https://plugins.jetbrains.com/plugin/io.github.yungyu16.claude-code-terminal-tab)
[![GitHub Release](https://img.shields.io/github/v/release/yungyu16/intellij-claude-code-terminal)](https://github.com/yungyu16/intellij-claude-code-terminal/releases)
[![License](https://img.shields.io/github/license/yungyu16/intellij-claude-code-terminal)](LICENSE)
[![Platform](https://img.shields.io/badge/IDE-2024.2%2B-blue)](https://plugins.jetbrains.com/plugin/io.github.yungyu16.claude-code-terminal-tab)

在 JetBrains IDE 中一键唤起 Claude Code 终端。

## 背景

Claude Code 官方已提供 [Claude Code (Beta)](https://plugins.jetbrains.com/plugin/27310-claude-code-beta-)，核心能力包括：

1. **IDE MCP Server** — 向 Claude Code CLI 暴露 IDE 上下文（当前选中文件、编辑器内容等），让 CLI 能感知 IDE 状态
2. **快捷打开终端会话** — 在 IDE 内置终端中一键打开或激活 Claude Code 会话

然而该插件长期处于 **Beta** 阶段（当前版本 `0.1.14-beta`），评分仅 **2.5 / 5**（347 人），且已超过五个月未更新。用户反馈集中在：多 Project 窗口下按钮点击无响应、IDE 检测频繁失败（`No available IDEs detected`），相关问题在官方 GitHub issue #22232 / #1232 中积压已久，未见修复。

本插件正是为解决上述终端唤起问题而开发 —— 与官方插件配合使用：官方插件提供 IDE MCP Server，本插件负责终端会话的可靠唤起。

## 功能

- **一键切换** — 点击工具栏按钮自动切换：tab 不存在则创建、已激活则隐藏、未激活则聚焦。也可通过 **Tools → Open Claude Code Terminal Tab** 触发
- **进程保活** — 自动检测 Claude 进程状态，进程退出后自动关闭旧 tab 并重建，避免空终端残留
- **防抖处理** — 快速连续点击不会重复创建多个 tab
- **全屏渲染** — 可选启用，消除闪烁、长对话内存稳定。保留终端原生选择与搜索能力
- **可配置** — 支持自定义 CLI 路径、模型、verbose 等参数

## 安装

### 方式一：Marketplace（推荐）

在 IDE **Settings → Plugins → Marketplace** 搜索 "Claude Code Terminal Tab" 安装。

[→ JetBrains Marketplace](https://plugins.jetbrains.com/plugin/31453-claude-code-terminal-tab)

### 方式二：手动安装

1. 从 [GitHub Releases](https://github.com/yungyu16/intellij-claude-code-terminal/releases) 下载最新 `.zip`
2. IDE 内 **Settings → Plugins → ⚙ → Install Plugin from Disk...**，选择 `.zip`，重启

## 快速上手

1. 确保已安装 [Claude Code CLI](https://claude.ai/code) 并可在终端直接调用
2. 安装插件后，工具栏出现 ![](docs/icon.svg) 图标
3. 点击图标，首次使用自动创建 terminal tab 并启动 Claude Code

> 也可通过 **Tools → Open Claude Code Terminal Tab** 触发。
>
> 终端内可用 `/resume` 搜索切换历史会话，或 `/clear` 清空上下文。

## 配置

**Settings → Tools → Claude Code 终端**

| 配置项 | 说明 | 默认值 |
|--------|------|--------|
| 启动命令 | Claude Code CLI 路径 | `claude` |
| 模型 (--model) | 模型别名 | 账户默认 |
| 启用全屏渲染 | 备用屏幕缓冲区渲染，消除闪烁、稳定内存 | 关闭 |
| --verbose | 完整流式输出及 tool call 细节 | 关闭 |
| --dangerously-skip-permissions | 跳过权限弹窗（仅限隔离沙箱） | 关闭 |
| 追加参数 | 拼接到命令末尾的额外参数 | （无） |

## 环境要求

- JetBrains IDE **2024.2+**（IntelliJ IDEA、GoLand、PyCharm 等）
- [Claude Code CLI](https://claude.ai/code) 已安装并可正常调用

## 兼容性

当前在 **macOS** 上测试通过。Windows 受限于终端实现差异和 Claude Code CLI 支持状态，暂无完整适配，欢迎 PR。

## 参与贡献

欢迎提交 Issue 和 Pull Request！详见 [CONTRIBUTING.md](CONTRIBUTING.md)。

## License

[MIT](LICENSE)
