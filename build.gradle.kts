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
            <p>一键在 JetBrains IDE 内置 Terminal 中打开 Claude Code，支持进程存活检测、自动重建和全屏渲染。</p>
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
            <br/>
            <p>Open Claude Code in JetBrains IDE's built-in Terminal with a single click.</p>
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

    // 发布到 JetBrains Marketplace（仅在 CI 中通过环境变量提供认证）
    publishing {
        token = System.getenv("JETBRAINS_TOKEN") ?: ""
    }

    // 插件签名（仅在 CI 中通过环境变量提供密钥）
    // 若密钥未配置，signPlugin 任务自动跳过，不影响本地构建
    signing {
        certificateChain = System.getenv("CERTIFICATE_CHAIN") ?: ""
        privateKey = System.getenv("PRIVATE_KEY") ?: ""
        password = System.getenv("PRIVATE_KEY_PASSWORD") ?: ""
    }
}

kotlin {
    jvmToolchain(17)
}
