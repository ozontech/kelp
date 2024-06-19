package ru.ozon.kelp

import org.gradle.api.logging.Logger
import ru.ozon.kelp.IdePluginAbsenceBehaviour.*
import java.io.File

/**
 * @return true when check is passed
 */
internal fun checkIdePluginPresence(
    kelpDir: File,
    requiredVersion: String?,
    idePluginAbsenceBehaviour: IdePluginAbsenceBehaviour,
    logger: Logger,
): Boolean {
    val installedVersion = kelpDir.resolve("pluginIsPresent")
        .takeIf { it.exists() }
        ?.readText()

    val msg = when {
        // if null — plugin is not installed
        installedVersion == null -> noIdePluginMsg
        // version mismatch
        requiredVersion != null && requiredVersion != installedVersion ->
            wrongIdePluginVersionMsg(currentVersion = installedVersion, requiredVersion = requiredVersion)

        else -> null
    }
    if (msg != null) when (idePluginAbsenceBehaviour) {
        NOTHING -> Unit
        WARNING -> logger.error(msg)
        BUILD_FAIL -> error(msg)
    }
    return idePluginAbsenceBehaviour == NOTHING || msg == null
}

private const val idePluginInstallUrl = "https://github.com/ozontech/kelp?tab=readme-ov-file#installation"
private const val noIdePluginMsg = "⚠️ Kelp IDE plugin is not installed or outdated. To install: $idePluginInstallUrl"

private fun wrongIdePluginVersionMsg(currentVersion: String, requiredVersion: String) =
    "⚠️ Kelp IDE plugin version ($currentVersion) doesn't match the required ($requiredVersion). " +
            "Install the required one: $idePluginInstallUrl"
