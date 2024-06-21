package ru.ozon.ideplugin.kelp.resourceIcons.gutter

import com.android.ide.common.rendering.api.ResourceReference
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.idea.base.plugin.KotlinPluginModeProvider
import org.jetbrains.kotlin.psi.KtNameReferenceExpression

// todo use when K2 migration guides for plugins will be published
class K2DsIconAnnotator : AndroidKotlinResourceExternalAnnotatorBase() {
    override fun collectInformation(file: PsiFile, editor: Editor): FileAnnotationInfo? =
        if (KotlinPluginModeProvider.isK2Mode()) {
            super.collectInformation(file, editor)
        } else {
            null
        }

    override fun KtNameReferenceExpression.resolveToResourceReference(): ResourceReference? {
        return null
//        return analyze(this) {
//            val config = project.kelpConfig()?.iconsRendering?.takeIf { it.gutterEnabled } ?: return null
//            val isTarget = filterDsIconProperty(
//                propertyNameFilter = config.propertyNameFilter,
//                propertyName = text
//            )
//            if (!isTarget) return null
//
//            val iconProperty = mainReference.resolveToSymbol() as? KtKotlinPropertySymbol ?: return null
//            val containingClassName =
//                iconProperty.callableIdIfNonLocal?.classId?.asFqNameString() ?: return@analyze null
//            if (containingClassName != config.containerClassName) return null
//            val resourceName = getDsIconResourceName(config.propertyToResourceMapper, getReferencedName())
//            return@analyze ResourceReference(ResourceNamespace.RES_AUTO, ResourceType.DRAWABLE, resourceName)
//        }
    }
}