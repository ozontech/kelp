package ru.ozon.ideplugin.kelp.inlayHints

import com.intellij.codeInsight.hints.declarative.*
import com.intellij.codeInsight.hints.declarative.InlayHintsProvider
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.roots.ProjectRootModificationTracker
import com.intellij.openapi.util.Key
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.CachedValue
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.elementType
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.containingClass
import org.jetbrains.uast.UClass
import org.jetbrains.uast.toUElementOfType
import ru.ozon.ideplugin.kelp.isInComposeEnabledModuleAndFile
import ru.ozon.ideplugin.kelp.pluginConfig.KelpConfig
import ru.ozon.ideplugin.kelp.pluginConfig.kelpConfig

class InlayHintsProvider : InlayHintsProvider {

    override fun createCollector(file: PsiFile, editor: Editor): InlayHintsCollector? {
        if (!file.isInComposeEnabledModuleAndFile()) return null
        return Collector()
    }

    private class Collector : SharedBypassCollector {
        override fun collectFromElement(element: PsiElement, sink: InlayTreeSink) {
            val config = element.project.kelpConfig()?.inlayHints
            if (config?.enabled != true || !isInlayTarget(element, config)) return
            val declaration = element.getInlayDeclarationFromUsage() ?: return
            val info = getInlayInfo(declaration) ?: return

            sink.addPresentation(
                position = InlineInlayPosition(element.textRange.endOffset, true),
                hintFormat = HintFormat.default
            ) {
                text(info)
            }
        }
    }
}

internal fun PsiElement.getInlayDeclarationFromUsage(): KtDeclaration? = parent?.reference?.resolve() as? KtDeclaration

internal fun getInlayInfo(declaration: KtDeclaration): String? {
    return CachedValuesManager.getCachedValue(declaration, inlayInfoKey) {
        val propertyName = (declaration as? KtNamed)?.nameAsName?.asString() ?: return@getCachedValue null

        val containingClass = (declaration as? KtDeclaration)
            ?.containingClass()
            ?.toUElementOfType<UClass>()

        val inlayInfo: String = containingClass
            ?.let(::getInlaysInfo)
            ?.getOrElse(propertyName) { null }
            ?: return@getCachedValue null

        CachedValueProvider.Result.create(
            /* value = */ inlayInfo,
            /* ...dependencies = */
            declaration.containingFile,
            ProjectRootModificationTracker.getInstance(declaration.project)
        )
    }
}

internal fun isInlayTarget(element: PsiElement, config: KelpConfig.InlayHints): Boolean {
    val parent = if (element.elementType == KtTokens.IDENTIFIER && element.parent is KtNameReferenceExpression) {
        element.getInlayDeclarationFromUsage() ?: return false
    } else {
        element as? KtDeclaration ?: return false
    }

    if (!config.enums && parent is KtEnumEntry) return false
    if (parent is KtNamed && (parent is KtEnumEntry || parent is KtProperty || parent is KtParameter)) {
        return getInlayInfo(parent) != null
    }
    return false
}

/**
 * Caches the "propertyName to inlayDisplayString" pairs for each containing class,
 * e.g. for each declaration like this:
 *
 * ```kotlin
 * class MyPaddings(
 *     val medium: Dp,
 *     val large: Dp,
 * ) {
 *     private class KelpInlayPreview {
 *         val `medium___8.dp` = Unit
 *         val `large___16.dp` = Unit
 *     }
 * }
 * ```
 *
 * cached value will be:
 * ```kotlin
 * MyPaddings.toUClass() to mapOf(
 *     "medium" to "8.dp",
 *     "large" to "16.dp",
 * )
 * ```
 */
private fun getInlaysInfo(uClass: UClass): Map<String, String>? {
    val classPsi = uClass.javaPsi
    return CachedValuesManager.getCachedValue(classPsi, inlaysInfoKey) {
        val inlaysInfo: Map<String, String> = uClass.innerClasses
            .find { it.name == KELP_INLAY_PREVIEW_CLASS_NAME }
            ?.fields
            ?.takeIf { it.any { it.name.contains(KELP_INLAY_PREVIEW_SEPARATOR) } }
            ?.associateBy(
                keySelector = { it.name.substringBefore(KELP_INLAY_PREVIEW_SEPARATOR) },
                valueTransform = { it.name.substringAfter(KELP_INLAY_PREVIEW_SEPARATOR) }
            )
            ?: return@getCachedValue null

        CachedValueProvider.Result.create(
            /* value = */ inlaysInfo,
            /* ...dependencies = */
            classPsi.containingFile,
            ProjectRootModificationTracker.getInstance(classPsi.project)
        )
    }
}

private val inlaysInfoKey = Key.create<CachedValue<Map<String, String>>>(
    "ru.ozon.ideplugin.kelp.inlayHints.StaticInlayHintsProvider.inlaysInfo"
)
private val inlayInfoKey =
    Key.create<CachedValue<String>>("ru.ozon.ideplugin.kelp.inlayHints.StaticInlayHintsProvider.inlayInfo")
private const val KELP_INLAY_PREVIEW_CLASS_NAME = "KelpInlayPreview"
private const val KELP_INLAY_PREVIEW_SEPARATOR = "___"