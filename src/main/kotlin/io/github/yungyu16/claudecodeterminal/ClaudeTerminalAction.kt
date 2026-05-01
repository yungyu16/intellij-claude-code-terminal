package io.github.yungyu16.claudecodeterminal

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.ui.content.Content
import org.jetbrains.plugins.terminal.ShellTerminalWidget
import org.jetbrains.plugins.terminal.TerminalToolWindowManager
import java.util.*

class ClaudeTerminalAction : AnAction() {

    companion object {
        /** Claude Code 终端标签页的显示名称 */
        private const val TAB_NAME = "Claude Code"

        /** 保底创建的 Local shell 标签页名称 */
        private const val LOCAL_TAB_NAME = "Local"

        /** Terminal 工具窗口 ID */
        private const val TOOL_WINDOW_ID = "Terminal"

        /** 通知组 ID（需与 plugin.xml 中 [notificationGroup].id 一致） */
        private const val NOTIFICATION_GROUP = "Claude Code Terminal"

        /** 用 UserData Key 标识 Claude tab，避免依赖不稳定的 displayName */
        private val CLAUDE_TAB_KEY = Key.create<Boolean>("ClaudeCodeTab")

        /** 用 UserData Key 标识 Local tab */
        private val LOCAL_TAB_KEY = Key.create<Boolean>("LocalTermTab")

        private val LOG = logger<ClaudeTerminalAction>()

        /**
         * 创建后存活检测冷却期。
         * 覆盖 Claude 启动到 shell integration 注册前台进程之间的窗口，避免误判进程已退出。
         */
        private const val CREATION_COOLDOWN_MS = 3000L

        /**
         * 防并发守卫：Project / Content 关闭后自动 GC，避免内存泄漏。
         *
         * creationInProgress / localTabCreationInProgress：当前均为同步调用链，防重入实际上不会触发；
         * 保留此结构作为未来改为异步创建时的骨架，同时也能防御外部（插件/脚本）绕过 EDT 的调用。
         */
        private val creationInProgress = WeakHashMap<Project, Boolean>()
        private val localTabCreationInProgress = WeakHashMap<Project, Boolean>()
        private val tabCreatedAt = WeakHashMap<Content, Long>()
        private val healthCheckInProgress = WeakHashMap<Content, Boolean>()
    }

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val toolWindow = ToolWindowManager.getInstance(project).getToolWindow(TOOL_WINDOW_ID) ?: return

        // 确保 Local Term Tab 存在
        ensureLocalTab(project, toolWindow)

        val existing = toolWindow.contentManager.contents.firstOrNull { it.getUserData(CLAUDE_TAB_KEY) == true }
        if (existing != null) {
            activateExistingTab(project, toolWindow, existing)
        } else {
            createTab(project)
        }
    }

    override fun update(e: AnActionEvent) {
        val project = e.project
        if (project == null) {
            e.presentation.isEnabledAndVisible = false
            return
        }
        e.presentation.isEnabledAndVisible =
            ToolWindowManager.getInstance(project).getToolWindow(TOOL_WINDOW_ID) != null
    }

    // ── tab 激活 / 重建 ──────────────────────────────────────────

    private fun activateExistingTab(project: Project, toolWindow: ToolWindow, tab: Content) {
        // 创建中：Claude 尚未完全启动，直接聚焦
        if (creationInProgress[project] == true) {
            toolWindow.activate { toolWindow.contentManager.setSelectedContent(tab) }
            return
        }

        // 冷却期内：刚创建不久，Claude 大概率在启动中，跳过存活检测
        val age = System.currentTimeMillis() - (tabCreatedAt[tab] ?: 0L)
        if (age < CREATION_COOLDOWN_MS) {
            toolWindow.activate { toolWindow.contentManager.setSelectedContent(tab) }
            return
        }

        // 防止同一 tab 的重复存活检测
        if (healthCheckInProgress[tab] == true) return
        healthCheckInProgress[tab] = true

        // hasRunningCommands() 要求后台线程，offload 后回 EDT 执行 UI 操作
        ApplicationManager.getApplication().executeOnPooledThread {
            val alive = isProcessAlive(tab)
            ApplicationManager.getApplication().invokeLater {
                healthCheckInProgress.remove(tab)
                if (alive) {
                    toggleTab(toolWindow, tab)
                } else {
                    LOG.info("Claude process is dead, removing tab and recreating")
                    tabCreatedAt.remove(tab)
                    toolWindow.contentManager.removeContent(tab, true)
                    createTab(project)
                }
            }
        }
    }

    private fun toggleTab(toolWindow: ToolWindow, tab: Content) {
        val isActive = toolWindow.isVisible && toolWindow.contentManager.selectedContent == tab
        if (isActive) {
            toolWindow.hide()
        } else {
            toolWindow.activate { toolWindow.contentManager.setSelectedContent(tab) }
        }
    }

    // ── tab 创建 ─────────────────────────────────────────────────

    private fun createTab(project: Project) {
        if (creationInProgress[project] == true) return
        creationInProgress[project] = true
        try {
            doCreateTab(project)
        } catch (ex: Exception) {
            LOG.error("Failed to create Claude Code terminal tab", ex)
            notifyError(project, "打开 Claude Code 终端失败: ${ex.message}")
        } finally {
            creationInProgress.remove(project)
        }
    }

    private fun doCreateTab(project: Project) {
        val basePath = project.basePath
        if (basePath == null) {
            notifyWarning(project, "无法打开 Claude Code：当前项目无根目录。")
            return
        }
        val command = ClaudeTerminalSettings.getInstance().buildCommand()

        val terminalManager = TerminalToolWindowManager.getInstance(project)
        val widget = terminalManager.createShellWidget(basePath, TAB_NAME, true, true)
        // 通过 container 直接获取 Content 并打标记，避免 listener 泄漏和竞态
        val content = try {
            terminalManager.getContainer(widget)?.content
        } catch (ex: Exception) {
            LOG.warn("getContainer() threw unexpectedly; CLAUDE_TAB_KEY not set — duplicate tab creation may occur", ex)
            null
        }
        if (content != null) {
            content.putUserData(CLAUDE_TAB_KEY, true)
            tabCreatedAt[content] = System.currentTimeMillis()
        } else {
            LOG.warn("getContainer() returned null; CLAUDE_TAB_KEY not set — duplicate tab creation may occur")
        }
        widget.sendCommandToExecute(command)
    }

    // ── Local Term Tab ─────────────────────────────────────────────

    /**
     * 确保 Local Term Tab 存在；不存在则创建一个默认 zsh shell。
     * Local tab 不参与 toggle / health-check 逻辑。
     * 查找时优先匹配 LOCAL_TAB_KEY 标记，兜底匹配 Tab Name == "Local"，保证最多只有一个。
     */
    private fun ensureLocalTab(project: Project, toolWindow: ToolWindow) {
        val existing = toolWindow.contentManager.contents.firstOrNull { it.getUserData(LOCAL_TAB_KEY) == true }
            ?: toolWindow.contentManager.contents.firstOrNull { it.displayName == LOCAL_TAB_NAME }
        if (existing == null) {
            createLocalTab(project)
        }
    }

    private fun createLocalTab(project: Project) {
        if (localTabCreationInProgress[project] == true) return
        localTabCreationInProgress[project] = true
        try {
            doCreateLocalTab(project)
        } catch (ex: Exception) {
            LOG.error("Failed to create Local terminal tab", ex)
        } finally {
            localTabCreationInProgress.remove(project)
        }
    }

    private fun doCreateLocalTab(project: Project) {
        val basePath = project.basePath
        if (basePath == null) {
            LOG.warn("Cannot create Local tab: no project base path")
            return
        }
        val terminalManager = TerminalToolWindowManager.getInstance(project)
        val widget = terminalManager.createShellWidget(basePath, LOCAL_TAB_NAME, true, true)
        val content = try {
            terminalManager.getContainer(widget)?.content
        } catch (ex: Exception) {
            LOG.warn("getContainer() threw unexpectedly; LOCAL_TAB_KEY not set — duplicate Local tab creation may occur", ex)
            null
        }
        if (content != null) {
            content.putUserData(LOCAL_TAB_KEY, true)
        } else {
            LOG.warn("getContainer() returned null; LOCAL_TAB_KEY not set — duplicate Local tab creation may occur")
        }
        // 不发送任何命令，保持默认 zsh shell
    }

    // ── 进程存活检测 ──────────────────────────────────────────────

    /**
     * 检测 Claude 是否仍在以前台进程运行。必须在后台线程调用——hasRunningCommands() 内部断言 off-EDT。
     *
     * 判断逻辑：
     * - widget 获取失败（null 或类型不符）        → 保守返回 true，避免误删有效 tab
     * - hasRunningCommands() == true             → shell integration 检测到前台进程，Claude 在运行
     * - ttyConnector.isConnected == false        → shell 本身已死，Claude 自然也不在
     * - hasRunningCommands() == false（其余情况）→ shell 活着但无前台进程，Claude 已退出
     * - 状态不可知（抛异常）                     → 保守返回 true，避免误删有效 tab
     *
     * 注：创建期间的竞态（Claude 刚启动尚未注册为前台进程）由调用方的冷却期守卫处理。
     */
    private fun isProcessAlive(content: Content): Boolean {
        val widget = TerminalToolWindowManager.getWidgetByContent(content) as? ShellTerminalWidget
            ?: return true  // 无法获取 widget，保守认为存活，避免误删有效 tab
        return try {
            // shell 本身已断开 → 整个终端已死，需要重建
            val connector = widget.ttyConnector
            if (connector != null && !connector.isConnected) return false
            // shell 存活但前台不是 Claude（无 running commands）→ 需要重建
            widget.hasRunningCommands()
        } catch (ex: Exception) {
            LOG.warn("Failed to check process status, assuming alive", ex)
            true
        }
    }

    // ── 通知 ──────────────────────────────────────────────────────

    private fun notifyWarning(project: Project, message: String) {
        notify(project, message, NotificationType.WARNING)
    }

    private fun notifyError(project: Project, message: String) {
        notify(project, message, NotificationType.ERROR)
    }

    private fun notify(project: Project, message: String, type: NotificationType) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup(NOTIFICATION_GROUP)
            ?.createNotification(message, type)
            ?.notify(project)
    }
}
