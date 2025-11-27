@file:UseSerializers(RegexSerializer::class)

package ru.ozon.ideplugin.kelp.pluginConfig

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.UseSerializers
import ru.ozon.ideplugin.kelp.KelpBundle
import ru.ozon.ideplugin.kelp.codeCompletion.DsComponentFunLookupElement
import ru.ozon.ideplugin.kelp.colorPreviews.DsColorLookupElement
import ru.ozon.ideplugin.kelp.colorPreviews.DsColorPreviewLineMarker
import ru.ozon.ideplugin.kelp.demoApp.OpenDsComponentInDemoAppIntention
import ru.ozon.ideplugin.kelp.demojet.DemoJetIntentionAction
import ru.ozon.ideplugin.kelp.inlayHints.InlayHintsProvider
import ru.ozon.ideplugin.kelp.liveTemplates.AddLiveTemplates
import ru.ozon.ideplugin.kelp.resourceIcons.DsIconLookupElement
import ru.ozon.ideplugin.kelp.resourceIcons.gutter.K1DsIconAnnotator
import ru.ozon.ideplugin.kelp.resourceIcons.gutter.K2DsIconAnnotator

/**
 * @see README.md
 */
@Serializable
class KelpConfig(
    /** For [DsComponentFunLookupElement] */
    @Serializable(with = WrappingFunctionFilterSerializer::class)
    val componentFunHighlighting: List<FunctionFilter>? = null,

    /** For [DsColorLookupElement] and [DsColorPreviewLineMarker] */
    val colorPreview: ColorPreview? = null,

    /** For [InlayHintsProvider] */
    val inlayHints: InlayHints? = null,

    /** For [DsIconLookupElement], [K1DsIconAnnotator] and [K2DsIconAnnotator] */
    @Serializable(with = WrappingIconsRenderingSerializer::class)
    val iconsRendering: List<IconsRendering>? = null,

    /** For [OpenDsComponentInDemoAppIntention] */
    val demoApp: DemoApp? = null,

    /** For [AddLiveTemplates] */
    val liveTemplates: List<LiveTemplate>? = null,

    /** For [DemoJetIntentionAction] */
    @Serializable(with = WrappingDemoJetSerializer::class)
    val demoJetDemosGeneration: List<DemoJetStubGeneration>? = null,
) {
    init {
        requireListNotEmpty(list = componentFunHighlighting, paramName = ::componentFunHighlighting.name)
        requireListNotEmpty(list = iconsRendering, paramName = ::iconsRendering.name)
        requireListNotEmpty(list = demoJetDemosGeneration, paramName = ::demoJetDemosGeneration.name)
    }

    @Serializable
    class FunctionFilter(
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
        private val functionFqnPrefix: String? = null,
        private val functionSimpleNamePrefix: String? = null,
        @SerialName("functionFilters")
        private val _functionFilters: List<FunctionFilter>? = null,
        val appPackageName: String,
        val componentDeeplink: String,
        val intentionName: String = KelpBundle.message("openInDemoAppIntentionName"),
        val apkInstallation: Boolean? = null,
        val apkDownloadGradleCommand: String? = null,
    ) {
        init {
            require(!((functionFqnPrefix == null || functionSimpleNamePrefix == null) && _functionFilters == null)) {
                "Only one of these must be provided, but both were: " +
                        "'functionFqnPrefix/functionSimpleNamePrefix', 'functionFilters'"
            }
            require(functionFqnPrefix != null || _functionFilters != null) {
                "Either 'functionFqnPrefix' or 'functionFilters' must be provided in the 'demoApp', but both were null"
            }
            requireListNotEmpty(list = _functionFilters, paramName = "functionFilters")
        }

        @Transient
        val functionFilters: List<FunctionFilter> = buildList {
            if (functionFqnPrefix != null) add(FunctionFilter(functionFqnPrefix, functionSimpleNamePrefix))
            if (_functionFilters != null) addAll(_functionFilters)
        }
    }

    @Serializable
    class InlayHints(val enabled: Boolean, val enums: Boolean = false)

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

    @Serializable
    class DemoJetStubGeneration(
        val enableOnlyIn: EnableOnlyIn? = null,
        val nullablePropertyFunctionName: String = "nullable",
        val parameterToPropertyFunctionMappings: List<DemoJetParameterToPropertyFunctionMapping>,
    ) {
        init {
            require(nullablePropertyFunctionName.isNotBlank()) {
                "nullablePropertyFunctionName should not be blank"
            }
            requireListNotEmpty(parameterToPropertyFunctionMappings, ::parameterToPropertyFunctionMappings.name)
        }

        @Serializable
        class EnableOnlyIn(val packageName: String)
    }
}

@Serializable
class DemoJetParameterToPropertyFunctionMapping(
    val functionParameterCriteria: FunctionParameter,
    val propertyFunction: PropertyFunction?,
) {

    @Serializable
    class FunctionParameter(
        val nameRegex: Regex? = null,
        val typeRegex: Regex? = null,
        val typeIsEnum: Boolean? = null,
        val typeIsSubclassOfAll: List<String>? = null,
    ) {
        init {
            require(nameRegex != null || typeRegex != null || typeIsEnum != null || typeIsSubclassOfAll != null) {
                "functionParameterCriteria: at least one param should be non-null"
            }
        }
    }

    @Serializable
    class PropertyFunction(
        val name: String,
        val includeDefault: Boolean = true,
        val defaultRegexToReplace: String? = null,
        val defaultReplacement: String? = null
    ) {
        init {
            if (!includeDefault) require(defaultRegexToReplace == null && defaultReplacement == null) {
                "propertyFunction: if includeDefault is false, than these must be null: " +
                        "defaultRegexToReplace, defaultReplacement"
            }
            if (defaultRegexToReplace != null) requireNotNull(defaultReplacement) {
                "propertyFunction: if defaultRegexToReplace is non-null, defaultReplacement must also be non-null"
            }
        }
    }
}

private fun requireListNotEmpty(list: List<Any>?, paramName: String) {
    if (list != null) require(list.isNotEmpty()) {
        "'$paramName' array must not be empty. If you need to disable this feature, just remove the key " +
                "from json entirely"
    }
}