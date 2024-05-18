package ru.ozon.ideplugin.kelp.colorPreviews

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
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.containingClass
import org.jetbrains.uast.*
import ru.ozon.ideplugin.kelp.pluginConfig.KelpConfig
import ru.ozon.ideplugin.kelp.pluginConfig.kelpConfig
import java.awt.Color

/**
 * Adds a color preview in code completion to the properties declared
 * in a class that has an inner class named [KELP_COLOR_PREVIEW_CLASS_NAME].
 *
 * @see README.md
 */
internal class DsColorLookupElement(
    private val psiFile: PsiFile,
    private val original: LookupElement,
) : LookupElementDecorator<LookupElement>(original) {
    override fun renderElement(presentation: LookupElementPresentation) {
        super.renderElement(presentation)
        if (psiFile.project.kelpConfig()?.colorPreview?.codeCompletionEnabled != true) return

        val (light, dark) = original.psiElement?.let(::getColorInfo) ?: return

        val scale = JBUI.scale(12)
        val cornerRadius = JBUI.scale(4)
        if (dark == null) {
            val color = Color(hexToARGB(light), true)
            presentation.icon = ColorIcon(scale, scale, scale, scale, color, false, cornerRadius)
            presentation.tailText = " #$light"
        } else {
            val darkColor = Color(hexToARGB(dark), true)
            val lightColor = Color(hexToARGB(light), true)
            presentation.icon = RoundedColorsIcon(scale, cornerRadius, lightColor, darkColor)
            presentation.tailText = " #$light, #$dark"
        }
    }

    companion object {
        fun appliesTo(psiElement: PsiElement): Boolean {
            val config = psiElement.project.kelpConfig()?.colorPreview
            if (config?.codeCompletionEnabled != true) return false
            return psiElement.isColorProperty(config)
        }
    }
}

internal fun PsiElement.isColorProperty(config: KelpConfig.ColorPreview): Boolean {
    if (this !is KtEnumEntry && this !is KtProperty && this !is KtParameter) return false
    if (config.enumColorTokensEnabled != true && this is KtEnumEntry) return false
    if (this is KtCallableDeclaration && typeReference?.text?.endsWith("Color") != true) return false
    return getColorInfo(this) != null
}

internal data class ColorInfo(val light: String, val dark: String?)

internal fun getColorInfo(psiElement: PsiElement): ColorInfo? {
    return CachedValuesManager.getCachedValue(psiElement, colorInfoKey) {
        val modificationTracker = ProjectRootModificationTracker.getInstance(psiElement.project)

        val colorName = (psiElement as? KtNamed)?.nameAsName?.asString()
            ?: return@getCachedValue CachedValueProvider.Result.create(
                null,
                listOfNotNull(psiElement.containingFile, modificationTracker)
            )

        val colorInfo: ColorInfo? =
            (psiElement as? KtDeclaration)
                ?.containingClass()
                ?.toUElementOfType<UClass>()
                ?.let(::getColorNames)
                ?.getOrElse(colorName) { null }
                ?.let {
                    val light = it.substringBefore('_').uppercase()
                    ColorInfo(
                        light = light,
                        // if same as light, skip dark
                        dark = it.substringAfter('_', "").takeIf { it != light }?.uppercase()
                    )
                }

        CachedValueProvider.Result.create(
            colorInfo,
            listOfNotNull(psiElement.containingFile, modificationTracker)
        )
    }
}

/**
 * Caches the "colorName to colorValues" pairs for each containing class,
 * e.g. for each declaration like this:
 *
 * ```kotlin
 * class MyColors(
 *     val primary: Color,
 *     val secondary: Color,
 * ) {
 *     private class KelpColorPreview {
 *         val `primary_FFD0BCFF_FF6650A4` = Unit
 *         val `secondary_12CCC2DC_FF625B71` = Unit
 *     }
 * }
 * ```
 *
 * cached value will be:
 * ```kotlin
 * MyColors.toUClass() to mapOf(
 *     "primary" to "FFD0BCFF_FF6650A4",
 *     "secondary" to "12CCC2DC_FF625B71",
 * )
 * ```
 */
private fun getColorNames(uClass: UClass): Map<String, String>? {
    return CachedValuesManager.getCachedValue(uClass, colorNamesKey) {
        val colorNames: Map<String, String>? = uClass.innerClasses
            .find { it.name == KELP_COLOR_PREVIEW_CLASS_NAME }
            ?.fields
            ?.associateBy(
                keySelector = { it.name.dropLast(HEX_COLORS_POSTFIX_LENGTH) },
                valueTransform = { it.name.takeLast(HEX_COLORS_POSTFIX_LENGTH - 1) } // -1 is the first underscore
            )

        CachedValueProvider.Result.create(
            colorNames, uClass.containingFile, ProjectRootModificationTracker.getInstance(uClass.project)
        )
    }
}

private val HEX_COLORS_POSTFIX_LENGTH = "_AARRGGBB_AARRGGBB".length
private val colorNamesKey =
    Key.create<CachedValue<Map<String, String>?>>("ru.ozon.ideplugin.kelp.DsColorLookupElement.colorNames")
private val colorInfoKey =
    Key.create<CachedValue<ColorInfo?>>("ru.ozon.ideplugin.kelp.DsColorLookupElement.colorInfo")
private const val KELP_COLOR_PREVIEW_CLASS_NAME = "KelpColorPreview"
