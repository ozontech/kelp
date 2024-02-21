package ru.ozon.ideplugin.kelp.codeCompletion

import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementDecorator
import com.intellij.codeInsight.lookup.LookupElementPresentation
import com.intellij.openapi.roots.ProjectRootModificationTracker
import com.intellij.openapi.util.Key
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.CachedValue
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.util.ui.ColorIcon
import com.intellij.util.ui.JBUI
import org.jetbrains.kotlin.psi.KtNamed
import org.jetbrains.kotlin.psi.KtValVarKeywordOwner
import org.jetbrains.uast.*
import ru.ozon.ideplugin.kelp.RoundedColorsIcon
import ru.ozon.ideplugin.kelp.hexToARGB
import ru.ozon.ideplugin.kelp.kelpConfig
import java.awt.Color

/**
 * Adds a color preview in code completion to the fields annotated with [KELP_COLOR_PREVIEW_CLASS_NAME].
 */
internal class DsColorLookupElement(
    private val psiFile: PsiFile,
    private val original: LookupElement,
) : LookupElementDecorator<LookupElement>(original) {
    override fun renderElement(presentation: LookupElementPresentation) {
        super.renderElement(presentation)
        if (psiFile.project.kelpConfig()?.colorPreview?.codeCompletionEnabled != true) return

        val (light, dark) = original.psiElement?.let(::getColorInfo) ?: return

        val scale = JBUI.scale(16)
        val cornerRadius = JBUI.scale(4)
        if (dark == null) {
            val color = Color(hexToARGB(light), true)
            presentation.icon = ColorIcon(scale, scale, scale, scale, color, false, cornerRadius)
            presentation.tailText = " #$light"
        } else {
            val darkColor = Color(hexToARGB(dark), true)
            val lightColor = Color(hexToARGB(light), true)
            presentation.icon = RoundedColorsIcon(scale, cornerRadius, darkColor, lightColor)
            presentation.tailText = " #$light, #$dark"
        }
    }

    companion object {
        fun appliesTo(psiElement: PsiElement): Boolean {
            if (psiElement.project.kelpConfig()?.colorPreview?.codeCompletionEnabled != true) return false
            return psiElement.isColorProperty()
        }
    }
}

internal fun PsiElement.isColorProperty(): Boolean {
    if (this !is KtValVarKeywordOwner) return false
    // long because Color is an inline class
    if (
        toUElementOfType<UMethod>()?.returnType?.canonicalText != "long" &&
        toUElementOfType<UField>()?.type?.canonicalText != "long"
    ) {
        return false
    }
    return getColorInfo(this) != null
}

internal data class ColorInfo(val light: String, val dark: String?)

internal fun getColorInfo(psiElement: PsiElement): ColorInfo? {
    return CachedValuesManager.getCachedValue(psiElement, colorInfoKey) {
        val colorName = (psiElement as? KtNamed)?.nameAsName?.asString()
        val colorInfo: ColorInfo? = psiElement.toUElementOfType<UMethod>()
            ?.getContainingUClass()
            ?.let(::getColorNames)
            ?.takeIf { colorName != null }
            ?.getOrElse(colorName!!) { null }
            ?.let {
                ColorInfo(
                    light = it.substringBefore(' ').uppercase(),
                    dark = it.substringAfter(' ', "").takeIf { it.isNotEmpty() }?.uppercase()
                )
            }

        CachedValueProvider.Result.create(
            colorInfo,
            psiElement.containingFile,
            ProjectRootModificationTracker.getInstance(psiElement.project)
        )
    }
}

private fun getColorNames(uClass: UClass): Map<String, String>? {
    return CachedValuesManager.getCachedValue(uClass, colorNamesKey) {
        val colorNames: Map<String, String>? = uClass.innerClasses
            .find { it.name == KELP_COLOR_PREVIEW_CLASS_NAME }
            ?.fields
            ?.associateBy(
                keySelector = { it.name.substringBefore(' ') },
                valueTransform = { it.name.substringAfter(' ') }
            )

        CachedValueProvider.Result.create(
            colorNames,
            uClass.containingFile,
            ProjectRootModificationTracker.getInstance(uClass.project)
        )
    }
}

private val colorNamesKey =
    Key.create<CachedValue<Map<String, String>?>>("ru.ozon.ideplugin.kelp.DsColorLookupElement.colorNames")
private val colorInfoKey =
    Key.create<CachedValue<ColorInfo?>>("ru.ozon.ideplugin.kelp.DsColorLookupElement.colorInfo")
private const val KELP_COLOR_PREVIEW_CLASS_NAME = "KelpColorPreview"
