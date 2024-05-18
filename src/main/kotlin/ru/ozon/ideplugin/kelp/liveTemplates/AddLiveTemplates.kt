package ru.ozon.ideplugin.kelp.liveTemplates

import com.intellij.codeInsight.template.impl.TemplateImpl
import com.intellij.codeInsight.template.impl.TemplateSettings
import org.jdom.Element
import ru.ozon.ideplugin.kelp.pluginConfig.KelpConfig

internal object AddLiveTemplates {
    private val kotlinTemplateContexts by lazy {
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

    fun execute(config: KelpConfig) {
        val templates = config.liveTemplates.orEmpty()
        val templateSettings = TemplateSettings.getInstance()
        templateSettings.templates.forEach {
            if (it.groupName == "Kelp") templateSettings.removeTemplate(it)
        }
        templates.forEach { template ->
            TemplateImpl(
                /* key = */ template.abbreviation,
                /* string = */ template.text,
                /* group = */ "Kelp"
            ).apply {
                template.description?.let { description = it }
                addVariable("CODE_COMPLETION", "complete()", "", true)
                templateContext.readTemplateContext(kotlinTemplateContexts)
                templateSettings.addTemplate(this)
            }
        }
    }
}