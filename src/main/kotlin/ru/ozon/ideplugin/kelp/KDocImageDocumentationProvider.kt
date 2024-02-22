package ru.ozon.ideplugin.kelp

import com.intellij.lang.documentation.DocumentationProvider
import com.intellij.psi.PsiDocCommentBase
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.idea.KotlinDocumentationProvider

/**
 * Fixes [this issue](https://youtrack.jetbrains.com/issue/KTIJ-13687/KDoc-support-inline-images).
 *
 * @see README.md
 */
internal class KDocImageDocumentationProvider(
    private val delegate: DocumentationProvider = KotlinDocumentationProvider()
) : DocumentationProvider by delegate {

    private val kdocImageRegex =
        "!<a href=['\"](?<url>.*?)['\"]>((?<width>\\d+)?x(?<height>\\d+)?)?(?<alt>.*?)</a>".toRegex()

    override fun generateRenderedDoc(comment: PsiDocCommentBase): String? =
        delegate.generateRenderedDoc(comment)?.let(::renderKdocImages)

    override fun generateDoc(element: PsiElement?, originalElement: PsiElement?): String? =
        delegate.generateDoc(element, originalElement)?.let(::renderKdocImages)

    private fun renderKdocImages(kotlinDoc: String): String {
        val replace = kotlinDoc.replace(kdocImageRegex) {
            val url = it.groups["url"]?.value ?: return@replace it.value
            val width = it.groups["width"]?.value?.toIntOrNull()
            val height = it.groups["height"]?.value?.toIntOrNull()
            val alt = it.groups["alt"]?.value

            val buildString = buildString {
                append("""<img src="$url" """)
                if (alt != null) append("alt=\"$alt\" ")
                if (width != null) append("width=\"$width\" ")
                if (height != null) append("height=\"$height\" ")
                append(">")
            }
            buildString
        }
        return replace
    }
}
