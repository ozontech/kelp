package ru.ozon.ideplugin.kelp.resourceIcons

import com.android.ide.common.rendering.api.ResourceNamespace
import com.android.resources.ResourceType
import com.android.tools.idea.rendering.GutterIconCache
import com.android.tools.idea.res.StudioResourceRepositoryManager
import com.android.tools.idea.res.getSourceAsVirtualFile
import com.android.tools.idea.util.androidFacet
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementDecorator
import com.intellij.codeInsight.lookup.LookupElementPresentation
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import org.jetbrains.android.AndroidAnnotatorUtil
import org.jetbrains.kotlin.idea.base.psi.kotlinFqName
import org.jetbrains.kotlin.idea.base.util.module
import org.jetbrains.kotlin.psi.KtProperty
import ru.ozon.ideplugin.kelp.pluginConfig.kelpConfig

/**
 * Adds a DS icon in code completion to the fields of the `dsIconClassName` class.
 */
internal class DsIconLookupElement(
    private val psiFile: PsiFile,
    private val original: LookupElement,
) : LookupElementDecorator<LookupElement>(original) {
    override fun renderElement(presentation: LookupElementPresentation) {
        super.renderElement(presentation)

        val module = psiFile.module ?: return
        val facet = module.androidFacet ?: return
        val config = psiFile.project.kelpConfig()?.iconsRendering ?: return

        val dsIconPropertyName = (original.psiElement as? KtProperty)?.name ?: return
        val isTarget = filterDsIconProperty(
            propertyNameFilter = config.propertyNameFilter,
            propertyName = dsIconPropertyName
        )
        if (!isTarget) return

        val resourceName = getDsIconResourceName(config.propertyToResourceMapper, dsIconPropertyName)

        val file = StudioResourceRepositoryManager.getInstance(module)
            ?.appResources
            ?.getResources(ResourceNamespace.RES_AUTO, ResourceType.DRAWABLE, resourceName)
            ?.firstNotNullOfOrNull { it.getSourceAsVirtualFile() }
            ?: return
        val resolver = AndroidAnnotatorUtil.pickConfiguration(psiFile.originalFile, facet)?.resourceResolver ?: return
        presentation.icon = GutterIconCache.getInstance(psiFile.project).getIcon(file, resolver, facet)
    }

    companion object {
        fun appliesTo(psiElement: PsiElement): Boolean {
            val config = psiElement.project.kelpConfig()?.iconsRendering
                ?.takeIf { it.codeCompletionEnabled } ?: return false
            val dsIconClass = config.containerClassName
            return psiElement is KtProperty &&
                    psiElement.kotlinFqName?.asString()?.startsWith(dsIconClass) == true
        }
    }
}