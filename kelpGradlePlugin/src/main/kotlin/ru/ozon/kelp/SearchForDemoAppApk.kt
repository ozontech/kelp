package ru.ozon.kelp

import org.gradle.api.logging.Logger
import java.io.File

private val apkFileNameRegex = "demoApp-(?<version>.+?).apk".toRegex()

private fun badFileNameError(apkFolder: File, file: File, logger: Logger): Nothing {
    logger.error(
        "$apkFolder contains a file (${file.name}) whose name does not conform to this regex: $apkFileNameRegex"
    )
    throwException()
}

internal fun searchForDemoAppApk(apkDir: File, requiredDemoApkVersion: String, logger: Logger): ApkSearchResult {
    val (apkFile, actualApkVersion) = apkDir.listFiles()
        .orEmpty()
        .mapNotNull { file ->
            val matchResult = apkFileNameRegex.matchEntire(file.name) ?: badFileNameError(apkDir, file, logger)
            val version = matchResult.groups["version"]?.value ?: badFileNameError(apkDir, file, logger)
            file to version
        }
        .maxByOrNull { it.second }
        ?: return ApkSearchResult.Absent

    return when {
        actualApkVersion != requiredDemoApkVersion -> ApkSearchResult.VersionMismatch(
            required = requiredDemoApkVersion,
            actual = actualApkVersion,
        )

        else -> ApkSearchResult.Success
    }
}

internal sealed class ApkSearchResult {
    object Success : ApkSearchResult()
    object Absent : ApkSearchResult()
    data class VersionMismatch(val required: String, val actual: String) : ApkSearchResult()
}