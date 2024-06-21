package ru.ozon.ideplugin.kelp

import com.android.tools.compose.isComposableFunction
import com.android.tools.idea.projectsystem.getModuleSystem
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.idea.base.psi.kotlinFqName
import org.jetbrains.kotlin.psi.KtNamedFunction
import ru.ozon.ideplugin.kelp.pluginConfig.KelpConfig
import kotlin.io.path.Path
import kotlin.io.path.div

internal val kelpPluginVersion = pluginVersion()

private fun pluginVersion(): String {
    val id = "ru.ozon.ideplugin.kelp"
    return requireNotNull(PluginManagerCore.getPlugin(PluginId.getId(id))) {
        "Kelp plugin was not found by its id: $id. Probably, plugin id has changed."
    }.version
}

internal fun pluginConfigDirPath(project: Project) = Path(project.basePath!!) / ".idea" / "kelp"

internal fun PsiElement.isDsComponentFunction(config: KelpConfig.ComponentFunHighlighting): Boolean =
    isDsComponentFunction(
        functionFqnPrefix = config.functionFqnPrefix,
        functionSimpleNamePrefix = config.functionSimpleNamePrefix ?: ""
    )

internal fun PsiElement.isDsComponentFunction(config: KelpConfig.DemoApp): Boolean =
    isDsComponentFunction(
        functionFqnPrefix = config.functionFqnPrefix,
        functionSimpleNamePrefix = config.functionSimpleNamePrefix ?: ""
    )

private fun PsiElement.isDsComponentFunction(
    functionFqnPrefix: String,
    functionSimpleNamePrefix: String
): Boolean {
    if (this !is KtNamedFunction) return false
    val fqName = kotlinFqName?.asString() ?: return false
    return isComposableFunction() &&
            fqName.startsWith(functionFqnPrefix) &&
            fqName.substringAfterLast('.').startsWith(functionSimpleNamePrefix)
}

internal fun CompletionParameters.isInComposeEnabledModuleAndFile() =
    position.language == KotlinLanguage.INSTANCE && position.getModuleSystem()?.usesCompose == true

internal fun String.camelToSnakeCase(): String {
    val pattern = "(?<=.)[A-Z]".toRegex()
    return replace(pattern, "_$0").lowercase()
}