package ru.ozon.ideplugin.kelp.resourceIcons.gutter

import com.android.ide.common.rendering.api.ResourceNamespace
import com.android.ide.common.rendering.api.ResourceReference
import com.android.resources.ResourceType
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.idea.base.plugin.KotlinPluginModeProvider
import org.jetbrains.kotlin.idea.caches.resolve.resolveToCall
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameOrNull
import ru.ozon.ideplugin.kelp.pluginConfig.kelpConfig
import ru.ozon.ideplugin.kelp.resourceIcons.filterDsIconProperty
import ru.ozon.ideplugin.kelp.resourceIcons.getDsIconResourceName

class K1DsIconAnnotator : AndroidKotlinResourceExternalAnnotatorBase() {
    override fun KtNameReferenceExpression.resolveToResourceReference(): ResourceReference? {
        if (!KotlinPluginModeProvider.isK1Mode()) return null
        val config = project.kelpConfig()?.iconsRendering?.takeIf { it.gutterEnabled } ?: return null
        val isTarget = filterDsIconProperty(
            propertyNameFilter = config.propertyNameFilter,
            propertyName = text
        )
        if (!isTarget) return null

        val referenceTarget = resolveToCall()?.resultingDescriptor as? PropertyDescriptor
        val containingClassName = referenceTarget?.containingDeclaration?.fqNameOrNull()?.asString()
        if (containingClassName != config.containerClassName) return null
        val resourceName = getDsIconResourceName(config.propertyToResourceMapper, getReferencedName())
        return ResourceReference(ResourceNamespace.RES_AUTO, ResourceType.DRAWABLE, resourceName)
    }
}

