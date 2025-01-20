package ru.ozon.ideplugin.kelp.pluginConfig

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
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
private class KelpConfigService(val project: Project) : Disposable {
    private val json = Json { ignoreUnknownKeys = true }
    private val configFilePath = pluginConfigDirPath(project) / CONFIG_FILE_NAME
    private val application = ApplicationManager.getApplication()

    private var configText: String? = null
    var data: KelpConfig? = null

    init {
        reloadConfig(isFirstRun = true)
        VirtualFileManager.getInstance().addAsyncFileListener({ events ->
            if (!events.any { it.path == configFilePath.toString() }) return@addAsyncFileListener null

            object : ChangeApplier {
                override fun afterVfsChange() = reloadConfig(isFirstRun = false)
            }
        }, this as Disposable)
    }

    private fun reloadConfig(isFirstRun: Boolean) {
        application.executeOnPooledThread {
            application.runReadAction {
                runCatching {
                    val previousConfig = data
                    configText = VirtualFileManager.getInstance()
                        .findFileByNioPath(configFilePath)
                        ?.readText()

                    val kelpConfig = json.decodeFromString<KelpConfig>(configText ?: return@runReadAction)
                    data = kelpConfig
                    AddLiveTemplates.execute(kelpConfig, project.name)
                    if (!isFirstRun) reloadNotification()
                    project.service<GrazieProNotification>().notifyIfNeeded(previousConfig, kelpConfig)
                }.onFailure {
                    invalidConfigError(it)
                }
            }
        }
    }

    private fun invalidConfigError(throwable: Throwable) {
        invokeLater {
            NotificationGroupManager.getInstance()
                .getNotificationGroup(KelpBundle.message("configReloadNotificationGroup"))
                .createNotification(
                    title = KelpBundle.message("invalidConfigNotificationTitle"),
                    content = buildString {
                        appendLine(KelpBundle.message("invalidConfigNotificationMessage"))
                        appendLine(throwable.localizedMessage)
                        appendLine(throwable.stackTraceToString())
                    },
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

internal const val CONFIG_FILE_NAME = "config.json"
