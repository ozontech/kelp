package ru.ozon.ideplugin.kelp.resourceIcons.gutter

// todo use when kotlin k2 support is needed
//import com.android.ide.common.rendering.api.ResourceNamespace
//import com.android.ide.common.rendering.api.ResourceReference
//import com.android.resources.ResourceType
//import org.jetbrains.kotlin.analysis.api.analyze
//import org.jetbrains.kotlin.analysis.api.symbols.KtKotlinPropertySymbol
//import org.jetbrains.kotlin.idea.base.plugin.KotlinPluginModeProvider
//import org.jetbrains.kotlin.idea.references.mainReference
//import org.jetbrains.kotlin.psi.KtNameReferenceExpression
//
//class K2DsIconAnnotator : AndroidKotlinResourceExternalAnnotatorBase() {
//    override fun KtNameReferenceExpression.resolveToResourceReference(): ResourceReference? {
//        if (!KotlinPluginModeProvider.isK2Mode()) return null
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
//    }
//}