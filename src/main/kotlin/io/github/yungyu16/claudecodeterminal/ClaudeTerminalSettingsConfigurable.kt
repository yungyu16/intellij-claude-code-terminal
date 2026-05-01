package io.github.yungyu16.claudecodeterminal

import com.intellij.openapi.options.Configurable
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.COLUMNS_LARGE
import com.intellij.ui.dsl.builder.COLUMNS_MEDIUM
import com.intellij.ui.dsl.builder.columns
import com.intellij.ui.dsl.builder.panel
import javax.swing.JComponent

/**
 * Settings → Tools → Claude Code 终端 配置页面。
 *
 * 使用 IntelliJ UI DSL (panel { }) 构建，所有字段与 [ClaudeTerminalSettings] 双向绑定。
 * 在 isModified / apply / reset 中同步 UI 组件与持久化状态。
 */
class ClaudeTerminalSettingsConfigurable : Configurable {

    private lateinit var commandField: JBTextField
    private lateinit var modelField: JBTextField
    private lateinit var fullscreenRenderingCheckbox: JBCheckBox
    private lateinit var verboseCheckbox: JBCheckBox
    private lateinit var skipPermissionsCheckbox: JBCheckBox
    private lateinit var extraArgsField: JBTextField

    override fun getDisplayName(): String = "Claude Code 终端"

    override fun createComponent(): JComponent {
        return panel {
            group("基本配置") {
                row("启动命令") {
                    commandField = textField()
                        .columns(COLUMNS_MEDIUM)
                        .applyToComponent { emptyText.text = "默认：${ClaudeTerminalSettings.DEFAULT_COMMAND}" }
                        .comment("留空使用 <b>${ClaudeTerminalSettings.DEFAULT_COMMAND}</b>；若未加入 PATH 可填完整路径，如 <b>/usr/local/bin/claude</b>")
                        .component
                }
                row("模型 (--model)") {
                    modelField = textField()
                        .columns(COLUMNS_MEDIUM)
                        .applyToComponent { emptyText.text = "默认：账户默认模型" }
                        .comment("留空使用账户默认模型。常用别名：<b>sonnet</b>（均衡）、<b>opus</b>（最强推理）、<b>haiku</b>（轻量快速）")
                        .component
                }
            }
            group("其他参数") {
                row {
                    fullscreenRenderingCheckbox = checkBox("启用全屏渲染")
                        .comment(
                            "启用备用屏幕缓冲区渲染（类似 vim / htop），消除闪烁、长对话内存保持稳定。" +
                            "一并关闭鼠标捕获，保留终端原生操作——文本选择、Cmd+F、tmux 复制模式等均正常工作。<br>" +
                            "启用后 Claude Code 内鼠标功能不可用，请改用键盘 PgUp/PgDn、Ctrl+Home/End 导航。<br>" +
                            "等价于 <b>/tui fullscreen</b>（需 v2.1.89+）。运行 <b>/tui default</b> 可切回默认模式。" +
                            "<br><a href='https://code.claude.com/docs/zh-CN/fullscreen'>查看官方文档</a>"
                        )
                        .component
                }
                row {
                    verboseCheckbox = checkBox("--verbose  详细输出")
                        .comment("显示完整流式输出及 tool call 细节，调试时有用")
                        .component
                }
                row {
                    skipPermissionsCheckbox = checkBox("--dangerously-skip-permissions  跳过权限确认 ⚠️")
                        .comment("绕过所有工具调用的权限弹窗。<b>仅限无互联网访问的隔离沙箱使用，日常开发请勿开启。</b>")
                        .component
                }
                row("追加参数") {
                    extraArgsField = textField()
                        .columns(COLUMNS_LARGE)
                        .applyToComponent { emptyText.text = "默认：（无）" }
                        .comment("拼接到命令末尾，例如：<b>--add-dir /path --permission-mode acceptEdits</b>")
                        .component
                }
            }
        }
    }

    override fun isModified(): Boolean {
        val s = ClaudeTerminalSettings.getInstance()
        return commandField.text.trim() != s.myState.commandName ||
                modelField.text.trim() != s.myState.model ||
                fullscreenRenderingCheckbox.isSelected != s.fullscreenRendering ||
                verboseCheckbox.isSelected != s.verbose ||
                skipPermissionsCheckbox.isSelected != s.skipPermissions ||
                extraArgsField.text.trim() != s.myState.extraArgs
    }

    override fun apply() {
        val s = ClaudeTerminalSettings.getInstance()
        s.commandName = commandField.text
        s.model = modelField.text
        s.fullscreenRendering = fullscreenRenderingCheckbox.isSelected
        s.verbose = verboseCheckbox.isSelected
        s.skipPermissions = skipPermissionsCheckbox.isSelected
        s.extraArgs = extraArgsField.text
    }

    override fun reset() {
        val s = ClaudeTerminalSettings.getInstance()
        commandField.text = s.myState.commandName
        modelField.text = s.myState.model
        fullscreenRenderingCheckbox.isSelected = s.fullscreenRendering
        verboseCheckbox.isSelected = s.verbose
        skipPermissionsCheckbox.isSelected = s.skipPermissions
        extraArgsField.text = s.myState.extraArgs
    }
}
