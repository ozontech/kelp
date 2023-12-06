package ru.ozon.ideplugin.kelp

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.openapi.util.io.toNioPath
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.findOrCreateFile
import com.intellij.openapi.vfs.writeText
import kotlin.io.path.Path
import kotlin.io.path.div

internal class NewWritePluginPresenceFile : ProjectActivity {
    override suspend fun execute(project: Project) = invokeLater {
        val pluginVersion = kelpPluginVersion
        runWriteAction {
            val configDirRelativePath = ".idea".toNioPath() / "kelp" / PLUGIN_PRESENCE_FILE_NAME
            getProjectBaseDir(project)
                .findOrCreateFile(configDirRelativePath.toString())
                .writeText(pluginVersion)
        }

        // create a service so that it lives as a singleton,
        // and dispose is called when the project is closed / the plugin is deleted
        project.service<DeletePluginPresenceFileService>()
    }
}

private fun getProjectBaseDir(project: Project): VirtualFile {
    val path = VirtualFileManager.getInstance().findFileByNioPath(Path(project.basePath!!))
    return path ?: error("project's root directory was not found: ${project.basePath}")
}

@Service(Service.Level.PROJECT)
internal class DeletePluginPresenceFileService(private val project: Project) : Disposable {
    override fun dispose() {
        runWriteAction {
            VirtualFileManager.getInstance()
                .findFileByNioPath(pluginConfigDirPath(project) / PLUGIN_PRESENCE_FILE_NAME)
                ?.delete(this)
        }
    }
}

private const val PLUGIN_PRESENCE_FILE_NAME = "pluginIsPresent"
