package ru.ozon.ideplugin.kelp.codeCompletion

import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementDecorator
import com.intellij.codeInsight.lookup.LookupElementPresentation
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.KtNamedFunction
import ru.ozon.ideplugin.kelp.KelpIcons
import ru.ozon.ideplugin.kelp.isDsComponentFunction
import ru.ozon.ideplugin.kelp.pluginConfig.kelpConfig

/**
 * Changes the icon for composable functions of DS components to make them stand out more.
 */
internal class DsComponentFunLookupElement(original: LookupElement) : LookupElementDecorator<LookupElement>(original) {

    override fun getPsiElement(): KtNamedFunction = super.getPsiElement() as KtNamedFunction

    override fun renderElement(presentation: LookupElementPresentation) {
        super.renderElement(presentation)

        presentation.icon = KelpIcons.getDsComponentFunIcon(psiElement.project)
    }

    companion object {
        fun appliesTo(psiElement: PsiElement): Boolean {
            val config = psiElement.project.kelpConfig()?.componentFunHighlighting?.takeIf { it.isNotEmpty() }
                ?: return false
            return psiElement.isDsComponentFunction(config)
        }
    }
}
