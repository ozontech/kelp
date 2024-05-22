package ru.ozon.ideplugin.kelp.liveTemplates

import com.intellij.codeInsight.template.impl.TemplateImpl
import com.intellij.codeInsight.template.impl.TemplateSettings
import org.jdom.Element
import ru.ozon.ideplugin.kelp.pluginConfig.KelpConfig

internal object AddLiveTemplates {
    private val kotlinTemplateContext by lazy {
        Element("a").apply {
            listOf("KOTLIN_EXPRESSION", "KOTLIN_STATEMENT").forEach { name ->
                addContent(
                    Element("option").apply {
                        setAttribute("name", name)
                        setAttribute("value", "true")
                    }
                )
            }
        }
    }

    fun execute(config: KelpConfig, projectName: String) {
        val templates = config.liveTemplates.orEmpty()
        val templateSettings = TemplateSettings.getInstance()
        removeAllTemplates(templateSettings, projectName)
        templates.forEach { template ->
            TemplateImpl(
                /* key = */ template.abbreviation,
                /* string = */ template.text,
                /* group = */ "Kelp ($projectName)"
            ).apply {
                template.description?.let { description = it }
                addVariable("CODE_COMPLETION", "complete()", "", true)
                templateContext.readTemplateContext(kotlinTemplateContext)
                templateSettings.addTemplate(this)
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