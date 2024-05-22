package ru.ozon.ideplugin.kelp.pluginConfig

import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.components.Service
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.project.Project
import ru.ozon.ideplugin.kelp.KelpBundle

/**
 * [Issue](https://youtrack.jetbrains.com/issue/GRZ-4351)
 */
@Service(Service.Level.PROJECT)
class GrazieProNotification(private val project: Project) {
    private var notified = false

    fun notifyIfNeeded(previousConfig: KelpConfig?, kelpConfig: KelpConfig) = invokeLater {
        if (!notified &&
            kelpConfig.iconsRendering?.gutterEnabled == true &&
            previousConfig?.iconsRendering?.gutterEnabled != true &&
            isGrazieProPluginEnabled()
        ) {
            NotificationGroupManager.getInstance()
                .getNotificationGroup(KelpBundle.message("grazieProNotificationGroup"))
                .createNotification(
                    title = KelpBundle.message("grazieProNotificationTitle"),
                    content = KelpBundle.message("grazieProNotificationMessage"),
                    type = NotificationType.WARNING,
                )
                .notify(project)

            notified = true
        }
    }

    private fun isGrazieProPluginEnabled(): Boolean {
        val pluginId = PluginId.getId(GRAZIE_PRO_PLUGIN_ID)
        return PluginManagerCore.getPlugin(pluginId) != null && !PluginManagerCore.isDisabled(pluginId)
    }
}

private const val GRAZIE_PRO_PLUGIN_ID = "com.intellij.grazie.pro"
