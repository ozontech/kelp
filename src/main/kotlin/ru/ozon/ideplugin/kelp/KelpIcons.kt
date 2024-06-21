package ru.ozon.ideplugin.kelp

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.IconLoader
import com.intellij.util.system.OS
import ru.ozon.ideplugin.kelp.codeCompletion.DsComponentFunLookupElement
import javax.swing.Icon
import kotlin.io.path.absolutePathString
import kotlin.io.path.div

internal object KelpIcons {
    internal fun getDsComponentFunIcon(project: Project): Icon {
        val path = (pluginConfigDirPath(project) / "dsComponentFunIcon.svg").absolutePathString()
        val prefix = if (OS.CURRENT == OS.Windows) "file:/" else "file://"
        return IconLoader.getIcon(prefix + path, DsComponentFunLookupElement::class.java)
    }
}
