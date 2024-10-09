package ru.ozon.ideplugin.kelp.resourceIcons.gutter

import com.android.ide.common.rendering.api.ResourceNamespace
import com.android.ide.common.rendering.api.ResourceReference
import com.android.resources.ResourceType
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.symbols.KaKotlinPropertySymbol
import org.jetbrains.kotlin.idea.base.plugin.KotlinPluginModeProvider
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import ru.ozon.ideplugin.kelp.pluginConfig.kelpConfig
import ru.ozon.ideplugin.kelp.resourceIcons.filterDsIconProperty
import ru.ozon.ideplugin.kelp.resourceIcons.getDsIconResourceName

class K2DsIconAnnotator : AndroidKotlinResourceExternalAnnotatorBase() {
    override fun collectInformation(file: PsiFile, editor: Editor): FileAnnotationInfo? =
        if (KotlinPluginModeProvider.isK2Mode()) {
            super.collectInformation(file, editor)
        } else {
            null
        }

    override fun KtNameReferenceExpression.resolveToResourceReference(): ResourceReference? = analyze(this) {
        val config = project.kelpConfig()?.iconsRendering?.takeIf { it.gutterEnabled } ?: return null
        val isTarget = filterDsIconProperty(
            propertyNameFilter = config.propertyNameFilter,
            propertyName = text
        )
        if (!isTarget) return null

        val iconProperty = mainReference.resolveToSymbol() as? KaKotlinPropertySymbol ?: return null
        val containingClassName = iconProperty.callableId?.classId?.asFqNameString() ?: return@analyze null
        if (containingClassName != config.containerClassName) return null
        val resourceName = getDsIconResourceName(config.propertyToResourceMapper, getReferencedName())
        return@analyze ResourceReference(ResourceNamespace.RES_AUTO, ResourceType.DRAWABLE, resourceName)
    }
}