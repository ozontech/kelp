package ru.ozon.kelp.downloaders

import org.gradle.api.logging.Logger
import ru.ozon.kelp.throwException
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.nio.channels.Channels

/**
 * Downloads an apk from the web using the provided url.
 *
 * @param additionalErrorMsg error message to display when downloading failed, e.g. "Turn on the corporate VPN"
 * @param onError invoked if an error occurred during downloading.
 */
public class SimpleApkDownloader(
    private val additionalErrorMsg: String? = null,
    private val onError: ((Throwable) -> Unit)? = null,
    private val urlProvider: ApkUrlProvider,
) : ApkDownloader {
    override fun download(version: String, destinationDir: File, fileName: String, logger: Logger) {
        val urlString = try {
            urlProvider.provide(version)
        } catch (throwable: Throwable) {
            logger.error("Failed to acquire the download url for the design system demo app apk", throwable)
            onError?.invoke(throwable)
            throwException()
        }
        try {
            val apkFile = destinationDir.resolve(fileName)
            Channels.newChannel(URL(urlString).openStream()).use { readableByteChannel ->
                FileOutputStream(apkFile).use { fileStream ->
                    fileStream.channel.transferFrom(readableByteChannel, 0, Long.MAX_VALUE)
                }
            }
        } catch (throwable: Throwable) {
            logger.error(
                buildString {
                    appendLine("Failed to download the demo app apk file from this url: $urlString")
                    additionalErrorMsg?.let(::appendLine)
                },
                throwable,
            )
            onError?.invoke(throwable)
            throwException()
        }
    }
}