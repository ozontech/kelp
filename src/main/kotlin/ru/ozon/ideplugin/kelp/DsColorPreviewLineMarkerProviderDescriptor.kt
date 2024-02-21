package ru.ozon.ideplugin.kelp

import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProviderDescriptor
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.psi.PsiElement
import com.intellij.util.ui.ColorIcon
import com.intellij.util.ui.JBUI
import org.jetbrains.kotlin.idea.editor.fixers.range
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtValVarKeywordOwner
import org.toml.lang.psi.ext.elementType
import ru.ozon.ideplugin.kelp.codeCompletion.getColorInfo
import ru.ozon.ideplugin.kelp.codeCompletion.isColorProperty
import java.awt.Color

/** [LineMarkerProviderDescriptor] that adds a gutter icon on @Composable function invocations. */
class DsColorPreviewLineMarkerProviderDescriptor : LineMarkerProviderDescriptor() {

    override fun getName() = KelpBundle.message("colorPreviewDescriptorName")

    override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<*>? {
        val colorPreviewDisabled = element.project.kelpConfig()?.colorPreview?.gutterEnabled != true
        val wrongElementType = element.elementType != KtTokens.IDENTIFIER
        if (colorPreviewDisabled || wrongElementType || !element.isColorPropertyUsage()) return null

        val parent = element.parent
        val (light, dark) = when (parent) {
            // declaration, e.g. class MyColors { val primary: Color }
            is KtValVarKeywordOwner -> parent.let(::getColorInfo)
            // usage, e.g. myColors.primary
            is KtNameReferenceExpression -> parent.reference?.resolve()?.let(::getColorInfo)
            else -> return null
        } ?: return null

        val scale = JBUI.scale(10)
        val cornerRadius = 0
        val tooltipText: String
        val icon = if (dark == null) {
            val color = Color(hexToARGB(light), true)
            tooltipText = KelpBundle.message("colorPreviewDescriptorTooltip", light)
            ColorIcon(scale, scale, scale, scale, color, false, cornerRadius)
        } else {
            val darkColor = Color(hexToARGB(dark), true)
            val lightColor = Color(hexToARGB(light), true)
            tooltipText = KelpBundle.message("colorPreviewDescriptorLightDarkTooltip", light, dark)
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

    private fun PsiElement.isColorPropertyUsage(): Boolean = isColorPropertyCall() || isColorPropertyDeclaration()

    private fun PsiElement.isColorPropertyCall() =
        parent is KtNameReferenceExpression && parent.reference?.resolve()?.isColorProperty() == true

    private fun PsiElement.isColorPropertyDeclaration() =
        parent is KtValVarKeywordOwner && parent?.isColorProperty() == true
}