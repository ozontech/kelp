package ru.ozon.ideplugin.kelp

import com.android.ide.common.rendering.api.ResourceNamespace
import com.android.ide.common.rendering.api.ResourceReference
import com.android.resources.ResourceType
import com.android.tools.idea.modes.essentials.EssentialsMode
import com.android.tools.idea.util.androidFacet
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiRecursiveElementWalkingVisitor
import org.jetbrains.android.AndroidResourceExternalAnnotatorBase
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.symbols.KtKotlinPropertySymbol
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.idea.base.plugin.isK2Plugin
import org.jetbrains.kotlin.idea.caches.resolve.resolveToCall
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameOrNull

/**
 * An annotator that places DS icons in gutter when referring to them in Kotlin files.
 * Like with drawable resources.
 *
 * Inspired by [this](https://cs.android.com/search?q=AndroidKotlinResourceExternalAnnotator).
 */
class DsGutterIconAnnotator : AndroidResourceExternalAnnotatorBase() {
    override fun collectInformation(file: PsiFile, editor: Editor): FileAnnotationInfo? {
        if (EssentialsMode.isEnabled()) return null
        val config = file.project.kelpConfig()?.iconsRendering?.takeIf { it.gutterEnabled } ?: return null
        val facet = file.androidFacet ?: return null
        val annotationInfo = FileAnnotationInfo(facet, file, editor)
        file.accept(
            object : PsiRecursiveElementWalkingVisitor() {
                override fun visitElement(element: PsiElement) {
                    super.visitElement(element)

                    val reference = element as? KtNameReferenceExpression ?: return
                    val isTarget = filterDsIconProperty(
                        propertyNameFilter = config.propertyNameFilter,
                        propertyName = reference.text
                    )
                    if (!isTarget) return

                    val resourceReference = if (isK2Plugin()) {
                        reference.resourceReferenceK2(config.containerClassName, config.propertyToResourceMapper)
                    } else {
                        reference.resourceReference(config.containerClassName, config.propertyToResourceMapper)
                    } ?: return
                    annotationInfo.elements.add(FileAnnotationInfo.AnnotatableElement(resourceReference, element))
                }
            },
        )
        return annotationInfo.takeIf { annotationInfo.elements.isNotEmpty() }
    }
}

private fun KtNameReferenceExpression.resourceReference(
    dsIconClassName: String,
    mapper: KelpConfig.IconsRendering.PropertyToResourceMapper?,
): ResourceReference? {
    val descriptor = resolveToCall()?.resultingDescriptor as? PropertyDescriptor ?: return null
    val containingClassName = descriptor.containingDeclaration.fqNameOrNull()?.asString()
    if (containingClassName != dsIconClassName) return null
    val resourceName = getDsIconResourceName(mapper, getReferencedName())
    return ResourceReference(ResourceNamespace.RES_AUTO, ResourceType.DRAWABLE, resourceName)
}

private fun KtNameReferenceExpression.resourceReferenceK2(
    dsIconClassName: String,
    mapper: KelpConfig.IconsRendering.PropertyToResourceMapper?,
): ResourceReference? = analyze(this) {
    val iconProperty = mainReference.resolveToSymbol() as? KtKotlinPropertySymbol ?: return@analyze null
    val containingClassName = iconProperty.callableIdIfNonLocal?.classId?.asFqNameString() ?: return@analyze null
    if (containingClassName != dsIconClassName) return null
    val resourceName = getDsIconResourceName(mapper, getReferencedName())
    return@analyze ResourceReference(ResourceNamespace.RES_AUTO, ResourceType.DRAWABLE, resourceName)
}
