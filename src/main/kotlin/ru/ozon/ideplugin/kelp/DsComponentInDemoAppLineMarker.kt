package ru.ozon.ideplugin.kelp

import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProviderDescriptor
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.psi.PsiElement
import icons.StudioIcons
import org.jetbrains.kotlin.idea.base.codeInsight.handlers.fixers.range
import org.jetbrains.kotlin.idea.codeinsight.utils.findExistingEditor
import org.jetbrains.kotlin.lexer.KtTokens
import org.toml.lang.psi.ext.elementType
import ru.ozon.ideplugin.kelp.pluginConfig.kelpConfig
import javax.swing.Icon

/** [LineMarkerProviderDescriptor] that adds a gutter icon on DS function declarations to open them in the demo app. */
class DsComponentInDemoAppLineMarker : LineMarkerProviderDescriptor() {

    override fun getName() = KelpBundle.message("openInDemoAppGutterIconName")
    override fun getIcon(): Icon = StudioIcons.Compose.Toolbar.RUN_ON_DEVICE

    override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<*>? {
        val config = element.project.kelpConfig()?.demoApp ?: return null
        val wrongElementType = element.elementType != KtTokens.IDENTIFIER
        if (wrongElementType || element.parent?.isDsComponentFunction(config) != true) return null

        return LineMarkerInfo<PsiElement>(
            /* element = */ element,
            /* range = */ element.range,
            /* icon = */ StudioIcons.Compose.Toolbar.RUN_ON_DEVICE,
            /* tooltipProvider = */ { config.intentionName },
            /* navHandler = */
            navHandler@{ _, psiElement ->
                OpenDsComponentInDemoAppIntention().invoke(
                    project = psiElement.project,
                    editor = psiElement.findExistingEditor() ?: return@navHandler,
                    element = psiElement
                )
            },
            /* alignment = */ GutterIconRenderer.Alignment.RIGHT,
            /* accessibleNameProvider = */ { config.intentionName },
        )
    }
}