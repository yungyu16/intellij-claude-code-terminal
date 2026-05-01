package io.github.yungyu16.claudecodeterminal

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage

/**
 * 应用级持久化设置，存储 Claude Code 终端的启动参数。
 *
 * 通过 [PersistentStateComponent] 自动序列化到 `ClaudeTerminalSettings.xml`。
 * 所有字段设默认值的空判断由对应 getter 处理，[State] 仅作为序列化载体。
 *
 * @see ClaudeTerminalSettingsConfigurable 配置页面
 * @see buildCommand 将当前设置组装为 CLI 命令字符串
 */
@State(
    name = "ClaudeTerminalSettings",
    storages = [Storage("ClaudeTerminalSettings.xml")]
)
@Service(Service.Level.APP)
class ClaudeTerminalSettings : PersistentStateComponent<ClaudeTerminalSettings.State> {

    data class State(
        var commandName: String = "",
        var model: String = "",
        var fullscreenRendering: Boolean = true,
        var verbose: Boolean = false,
        var skipPermissions: Boolean = false,
        var extraArgs: String = ""
    )

    internal var myState = State()

    override fun getState(): State = myState

    override fun loadState(state: State) {
        myState = state
    }

    var commandName: String
        get() = myState.commandName.ifBlank { DEFAULT_COMMAND }
        set(value) { myState.commandName = value.trim() }

    var model: String
        get() = myState.model.trim()
        set(value) { myState.model = value.trim() }

    var fullscreenRendering: Boolean
        get() = myState.fullscreenRendering
        set(value) { myState.fullscreenRendering = value }

    var verbose: Boolean
        get() = myState.verbose
        set(value) { myState.verbose = value }

    var skipPermissions: Boolean
        get() = myState.skipPermissions
        set(value) { myState.skipPermissions = value }

    var extraArgs: String
        get() = myState.extraArgs.trim()
        set(value) { myState.extraArgs = value.trim() }

    /** 构造完整的 Claude Code 启动命令。
     *
     * 按序拼装：环境变量 → CLI 路径 → 模型 → verbose → skip-permissions → 追加参数。
     * 环境变量仅在 [fullscreenRendering] 开启时添加。
     *
     * 示例输出：`CLAUDE_CODE_NO_FLICKER=1 CLAUDE_CODE_DISABLE_MOUSE=1 claude --model sonnet`
     */
    fun buildCommand(): String = buildString {
        if (fullscreenRendering) append("CLAUDE_CODE_NO_FLICKER=1 CLAUDE_CODE_DISABLE_MOUSE=1 ")
        append(commandName)
        if (model.isNotBlank()) append(" --model ").append(model)
        if (verbose) append(" --verbose")
        if (skipPermissions) append(" --dangerously-skip-permissions")
        if (extraArgs.isNotBlank()) append(" ").append(extraArgs)
    }

    companion object {
        /** CLI 默认路径，commandName 为空时的 fallback */
        const val DEFAULT_COMMAND = "claude"

        /** 获取应用级单例 */
        fun getInstance(): ClaudeTerminalSettings =
            ApplicationManager.getApplication().getService(ClaudeTerminalSettings::class.java)
    }
}
