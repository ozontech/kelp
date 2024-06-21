package ru.ozon.ideplugin.kelp.codeCompletion

import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementDecorator
import com.intellij.codeInsight.lookup.LookupElementPresentation
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.IconLoader
import com.intellij.psi.PsiElement
import com.intellij.util.system.OS
import org.jetbrains.kotlin.psi.KtNamedFunction
import ru.ozon.ideplugin.kelp.isDsComponentFunction
import ru.ozon.ideplugin.kelp.pluginConfig.kelpConfig
import ru.ozon.ideplugin.kelp.pluginConfigDirPath
import javax.swing.Icon
import kotlin.io.path.absolutePathString
import kotlin.io.path.div

/**
 * Changes the icon for composable functions of DS components to make them stand out more.
 */
internal class DsComponentFunLookupElement(original: LookupElement) : LookupElementDecorator<LookupElement>(original) {

    override fun getPsiElement(): KtNamedFunction = super.getPsiElement() as KtNamedFunction

    override fun renderElement(presentation: LookupElementPresentation) {
        super.renderElement(presentation)

        presentation.icon = getDsComponentFunIcon(psiElement.project)
    }

    companion object {
        fun appliesTo(psiElement: PsiElement): Boolean {
            val config = psiElement.project.kelpConfig()?.componentFunHighlighting ?: return false
            return psiElement.isDsComponentFunction(config)
        }
    }
}

internal fun getDsComponentFunIcon(project: Project): Icon {
    val path = (pluginConfigDirPath(project) / "dsComponentFunIcon.svg").absolutePathString()
    val prefix = if (OS.CURRENT == OS.Windows) "file:/" else "file://"
    return IconLoader.getIcon(prefix + path, DsComponentFunLookupElement::class.java)
}
