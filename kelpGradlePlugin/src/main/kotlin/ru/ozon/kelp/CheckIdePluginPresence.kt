package ru.ozon.kelp

import org.gradle.api.logging.Logger
import ru.ozon.kelp.IdePluginAbsenceBehaviour.*
import java.io.File

/**
 * @return true when check is passed
 */
internal fun checkIdePluginPresence(
    kelpDir: File,
    idePluginAbsenceBehaviour: IdePluginAbsenceBehaviour,
    logger: Logger,
): Boolean {
    val installedVersion = kelpDir.resolve("pluginIsPresent")
        .takeIf { it.exists() }
        ?.readText()

    if (installedVersion == null) {
        when (idePluginAbsenceBehaviour) {
            NOTHING -> Unit
            WARNING -> logger.error("error: $noIdePluginMsg")
            BUILD_FAIL -> error(noIdePluginMsg)
        }
    }
    return idePluginAbsenceBehaviour == NOTHING || installedVersion != null
}

private const val idePluginInstallUrl = "https://github.com/ozontech/kelp?tab=readme-ov-file#-installation"
private const val noIdePluginMsg = "⚠️ Kelp IDE plugin is not installed. To install: $idePluginInstallUrl"
