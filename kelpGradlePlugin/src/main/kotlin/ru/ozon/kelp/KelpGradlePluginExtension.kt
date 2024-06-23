package ru.ozon.kelp

import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.gradle.kotlin.dsl.withType
import ru.ozon.kelp.downloaders.ApkDownloader

public interface KelpGradlePluginExtension {
    /**
     * What to do when Kelp plugin is not installed or the version doesn't equal the [requiredIdePluginVersion]
     *
     * [IdePluginAbsenceBehaviour.WARNING] by default
     */
    public val idePluginAbsenceBehaviour: Property<IdePluginAbsenceBehaviour>

    /** Optional */
    public val requiredIdePluginVersion: Property<String>

    /**
     * Optional: version of the demo app apk. If apk is absent or present, but filename
     * doesn't have the [requiredDemoApkVersion], new apk is downloaded with the [requiredDemoApkVersion].
     */
    public val requiredDemoApkVersion: Property<String>

    /**
     * Optional: responsible for downloading the demo app apk.
     *
     * @see ru.ozon.kelp.downloaders.SimpleApkDownloader
     * @see ru.ozon.kelp.downloaders.BrowserApkDownloader
     */
    public fun Project.setApkDownloader(apkDownloader: ApkDownloader) {
        tasks.withType<CheckDemoAppApk> {
            setApkDownloader(apkDownloader)
        }
    }
}

public enum class IdePluginAbsenceBehaviour {
    NOTHING, WARNING, BUILD_FAIL
}

