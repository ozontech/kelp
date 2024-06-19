package ru.ozon.ideplugin.kelp.pluginConfig

import kotlinx.serialization.Serializable
import ru.ozon.ideplugin.kelp.KelpBundle
import ru.ozon.ideplugin.kelp.OpenDsComponentInDemoAppIntention
import ru.ozon.ideplugin.kelp.codeCompletion.DsComponentFunLookupElement
import ru.ozon.ideplugin.kelp.colorPreviews.DsColorLookupElement
import ru.ozon.ideplugin.kelp.colorPreviews.DsColorPreviewLineMarkerProviderDescriptor
import ru.ozon.ideplugin.kelp.liveTemplates.AddLiveTemplates
import ru.ozon.ideplugin.kelp.resourceIcons.DsIconLookupElement
import ru.ozon.ideplugin.kelp.resourceIcons.gutter.K1DsIconAnnotator

/**
 * @see README.md
 */
@Serializable
class KelpConfig(
    /** For [DsComponentFunLookupElement] */
    val componentFunHighlighting: ComponentFunHighlighting? = null,

    /** For [DsColorLookupElement] and [DsColorPreviewLineMarkerProviderDescriptor] */
    val colorPreview: ColorPreview? = null,

    /** For [DsIconLookupElement] and [K1DsIconAnnotator] */
    val iconsRendering: IconsRendering? = null,

    /** For [OpenDsComponentInDemoAppIntention] */
    val demoApp: DemoApp? = null,

    /** For [AddLiveTemplates] */
    val liveTemplates: List<LiveTemplate>? = null,
) {
    @Serializable
    class ComponentFunHighlighting(
        val functionFqnPrefix: String,
        val functionSimpleNamePrefix: String? = null,
    )
    @Serializable
    class ColorPreview(
        val codeCompletionEnabled: Boolean,
        val gutterEnabled: Boolean? = null,
        val enumColorTokensEnabled: Boolean? = null,
    )

    @Serializable
    class IconsRendering(
        val codeCompletionEnabled: Boolean,
        val gutterEnabled: Boolean,
        val containerClassName: String,
        val propertyNameFilter: IconPropertyNameFilter? = null,
        val propertyToResourceMapper: PropertyToResourceMapper? = null,
    ) {
        @Serializable
        class IconPropertyNameFilter(
            val startsWith: Set<String>? = null,
            val doesNotStartWith: Set<String>? = null,
        )

        @Serializable
        class PropertyToResourceMapper(
            val addPrefix: String? = null,
            val convertToSnakeCase: Boolean = false,
        )
    }

    @Serializable
    class DemoApp(
        val functionFqnPrefix: String,
        val functionSimpleNamePrefix: String? = null,
        val appPackageName: String,
        val componentDeeplink: String,
        val intentionName: String = KelpBundle.message("openInDemoAppIntentionName"),
        val apkInstallation: Boolean? = null,
        val apkDownloadGradleCommand: String? = null,
    )

    @Serializable
    class LiveTemplate(
        val abbreviation: String,
        val text: String,
        val description: String? = null,
        val reformat: Boolean = false,
        val shortenFQNames: Boolean = true,
        val variables: List<Variable> = emptyList(),
        val context: List<String> = listOf("KOTLIN_EXPRESSION", "KOTLIN_STATEMENT"),
    ) {
        @Serializable
        class Variable(
            val name: String,
            val expression: String = "",
            val defaultValue: String = "",
            val alwaysStopAt: Boolean = true,
        )
    }
}
