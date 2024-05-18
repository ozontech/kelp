package ru.ozon.ideplugin.kelp.codeCompletion

import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResult
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.psi.PsiFile
import ru.ozon.ideplugin.kelp.colorPreviews.DsColorLookupElement
import ru.ozon.ideplugin.kelp.isInComposeEnabledModuleAndFile
import ru.ozon.ideplugin.kelp.resourceIcons.DsIconLookupElement

/**
 * Changes [LookupElement]s for parts of the design system to improve the UX of using the DS.
 *
 * Inspired by [this](https://cs.android.com/search?q=ComposeCompletionContributor).
 */
internal class CompletionContributor : CompletionContributor() {
    override fun fillCompletionVariants(parameters: CompletionParameters, resultSet: CompletionResultSet) {
        if (!parameters.isInComposeEnabledModuleAndFile()) return

        resultSet.runRemainingContributors(parameters) { completionResult ->
            transformCompletionResult(parameters.position.containingFile, completionResult).let(resultSet::passResult)
        }
    }

    private fun transformCompletionResult(psiFile: PsiFile, completionResult: CompletionResult): CompletionResult {
        val lookupElement = completionResult.lookupElement
        val psi = lookupElement.psiElement ?: return completionResult

        val newLookupElement = when {
            DsComponentFunLookupElement.appliesTo(psi) -> DsComponentFunLookupElement(lookupElement)
            DsIconLookupElement.appliesTo(psi) -> DsIconLookupElement(psiFile, lookupElement)
            DsColorLookupElement.appliesTo(psi) -> DsColorLookupElement(psiFile, lookupElement)
            else -> return completionResult
        }

        return completionResult.withLookupElement(newLookupElement)
    }
}