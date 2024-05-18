package ru.ozon.ideplugin.kelp

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys


class ThemeQuickTypeAction : AnAction() {
    override fun update(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR)
        e.presentation.isEnabledAndVisible =
            e.project != null && editor != null && editor.selectionModel.hasSelection()

    }
    override fun actionPerformed(e: AnActionEvent) {
//        Template.()
//        TemplateImpl("sd", "").templateText = "ds"
//        TemplateSettings.getInstance().templateGroups.find { it.name == "Kelp" }?.
//registerCustomShortcutSet()
//
//        setShortcutSet {  }
//        ActionUtil.registerForEveryKeyboardShortcut()
    }
}
