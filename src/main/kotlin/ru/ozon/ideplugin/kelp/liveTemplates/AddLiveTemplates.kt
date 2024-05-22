package ru.ozon.ideplugin.kelp.liveTemplates

import com.intellij.codeInsight.template.impl.TemplateImpl
import com.intellij.codeInsight.template.impl.TemplateSettings
import org.jdom.Element
import ru.ozon.ideplugin.kelp.pluginConfig.KelpConfig

internal object AddLiveTemplates {
    fun execute(config: KelpConfig, projectName: String) {
        val templateSettings = TemplateSettings.getInstance()
        removeAllTemplates(templateSettings, projectName)

        val templates = config.liveTemplates.orEmpty()
        templatesFromKelpConfig(templates, projectName)
            .forEach(templateSettings::addTemplate)
    }

    private fun templatesFromKelpConfig(
        templates: List<KelpConfig.LiveTemplate>,
        projectName: String
    ) = templates.map { template ->
        TemplateImpl(
            /* key = */ template.abbreviation,
            /* string = */ template.text,
            /* group = */ "Kelp ($projectName)"
        ).apply {
            description = template.description
            isToReformat = template.reformat
            isToShortenLongNames = template.shortenFQNames
            template.variables.forEach { variable ->
                addVariable(
                    /* name = */ variable.name,
                    /* expression = */ variable.expression,
                    /* defaultValue = */ variable.defaultValue,
                    /* isAlwaysStopAt = */ variable.alwaysStopAt,
                )
            }
            templateContext.readTemplateContext(
                buildTemplateContext(template.context)
            )
        }
    }

    private fun buildTemplateContext(contextIds: List<String>): Element {
        return Element("a").apply {
            contextIds.forEach { name ->
                addContent(
                    Element("option").apply {
                        setAttribute("name", name)
                        setAttribute("value", "true")
                    }
                )
            }
        }
    }

    private fun removeAllTemplates(
        templateSettings: TemplateSettings,
        projectName: String
    ) {
        templateSettings.templates.forEach {
            if (it.groupName == "Kelp ($projectName)") templateSettings.removeTemplate(it)
        }
    }
}