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
import ru.ozon.ideplugin.kelp.codeCompletion.getColorAnnotation
import ru.ozon.ideplugin.kelp.codeCompletion.getPreviewColorText
import ru.ozon.ideplugin.kelp.codeCompletion.isColorProperty
import java.awt.Color
import javax.swing.Icon

/** [LineMarkerProviderDescriptor] that adds a gutter icon on @Composable function invocations. */
class DsColorPreviewLineMarkerProviderDescriptor : LineMarkerProviderDescriptor() {

    override fun getName() = KelpBundle.message("colorPreviewDescriptorName")

    override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<*>? {
        if (
            element.elementType != KtTokens.IDENTIFIER ||
            element.project.kelpConfig()?.colorPreview?.gutterEnabled != true ||
            !element.isColorPropertyCall()
        ) {
            return null
        }

        val valueArguments = element.parent.reference?.resolve()?.getColorAnnotation()?.valueArguments

        val lightColorText = valueArguments?.getPreviewColorText("light") ?: return null
        val darkColorText = valueArguments.getPreviewColorText("dark")

        val scale = JBUI.scale(16)
        val cornerRadius = JBUI.scale(4)
        val icon: Icon
        if (darkColorText == null) {
            val color = Color(hexToARGB(lightColorText), true)
            icon = ColorIcon(scale, scale, scale, scale, color, false, cornerRadius)
        } else {
            val darkColor = Color(hexToARGB(darkColorText), true)
            val lightColor = Color(hexToARGB(lightColorText), true)
            icon = RoundedColorsIcon(scale, cornerRadius, darkColor, lightColor)
        }

        return LineMarkerInfo<PsiElement>(
            /* element = */ element,
            /* range = */ element.range,
            /* icon = */ icon,
            /* tooltipProvider = */ { KelpBundle.message("colorPreviewDescriptorTooltip", lightColorText) },
            /* navHandler = */ null,
            /* alignment = */ GutterIconRenderer.Alignment.RIGHT,
            /* accessibleNameProvider = */ { KelpBundle.message("colorPreviewDescriptorTooltip", lightColorText) },
        )
    }

    private fun PsiElement.isColorPropertyCall(): Boolean {
        return parent is KtNameReferenceExpression && parent.reference?.resolve()?.isColorProperty() == true
    }
}