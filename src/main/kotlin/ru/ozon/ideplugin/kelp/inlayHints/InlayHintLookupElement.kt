package ru.ozon.ideplugin.kelp.inlayHints

import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementDecorator
import com.intellij.codeInsight.lookup.LookupElementPresentation
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.KtDeclaration
import ru.ozon.ideplugin.kelp.pluginConfig.kelpConfig

internal class InlayHintLookupElement(
    private val original: LookupElement,
) : LookupElementDecorator<LookupElement>(original) {
    override fun renderElement(presentation: LookupElementPresentation) {
        super.renderElement(presentation)
        val psiElement = original.psiElement as? KtDeclaration ?: return
        val config = psiElement.project.kelpConfig()?.inlayHints
        if (config?.enabled != true) return

        val info = getInlayInfo(psiElement) ?: return

        presentation.tailText = " $info"
    }

    companion object {
        fun appliesTo(psiElement: PsiElement): Boolean {
            val config = psiElement.project.kelpConfig()?.inlayHints?.takeIf { it.enabled } ?: return false
            return isInlayTarget(psiElement, config)
        }
    }
}