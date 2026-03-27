package ru.ozon.ideplugin.kelp.pluginConfig

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.jsonSchema.extension.JsonSchemaFileProvider
import com.jetbrains.jsonSchema.extension.JsonSchemaProviderFactory
import com.jetbrains.jsonSchema.extension.JsonSchemaProviderFactory.getResourceFile
import com.jetbrains.jsonSchema.extension.SchemaType
import com.jetbrains.jsonSchema.impl.JsonSchemaVersion
import ru.ozon.ideplugin.kelp.KelpBundle

internal class ConfigJsonSchemaProviderFactory : JsonSchemaProviderFactory {
    override fun getProviders(project: Project) = listOf(ConfigJsonSchemaFileProvider())
}

internal class ConfigJsonSchemaFileProvider : JsonSchemaFileProvider {
    override fun isAvailable(file: VirtualFile) =
        file.path.endsWith("/.idea/kelp/$CONFIG_FILE_NAME")

    override fun getName() = KelpBundle.message("configJsonSchemaName")
    override fun getSchemaFile() = getResourceFile(this::class.java, "/jsonSchema/config.schema.json")
    override fun getSchemaType() = SchemaType.embeddedSchema
    override fun isUserVisible() = false
    override fun getSchemaVersion() = JsonSchemaVersion.SCHEMA_2020_12
}
