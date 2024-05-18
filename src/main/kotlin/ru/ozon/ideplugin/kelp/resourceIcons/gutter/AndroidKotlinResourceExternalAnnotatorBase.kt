package ru.ozon.ideplugin.kelp.resourceIcons.gutter

import com.android.ide.common.rendering.api.ResourceReference
import com.android.tools.idea.util.androidFacet
import com.intellij.ide.EssentialHighlightingMode
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiRecursiveElementWalkingVisitor
import org.jetbrains.android.AndroidResourceExternalAnnotatorBase
import org.jetbrains.kotlin.psi.KtNameReferenceExpression

/**
 * Annotator which puts colors and image icons in the editor gutter when referenced in Kotlin files.
 *
 * Copied from [here](https://cs.android.com/search?q=AndroidKotlinResourceExternalAnnotatorBase)
 */
abstract class AndroidKotlinResourceExternalAnnotatorBase : AndroidResourceExternalAnnotatorBase() {
    override fun collectInformation(file: PsiFile, editor: Editor): FileAnnotationInfo? {
        if (EssentialHighlightingMode.isEnabled()) return null
        val facet = file.androidFacet ?: return null
        val annotationInfo = FileAnnotationInfo(facet, file, editor)
        file.accept(object : PsiRecursiveElementWalkingVisitor() {
            override fun visitElement(element: PsiElement) {
                super.visitElement(element)
                val reference = element as? KtNameReferenceExpression ?: return
                val resourceReference = reference.resolveToResourceReference() ?: return
                annotationInfo.elements.add(FileAnnotationInfo.AnnotatableElement(resourceReference, element))
            }
        })
        return annotationInfo.takeIf { annotationInfo.elements.isNotEmpty() }
    }

    abstract fun KtNameReferenceExpression.resolveToResourceReference(): ResourceReference?
}