package ru.ozon.ideplugin.kelp

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
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import ru.ozon.ideplugin.kelp.codeCompletion.DsColorLookupElement
import ru.ozon.ideplugin.kelp.codeCompletion.DsComponentFunLookupElement
import ru.ozon.ideplugin.kelp.codeCompletion.DsIconLookupElement
import kotlin.io.path.div

/**
 * @return null, if this project does not contain kelp config file, meaning that plugin must be disabled for
 * this project.
 */
fun Project.kelpConfig(): KelpConfig? = serviceOrNull<KelpConfigImpl>()?.data

/**
 * @see README.md
 */
@Serializable
class KelpConfig(
    /** For [DsComponentFunLookupElement] */
    val componentFunHighlighting: ComponentFunHighlighting? = null,

    /** For [DsColorLookupElement] and [DsColorPreviewLineMarkerProviderDescriptor] */
    val colorPreview: ColorPreview? = null,

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
    class ColorPreview(
        val codeCompletionEnabled: Boolean,
        val gutterEnabled: Boolean? = null,
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
        val intentionName: String = KelpBundle.message("openInDemoAppIntentionName"),
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

    private fun reloadConfig() {
        runCatching {
            runReadAction {
                configText = VirtualFileManager.getInstance()
                    .findFileByNioPath(configFilePath)
                    ?.readText()

                data = json.decodeFromString<KelpConfig>(configText ?: return@runReadAction)
            }
        }.onFailure {
            invalidConfigError(it)
        }
    }

    private fun invalidConfigError(throwable: Throwable) {
        val msg = KelpBundle.message("invalidConfigNotificationMessage")
        invokeLater {
            NotificationGroupManager.getInstance()
                .getNotificationGroup(KelpBundle.message("invalidConfigNotificationGroup"))
                .createNotification(
                    title = KelpBundle.message("invalidConfigNotificationTitle"),
                    content = msg,
                    type = NotificationType.ERROR,
                )
                .notify(project)
        }
        invokeLater { throw throwable }
    }

    override fun dispose() = Unit
}

private const val CONFIG_FILE_NAME = "config.json"
