package ru.ozon.ideplugin.kelp

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.AsyncFileListener.ChangeApplier
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.readText
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import ru.ozon.ideplugin.kelp.codeCompletion.DsComponentFunLookupElement
import ru.ozon.ideplugin.kelp.codeCompletion.DsIconLookupElement
import kotlin.io.path.div

/**
 * @return null, if this project does not contain kelp config file, meaning that plugin must be disabled for
 * this project.
 */
fun Project.kelpConfig(): KelpConfig? = service<KelpConfigImpl>().takeIf { it.exists }?.data

/**
 * See [README.md]
 */
@Serializable
class KelpConfig(
    /** For [DsComponentFunLookupElement] */
    val componentFunHighlighting: ComponentFunHighlighting? = null,

    /** For [DsIconLookupElement] and [DsGutterIconAnnotator] */
    val iconsRendering: IconsRendering? = null,

    /** For [OpenDsComponentInDemoAppIntention] */
    val demoApp: DemoApp? = null,
) {
    @Serializable
    class ComponentFunHighlighting(
        val functionFqnPrefix: String,
        val functionSimpleNamePrefix: String? = null,
    )

    @Serializable
    class IconsRendering(
        val codeCompletionEnabled: Boolean,
        val gutterEnabled: Boolean,
        val containerClassName: String,
        val propertyNameFilter: IconPropertyNameFilter? = null,
        val propertyToResourceMapper: PropertyToResourceMapper? = null,
    ) {
        @Serializable
        class IconPropertyNameFilter(
            val startsWith: Set<String>? = null,
            val doesNotStartWith: Set<String>? = null,
        )

        @Serializable
        class PropertyToResourceMapper(
            val addPrefix: String? = null,
            val convertToSnakeCase: Boolean = false,
        )
    }

    @Serializable
    class DemoApp(
        val functionFqnPrefix: String,
        val functionSimpleNamePrefix: String? = null,
        val appPackageName: String,
        val componentDeeplink: String,
        val intentionName: String = "🚀 Open in design system demo app",
        val apkInstalling: ApkInstalling? = null,
    ) {
        @Serializable
        class ApkInstalling(val latestVersion: LatestVersion) {
            @Serializable
            class LatestVersion(
                val file: String,
                val regex: String,
            )
        }
    }
}

/**
 * To retrieve this service, you MUST use [kelpConfig].
 */
@Service(Service.Level.PROJECT)
private class KelpConfigImpl(private val project: Project) : Disposable {
    private val json = Json { ignoreUnknownKeys = true }
    private val configFilePath = pluginConfigDirPath(project) / CONFIG_FILE_NAME

    private var configText: String? = null
    var exists: Boolean = false
    var data: KelpConfig? = null

    init {
        reloadConfig()
        VirtualFileManager.getInstance().addAsyncFileListener({ events ->
            if (!events.any { it.path == configFilePath.toString() }) return@addAsyncFileListener null

            object : ChangeApplier {
                override fun afterVfsChange() = reloadConfig()
            }
        }, this)
    }

    private fun reloadConfig() = runReadAction {
        configText = VirtualFileManager.getInstance()
                .findFileByNioPath(configFilePath)
                ?.readText()

        exists = !configText.isNullOrBlank()

        data = run {
            val configText = configText

            checkNotNull(configText) {
                val name = KelpConfigImpl::class.java.simpleName
                "Kelp config file wasn't found. Please, use kelpConfig() function to retrieve $name service"
            }

            runCatching {
                json.decodeFromString<KelpConfig>(configText)
            }.onFailure {
                invalidConfigError(it)
            }.getOrNull()
        }
    }

    private fun invalidConfigError(throwable: Throwable) {
        val msg = "Config file isn't valid. Please, follow plugin setup instructions to create a valid config file."
        invokeLater {
            NotificationGroupManager.getInstance().getNotificationGroup("KelpConfigError")
                .createNotification(title = "Kelp", content = msg, type = NotificationType.ERROR)
                .notify(project)
        }
        invokeLater { throw throwable }
    }

    override fun dispose() = Unit
}

private const val CONFIG_FILE_NAME = "config.json"
