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
import org.jetbrains.kotlin.analysis.api.KtAllowAnalysisOnEdt
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.lifetime.allowAnalysisOnEdt
import org.jetbrains.kotlin.idea.base.plugin.isK2Plugin
import org.jetbrains.kotlin.psi.KtAnnotated
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtValVarKeywordOwner
import org.jetbrains.kotlin.psi.ValueArgument
import ru.ozon.ideplugin.kelp.RoundedColorsIcon
import ru.ozon.ideplugin.kelp.hexToARGB
import ru.ozon.ideplugin.kelp.kelpConfig
import java.awt.Color

/**
 * Adds a color preview in code completion to the fields annotated with [KELP_COLOR_PREVIEW_ANNOTATION_NAME].
 */
internal class DsColorLookupElement(
    private val psiFile: PsiFile,
    private val original: LookupElement,
) : LookupElementDecorator<LookupElement>(original) {
    override fun renderElement(presentation: LookupElementPresentation) {
        super.renderElement(presentation)
        psiFile.project.kelpConfig()?.colorPreview?.codeCompletionEnabled ?: return

        val valueArguments = original.psiElement?.getColorAnnotation()?.valueArguments

        val lightColorText = valueArguments?.getPreviewColorText(LIGHT_COLOR_ANN_PARAM_NAME) ?: return
        val darkColorText = valueArguments.getPreviewColorText(DARK_COLOR_ANN_PARAM_NAME)

        val scale = JBUI.scale(16)
        val cornerRadius = JBUI.scale(4)
        if (darkColorText == null) {
            val color = Color(hexToARGB(lightColorText), true)
            presentation.icon = ColorIcon(scale, scale, scale, scale, color, false, cornerRadius)
            presentation.tailText = " #$lightColorText"
        } else {
            val darkColor = Color(hexToARGB(darkColorText), true)
            val lightColor = Color(hexToARGB(lightColorText), true)
            presentation.icon = RoundedColorsIcon(scale, cornerRadius, darkColor, lightColor)
            presentation.tailText = " #$lightColorText, #$darkColorText"
        }
    }

    companion object {
        const val LIGHT_COLOR_ANN_PARAM_NAME = "light"
        const val DARK_COLOR_ANN_PARAM_NAME = "dark"

        fun appliesTo(psiElement: PsiElement): Boolean {
            psiElement.project.kelpConfig()?.colorPreview?.codeCompletionEnabled ?: return false
            return psiElement.isColorProperty()
        }
    }
}

private const val KELP_COLOR_PREVIEW_ANNOTATION_NAME = "KelpColorPreview"

private val colorPropertyKey = Key.create<CachedValue<KtAnnotationEntry?>>(
    "ru.ozon.ideplugin.kelp.Utils.isColorProperty"
)

internal fun List<ValueArgument>.getPreviewColorText(paramName: String) =
    find { it.getArgumentName()?.asName?.asString() == paramName }
        ?.getArgumentExpression()
        ?.text
        ?.removeSurrounding("\"")
        ?.uppercase()

internal fun PsiElement.isColorProperty(): Boolean {
    if (this !is KtValVarKeywordOwner) return false
    return getColorAnnotation() != null
}

// caching logic borrowed from here: https://cs.android.com/android-studio/platform/tools/adt/idea/+/mirror-goog-studio-main:intellij.android.compose-common/src/com/android/tools/compose/PsiUtils.kt;drc=17cc50009e0e510b721a790d8319ab2bf43ff74d;l=72

@OptIn(KtAllowAnalysisOnEdt::class)
internal fun PsiElement.getColorAnnotation(): KtAnnotationEntry? {
    if (this !is KtAnnotated) return null
    return if (isK2Plugin()) {
        getAnnotationWithCaching(colorPropertyKey) { annotationEntry ->
            allowAnalysisOnEdt { analyze(annotationEntry) { annotationEntry.isColorPreviewAnnotation() } }
        }
    } else {
        getAnnotationWithCaching(colorPropertyKey) { it.isColorPreviewAnnotation() }
    }
}

private fun PsiElement.isColorPreviewAnnotation(): Boolean =
    (this as? KtAnnotationEntry)?.shortName?.asString() == KELP_COLOR_PREVIEW_ANNOTATION_NAME

private fun KtAnnotated.getAnnotationWithCaching(
    key: Key<CachedValue<KtAnnotationEntry?>>,
    doCheck: (KtAnnotationEntry) -> Boolean
): KtAnnotationEntry? {
    return CachedValuesManager.getCachedValue(this, key) {
        val annotationEntry = annotationEntries.firstOrNull { doCheck(it) }
        val containingKtFile = this.containingKtFile

        CachedValueProvider.Result.create(
            annotationEntry,
            containingKtFile,
            ProjectRootModificationTracker.getInstance(project)
        )
    }
}