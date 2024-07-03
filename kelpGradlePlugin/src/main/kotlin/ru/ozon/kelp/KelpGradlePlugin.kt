package ru.ozon.kelp

import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Internal
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.register
import ru.ozon.kelp.IdePluginAbsenceBehaviour.WARNING
import ru.ozon.kelp.downloaders.ApkDownloader
import java.io.File

private var idePluginPresenceCheckPassed = false

public class KelpGradlePlugin : Plugin<Project> {

    override fun apply(project: Project) {
        val isCI = System.getenv().containsKey("CI")

        val extension = project.extensions.create<KelpGradlePluginExtension>("kelp")
        extension.idePluginAbsenceBehaviour.convention(WARNING)
        val kelpDir = project.rootDir.resolve(".idea").resolve("kelp")
        val apkDir = kelpDir.resolve("apk").apply { mkdirs() }

        val checkIdePluginPresence = project.tasks.register("kelpCheckIdePluginPresence") {
            group = "kelp"
            description = "Notify if Kelp IDE plugin is absent or has an incorrect version"
            enabled = !isCI

            inputs.dir(kelpDir)
            inputs.property("idePluginAbsenceBehaviour", extension.idePluginAbsenceBehaviour).optional(true)
            inputs.property("requiredIdePluginVersion", extension.requiredIdePluginVersion).optional(true)
            outputs.upToDateWhen { idePluginPresenceCheckPassed }

            doFirst {
                idePluginPresenceCheckPassed = checkIdePluginPresence(
                    kelpDir = kelpDir,
                    requiredVersion = extension.requiredIdePluginVersion.getOrNull(),
                    idePluginAbsenceBehaviour = extension.idePluginAbsenceBehaviour.get(),
                    logger = logger,
                )
            }
        }

        val checkDemoAppApk = project.tasks.register<CheckDemoAppApk>("kelpCheckDemoAppApk") {
            dependsOn(checkIdePluginPresence)

            group = "kelp"
            description = "Check presence and version of the design system demo app apk. Download one if needed"
            enabled = !isCI

            inputs.dir(apkDir)
            inputs.property("requiredDemoApkVersion", extension.requiredDemoApkVersion).optional(true)
            outputs.dir(apkDir)

            doFirst {
                val requiredDemoApkVersion = extension.requiredDemoApkVersion.getOrNull() ?: return@doFirst
                validateInputs(requiredDemoApkVersion)
                val result = searchForDemoAppApk(apkDir, requiredDemoApkVersion, logger)
                if (result is ApkSearchResult.Success) return@doFirst
                with(apkDir) {
                    deleteRecursively()
                    mkdirs()
                }
                /** prepare data for the second task from [CheckDemoAppApk.setApkDownloader] */
                @Suppress("RedundantRequireNotNullCall")
                apply {
                    apkSearchResult = requireNotNull(result)
                    apkVersionToDownload = requireNotNull(requiredDemoApkVersion)
                    outputDir = requireNotNull(apkDir)
                }
            }
        }

        project.tasks.named("preBuild") {
            dependsOn(checkIdePluginPresence, checkDemoAppApk)
        }
        return
    }

    private fun Task.validateInputs(requiredDemoApkVersion: String) {
        val requiredDemoApkVersionName = { KelpGradlePluginExtension::requiredDemoApkVersion.name }
        val apkDownloaderName = { ApkDownloader::class.java.name }
        require(requiredDemoApkVersion.isNotBlank()) { "$requiredDemoApkVersionName should not be blank" }
        requireNotNull(actions.size == 2) {
            "If ${requiredDemoApkVersionName()} is provided, ${apkDownloaderName()} should also be provided"
        }
    }
}

@CacheableTask
internal abstract class CheckDemoAppApk : DefaultTask() {
    @get:Internal
    var apkSearchResult: ApkSearchResult? = null

    @get:Internal
    var apkVersionToDownload: String? = null

    @get:Internal
    var outputDir: File? = null

    fun setApkDownloader(apkDownloader: ApkDownloader) {
        if (actions.size > 1) actions.subList(1, actions.size).clear()
        doLast {
            when (val result = apkSearchResult) {
                ApkSearchResult.Success, null -> return@doLast
                ApkSearchResult.Absent -> logger.warn("Demo app apk is absent")
                is ApkSearchResult.VersionMismatch -> logger.warn(
                    "Demo app apk has an incorrect version. Actual: ${result.actual}. Required: ${result.required}"
                )
            }
            logger.warn("Downloading the demo app apk...")
            apkDownloader.download(
                version = apkVersionToDownload!!,
                destinationDir = outputDir!!,
                fileName = "demoApp-${apkVersionToDownload!!}.apk",
                logger = logger,
            )

            outputDir = null
            apkVersionToDownload = null
            apkSearchResult = null
        }
    }
}
