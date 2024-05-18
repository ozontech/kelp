package ru.ozon.ideplugin.kelp.colorPreviews

import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProviderDescriptor
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.psi.PsiElement
import com.intellij.util.ui.ColorIcon
import com.intellij.util.ui.JBUI
import org.jetbrains.kotlin.idea.base.codeInsight.handlers.fixers.range
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.toml.lang.psi.ext.elementType
import ru.ozon.ideplugin.kelp.KelpBundle
import ru.ozon.ideplugin.kelp.pluginConfig.KelpConfig
import ru.ozon.ideplugin.kelp.pluginConfig.kelpConfig
import java.awt.Color

/** [LineMarkerProviderDescriptor] that adds a gutter icon on DS color references. */
class DsColorPreviewLineMarkerProviderDescriptor : LineMarkerProviderDescriptor() {

    override fun getName() = KelpBundle.message("colorPreviewDescriptorName")

    override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<*>? {
        val config = element.project.kelpConfig()?.colorPreview ?: return null
        val colorPreviewDisabled = config.gutterEnabled != true
        val wrongElementType = element.elementType != KtTokens.IDENTIFIER
        if (colorPreviewDisabled || wrongElementType || !element.isColorPropertyUsage(config)) return null

        val parent = element.parent
        val (light, dark) = parent.reference?.resolve()?.let(::getColorInfo) ?: return null

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

    private fun PsiElement.isColorPropertyUsage(config: KelpConfig.ColorPreview): Boolean =
        parent is KtNameReferenceExpression && parent.reference?.resolve()?.isColorProperty(config) == true
}