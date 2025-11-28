package ru.ozon.ideplugin.kelp.inlayHints

import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementDecorator
import com.intellij.codeInsight.lookup.LookupElementPresentation
import com.intellij.psi.PsiElement
import org.jetbrains.annotations.Unmodifiable
import org.jetbrains.kotlin.psi.KtDeclaration
import ru.ozon.ideplugin.kelp.pluginConfig.kelpConfig
import java.util.*

internal class InlayHintLookupElement(
    private val original: LookupElement,
) : LookupElementDecorator<LookupElement>(original) {

    private val info: String? = run {
        val psiElement = original.psiElement as? KtDeclaration ?: return@run null
        val config = psiElement.project.kelpConfig()?.inlayHints
        if (config?.enabled != true) return@run null

        getInlayInfo(psiElement) ?: return@run null
    }

    override fun renderElement(presentation: LookupElementPresentation) {
        super.renderElement(presentation)

        if (info != null) presentation.tailText = " $info"
    }

    override fun getAllLookupStrings(): @Unmodifiable Set<String?>? {
        val base = super.getAllLookupStrings()
        if (info.isNullOrBlank()) return base
        return Collections.unmodifiableSet(base.toMutableSet().apply{ add(info) })
    }

    companion object {
        fun appliesTo(psiElement: PsiElement): Boolean {
            val config = psiElement.project.kelpConfig()?.inlayHints?.takeIf { it.enabled } ?: return false
            return isInlayTarget(psiElement, config)
        }
    }
}