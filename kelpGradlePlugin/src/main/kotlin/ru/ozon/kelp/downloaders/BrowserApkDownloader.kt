package ru.ozon.kelp.downloaders

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.gradle.api.logging.Logger
import ru.ozon.kelp.throwException
import java.awt.Desktop
import java.io.File
import java.net.URI
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

/**
 * Can be used when an apk cannot be simply downloaded from a url by [SimpleApkDownloader].
 * For example, logging in is required or UI interaction is necessary.
 *
 * 1. Opens the provided url in the browser
 * 2. Asks the user to download an apk
 * 3. Listens for the appearance of the new ".apk" file in the downloads folder
 * 4. Copies it to the [destinationDir] with the [fileName].
 *
 * @property customDownloadsDir overrides the default downloads dir where ".apk" is awaited to appear
 */
public open class BrowserApkDownloader(
    private val customDownloadsDir: File? = null,
    private val urlProvider: ApkUrlProvider,
) : ApkDownloader {
    override fun download(version: String, destinationDir: File, fileName: String, logger: Logger) {
        val url = try {
            urlProvider.provide(version)
        } catch (throwable: Throwable) {
            logger.error("Failed to acquire the download url for the design system demo app apk", throwable)
            throwException()
        }
        val downloads = customDownloadsDir ?: File(System.getProperty("user.home")).run {
            resolve("Download").takeIf { it.exists() } ?: resolve("Downloads").takeIf { it.exists() }
        }
        val apkFile = destinationDir.resolve(fileName)
        if (downloads != null) {
            openUrl(url)
            logger.warn(
                """
                    |🔗 Link to apk opened in 🌐 web browser (if not, manually open $url).
                    |Please, ⬇️ download the apk, version: $version.
                    |
                    |If after successful download this message still appears,
                    |manually place the apk into $apkFile and re-run Gradle.
                    |
                    |⏳ Waiting for .apk to appear in $downloads ...
                    |""".trimMargin(),
            )
            val apk = runBlocking { waitForApkToAppear(downloads) }
            if (apk != null) {
                apk.copyTo(apkFile)
                logger.warn("🎉 Apk found!")
            } else {
                logger.error("🔍 Apk was not found. Please, follow the previous instructions.")
                throwException()
            }
        } else {
            openUrl(url)
            logger.error(
                """
                    |🔗 Link to apk opened in 🌐 web browser (if not, manually open $url).
                    |Please, ⬇️ download the apk, version: $version.
                    |
                    |Then, manually place the apk into $apkFile and re-run Gradle.
                    |""".trimMargin(),
            )
            throwException()
        }
    }

    /**
     * Listens for the appearance of the new ".apk" file in the [downloadsDir]
     */
    protected suspend fun waitForApkToAppear(downloadsDir: File, timeout: Duration = 2.minutes): File? {
        val filesSnapshot = downloadsDir.list().orEmpty().toSet()
        var apk: File? = null
        withTimeout(timeout) {
            while (apk == null) {
                delay(500)
                // compare old and new snapshots to find new downloaded apk file
                apk = downloadsDir.list().orEmpty().toSet()
                    .minus(filesSnapshot)
                    .find { it.substringAfterLast('.') == "apk" }
                    ?.let { downloadsDir.resolve(it) }
            }
        }
        return apk
    }

    /**
     * Opens [url] in the default browser
     */
    protected fun openUrl(url: String) {
        val os = System.getProperty("os.name").lowercase()
        val command = when {
            Desktop.isDesktopSupported() -> {
                Desktop.getDesktop().browse(URI(url))
                return
            }

            os.contains("win") -> "rundll32 url.dll,FileProtocolHandler $url"
            os.contains("mac") -> "open $url"
            os.contains("nix") || os.contains("nix") -> "xdg-open $url"
            else -> return
        }
        Runtime.getRuntime().exec(command)
    }
}