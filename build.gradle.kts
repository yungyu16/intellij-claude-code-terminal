plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.intellij.platform)
}

group = "io.github.yungyu16"
// CI 通过环境变量 PLUGIN_VERSION 注入（来自 git tag 去掉 v 前缀）；本地默认 1.0.0-dev
version = System.getenv("PLUGIN_VERSION") ?: "1.0.0-dev"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        intellijIdeaCommunity("2024.2")
        bundledPlugin("org.jetbrains.plugins.terminal")
    }
}

intellijPlatform {
    pluginConfiguration {
        id = "io.github.yungyu16.claude-code-terminal-tab"
        name = "Claude Code Terminal Tab"
        version = project.version.toString()

        description = """
            <p>Open Claude Code in JetBrains IDE's built-in Terminal with a single click.</p>
            <br/>
            <p><b>Background</b></p>
            <p>
                Claude Code officially provides the <a href="https://plugins.jetbrains.com/plugin/27310-claude-code-beta-">Claude Code (Beta)</a> plugin with two core capabilities:
                (1) IDE MCP Server — exposes IDE context (open files, selections) to the CLI;
                (2) Quick terminal session — open/activate a Claude Code session in the built-in Terminal.
            </p>
            <p>
                However, the official plugin has long remained in Beta (<code>0.1.14-beta</code>), rated 2.5/5, and has not been updated for over five months.
                Capability (2) has a known bug: the button is unresponsive when multiple Projects are open.
                Related issues (GitHub #22232, #1232) have been reported for a long time without a fix.
            </p>
            <p>
                This plugin is built specifically to fix that problem.
                Use both together: the official plugin provides IDE MCP Server, this plugin handles reliable terminal session toggling.
            </p>
            <br/>
            <p><b>Features</b></p>
            <ul>
                <li>Click the Toolbar button to focus the <b>Claude Code</b> Terminal tab instantly; hides if already active, focuses if inactive.</li>
                <li>If the tab doesn't exist, it will be created and the launch command will be executed automatically.</li>
                <li>Process health check: automatically detects if the Claude process has exited and recreates the tab.</li>
                <li>Concurrent-click safe — rapid clicks never create duplicate tabs.</li>
                <li>Optional fullscreen rendering: eliminates flickering, stabilizes memory for long conversations.</li>
                <li>Customize command, model, verbose mode, extra args, and more in <b>Settings → Tools → Claude Code 终端</b>.</li>
            </ul>
            <br/>
            <p>一键在 JetBrains IDE 内置 Terminal 中打开 Claude Code，支持进程存活检测、自动重建和全屏渲染。</p>
            <br/>
            <p><b>背景</b></p>
            <p>
                Claude Code 官方已提供 <a href="https://plugins.jetbrains.com/plugin/27310-claude-code-beta-">Claude Code (Beta)</a> 插件，核心能力包括：
                （1）IDE MCP Server — 向 CLI 暴露 IDE 上下文（打开的文件、选中内容等）；
                （2）快捷打开终端会话 — 在 IDE 内置终端中一键打开或激活 Claude Code 会话。
            </p>
            <p>
                然而该插件长期处于 Beta 阶段（<code>0.1.14-beta</code>），评分仅 2.5/5，且已超过五个月未更新。
                能力（2）在多 Project 窗口下存在已知 bug：按钮点击无响应。
                相关问题（GitHub #22232、#1232）积压已久，未见修复。
            </p>
            <p>
                本插件正是为解决这一问题而开发。与官方插件配合使用：官方插件提供 IDE MCP Server，本插件负责终端会话的可靠唤起。
            </p>
            <br/>
            <p><b>功能</b></p>
            <ul>
                <li>点击 Toolbar 按钮，自动聚焦 <b>Claude Code</b> Terminal tab；已激活则隐藏，未激活则聚焦。</li>
                <li>若 tab 不存在，自动新建并执行启动命令（默认 <code>claude</code>）。</li>
                <li>进程存活检测：tab 已创建但 Claude 进程已退出时，自动关闭旧 tab 并重建。</li>
                <li>防并发：连续快速点击不会重复创建多个 tab。</li>
                <li>可选全屏渲染：消除闪烁、长对话内存稳定，同时保留终端原生选择与搜索。</li>
                <li>可在 <b>Settings → Tools → Claude Code 终端</b> 中自定义命令、模型、verbose、追加参数等。</li>
            </ul>
        """.trimIndent()

        vendor {
            name = "yungyu16"
            url = "https://github.com/yungyu16"
        }

        changeNotes = """
            <p>完整更新日志参见 <a href="https://github.com/yungyu16/intellij-claude-code-terminal/releases">GitHub Releases</a>。</p>
        """.trimIndent()

        ideaVersion {
            sinceBuild = "242"
            untilBuild = provider { null }
        }
    }

}

kotlin {
    jvmToolchain(17)
}
