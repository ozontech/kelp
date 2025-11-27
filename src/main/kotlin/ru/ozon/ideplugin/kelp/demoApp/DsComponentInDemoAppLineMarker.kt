package ru.ozon.ideplugin.kelp.demoApp

import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProviderDescriptor
import com.intellij.icons.AllIcons
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.psi.PsiElement
import com.intellij.psi.util.elementType
import com.intellij.ui.LayeredIcon
import com.intellij.util.IconUtil
import com.intellij.util.ui.EmptyIcon
import org.jetbrains.kotlin.idea.base.codeInsight.handlers.fixers.range
import org.jetbrains.kotlin.idea.codeinsight.utils.findExistingEditor
import org.jetbrains.kotlin.lexer.KtTokens
import ru.ozon.ideplugin.kelp.KelpBundle
import ru.ozon.ideplugin.kelp.KelpIcons
import ru.ozon.ideplugin.kelp.isDsComponentFunction
import ru.ozon.ideplugin.kelp.pluginConfig.kelpConfig
import javax.swing.Icon
import javax.swing.SwingConstants

/** [LineMarkerProviderDescriptor] that adds a gutter icon on DS function declarations to open them in the demo app. */
class DsComponentInDemoAppLineMarker : LineMarkerProviderDescriptor() {

    override fun getName() = KelpBundle.message("openInDemoAppGutterIconName")
    override fun getIcon(): Icon = AllIcons.RunConfigurations.TestState.Run

    override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<*>? {
        val kelpConfig = element.project.kelpConfig()
        val demoAppConfig = kelpConfig?.demoApp ?: return null
        val wrongElementType = element.elementType != KtTokens.IDENTIFIER
        if (wrongElementType || element.parent?.isDsComponentFunction(demoAppConfig.functionFilters) != true) {
            return null
        }

        val icon = if (kelpConfig.componentFunHighlighting != null) {
            LayeredIcon(3).apply {
                val funIcon = KelpIcons.getDsComponentFunIcon(element.project)
                    .let { IconUtil.scale(it, null, 0.8f) }
                val runIcon = AllIcons.RunConfigurations.TestState.Run
                    .let { IconUtil.scale(it, null, 0.6f) }
                    .let { IconUtil.brighter(it, 2) }

                setIcon(EmptyIcon.ICON_16, 0)
                setIcon(funIcon, 1, SwingConstants.NORTH_WEST)
                setIcon(runIcon, 2, SwingConstants.SOUTH_EAST)
            }
        } else {
            AllIcons.RunConfigurations.TestState.Run
        }

        return LineMarkerInfo<PsiElement>(
            /* element = */ element,
            /* range = */ element.range,
            /* icon = */ icon,
            /* tooltipProvider = */ { demoAppConfig.intentionName },
            /* navHandler = */
            navHandler@{ _, psiElement ->
                OpenDsComponentInDemoAppIntention().invoke(
                    project = psiElement.project,
                    editor = psiElement.findExistingEditor() ?: return@navHandler,
                    element = psiElement
                )
            },
            /* alignment = */ GutterIconRenderer.Alignment.RIGHT,
            /* accessibleNameProvider = */ { demoAppConfig.intentionName },
        )
    }
}