package ru.ozon.ideplugin.kelp.pluginConfig

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.serviceOrNull
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.AsyncFileListener.ChangeApplier
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.readText
import kotlinx.serialization.json.Json
import ru.ozon.ideplugin.kelp.KelpBundle
import ru.ozon.ideplugin.kelp.liveTemplates.AddLiveTemplates
import ru.ozon.ideplugin.kelp.pluginConfigDirPath
import kotlin.io.path.div

/**
 * @return null, if this project does not contain kelp config file, meaning that plugin must be disabled for
 * this project.
 */
fun Project.kelpConfig(): KelpConfig? = serviceOrNull<KelpConfigService>()?.data

/**
 * To retrieve this service, you MUST use [kelpConfig].
 */
@Service(Service.Level.PROJECT)
private class KelpConfigService(private val project: Project) : Disposable {
    private val json = Json { ignoreUnknownKeys = true }
    private val configFilePath = pluginConfigDirPath(project) / CONFIG_FILE_NAME

    private var configText: String? = null
    var data: KelpConfig? = null

    init {
        reloadConfig(isFirstRun = true)
        VirtualFileManager.getInstance().addAsyncFileListener({ events ->
            if (!events.any { it.path == configFilePath.toString() }) return@addAsyncFileListener null

            object : ChangeApplier {
                override fun afterVfsChange() = reloadConfig(isFirstRun = false)
            }
        }, this)
    }

    private fun reloadConfig(isFirstRun: Boolean) {
        runCatching {
            runReadAction {
                configText = VirtualFileManager.getInstance()
                    .findFileByNioPath(configFilePath)
                    ?.readText()

                data = json.decodeFromString<KelpConfig>(configText ?: return@runReadAction)
                AddLiveTemplates.execute(data!!)
                if (!isFirstRun) reloadNotification()
            }
        }.onFailure {
            invalidConfigError(it)
        }
    }

    private fun invalidConfigError(throwable: Throwable) {
        val msg = KelpBundle.message("invalidConfigNotificationMessage")
        invokeLater {
            NotificationGroupManager.getInstance()
                .getNotificationGroup(KelpBundle.message("configReloadNotificationGroup"))
                .createNotification(
                    title = KelpBundle.message("invalidConfigNotificationTitle"),
                    content = msg,
                    type = NotificationType.ERROR,
                )
                .notify(project)
        }
        invokeLater { throw throwable }
    }

    private fun reloadNotification() {
        val msg = KelpBundle.message("configReloadSuccessMessage")
        invokeLater {
            NotificationGroupManager.getInstance()
                .getNotificationGroup(KelpBundle.message("configReloadNotificationGroup"))
                .createNotification(
                    title = KelpBundle.message("configReloadSuccessTitle"),
                    content = msg,
                    type = NotificationType.INFORMATION,
                )
                .notify(project)
        }
    }

    override fun dispose() = Unit
}

private const val CONFIG_FILE_NAME = "config.json"
