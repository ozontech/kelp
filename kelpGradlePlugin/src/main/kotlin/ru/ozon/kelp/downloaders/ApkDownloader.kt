package ru.ozon.kelp.downloaders

import org.gradle.api.logging.Logger
import org.gradle.kotlin.dsl.version
import java.io.File

/**
 * Responsible for downloading the demo app apk.
 */
public fun interface ApkDownloader {
    /**
     * @param version of the demo app apk that is needed
     * @param destinationDir into which to put the apk file
     * @param fileName the apk must have
     * @param logger to print errors
     */
    public fun download(version: String, destinationDir: File, fileName: String, logger: Logger)
}

/**
 * Provides a url for downloading an apk of the [version].
 */
public fun interface ApkUrlProvider {
    public fun provide(version: String): String
}