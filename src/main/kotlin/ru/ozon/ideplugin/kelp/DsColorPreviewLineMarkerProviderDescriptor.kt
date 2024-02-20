package ru.ozon.ideplugin.kelp

import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProviderDescriptor
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.psi.PsiElement
import com.intellij.psi.util.elementType
import com.intellij.util.ui.ColorIcon
import com.intellij.util.ui.JBUI
import org.jetbrains.kotlin.idea.editor.fixers.range
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import ru.ozon.ideplugin.kelp.codeCompletion.DsColorLookupElement.Companion.DARK_COLOR_ANN_PARAM_NAME
import ru.ozon.ideplugin.kelp.codeCompletion.DsColorLookupElement.Companion.LIGHT_COLOR_ANN_PARAM_NAME
import ru.ozon.ideplugin.kelp.codeCompletion.getColorAnnotation
import ru.ozon.ideplugin.kelp.codeCompletion.getPreviewColorText
import ru.ozon.ideplugin.kelp.codeCompletion.isColorProperty
import java.awt.Color

/** [LineMarkerProviderDescriptor] that adds a gutter icon on @Composable function invocations. */
class DsColorPreviewLineMarkerProviderDescriptor : LineMarkerProviderDescriptor() {

    override fun getName() = KelpBundle.message("colorPreviewDescriptorName")

    override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<*>? {
        val notIdentifier = element.elementType != KtTokens.IDENTIFIER
        val colorPreviewDisabled = element.project.kelpConfig()?.colorPreview?.gutterEnabled != true
        if (notIdentifier || colorPreviewDisabled || !element.isColorPropertyCall()) return null

        val valueArguments = element.parent.reference?.resolve()?.getColorAnnotation()?.valueArguments

        val lightColorText = valueArguments?.getPreviewColorText(LIGHT_COLOR_ANN_PARAM_NAME) ?: return null
        val darkColorText = valueArguments.getPreviewColorText(DARK_COLOR_ANN_PARAM_NAME)

        val scale = JBUI.scale(10)
        val cornerRadius = 0
        val tooltipText: String
        val icon = if (darkColorText == null) {
            val color = Color(hexToARGB(lightColorText), true)
            tooltipText = KelpBundle.message("colorPreviewDescriptorTooltip", lightColorText)
            ColorIcon(scale, scale, scale, scale, color, false, cornerRadius)
        } else {
            val darkColor = Color(hexToARGB(darkColorText), true)
            val lightColor = Color(hexToARGB(lightColorText), true)
            tooltipText = KelpBundle.message("colorPreviewDescriptorLightDarkTooltip", lightColorText, darkColorText)
            RoundedColorsIcon(scale, cornerRadius, darkColor, lightColor)
        }

        return LineMarkerInfo<PsiElement>(
            /* element = */ element,
            /* range = */ element.range,
            /* icon = */ icon,
            /* tooltipProvider = */ { tooltipText },
            /* navHandler = */ null,
            /* alignment = */ GutterIconRenderer.Alignment.RIGHT,
            /* accessibleNameProvider = */ { tooltipText },
        )
    }

    private fun PsiElement.isColorPropertyCall(): Boolean {
        return parent is KtNameReferenceExpression && parent.reference?.resolve()?.isColorProperty() == true
    }
}