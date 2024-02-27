package ru.ozon.ideplugin.kelp

import com.android.ddmlib.IDevice
import com.android.ddmlib.MultiLineReceiver
import com.android.ddmlib.NullOutputReceiver
import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.codeInsight.intention.PriorityAction
import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction
import com.intellij.codeInsight.intention.preview.IntentionPreviewInfo
import com.intellij.notification.NotificationGroup
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.ListPopupStep
import com.intellij.openapi.ui.popup.PopupStep
import com.intellij.openapi.ui.popup.util.BaseListPopupStep
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import org.jetbrains.android.sdk.AndroidSdkUtils
import org.jetbrains.kotlin.idea.base.psi.kotlinFqName
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.uast.UIdentifier
import org.jetbrains.uast.toUElement
import java.nio.file.Path
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.io.path.div

/**
 * If you place the cursor on one of the functions (components of the design system) and summon the
 * Intention Actions list, a new action will be available.
 *
 * - When called, the plugin checks whether there is a running Android device; if not, it offers to launch it.
 * - If launched, checks how many. If there are more than one, then a menu ([chooseFromManyDevices]) is offered for
 * selecting the Android device on which you'd like to open the demo app. If there is only one Android device,
 * it is selected by default.
 * - Turns on the screen of the Android device
 * - Acquires the intended latest version of the demo app from name of the demo app apk file.
 * - Checks whether the demo app is installed on the Android device, and if so, whether it is the latest
 * version. If not, the apk file is installed on the Android device from the predefined path: [apkPath].
 * - Next, an intent is launched with a deeplink leading to the component page in demo app.
 */
@Suppress("IntentionDescriptionNotFoundInspection")
internal class OpenDsComponentInDemoAppIntention : PsiElementBaseIntentionAction(), IntentionAction, PriorityAction {
    override fun getFamilyName(): String = KelpBundle.message("kelpIntentionsFamilyName")
    override fun generatePreview(project: Project, editor: Editor, file: PsiFile) = IntentionPreviewInfo.EMPTY!!
    override fun getPriority(): PriorityAction.Priority = PriorityAction.Priority.HIGH

    override fun isAvailable(project: Project, editor: Editor?, element: PsiElement): Boolean {
        val config = project.kelpConfig()?.demoApp ?: return false
        if (editor == null ||
            element.toUElement() !is UIdentifier ||
            config.functionSimpleNamePrefix?.let { !element.text.startsWith(it) } == true
        ) {
            return false
        }

        val isAvailable = element.parent.reference?.resolve()?.isDsComponentFunction(config) == true ||
                (element.parent as? KtNamedFunction)?.isDsComponentFunction(config) == true
        if (isAvailable) text = config.intentionName
        return isAvailable
    }

    override fun invoke(project: Project, editor: Editor, element: PsiElement) {
        val notificationGroup = NotificationGroupManager.getInstance().getNotificationGroup(
            KelpBundle.message("kelpNotificationGroup"),
        )
        val componentFqn = getDsComponentFQN(element) ?: run {
            showNoDSComponentFQN(project, notificationGroup)
            return
        }
        val debugBridge = AndroidSdkUtils.getDebugBridge(project) ?: run {
            showNoSdkError(project, notificationGroup)
            return
        }
        val taskName = KelpBundle.message("openingInDemoAppMessage")
        object : Task.Backgroundable(project, taskName, false) {
            override fun run(progressIndicator: ProgressIndicator) {
                runCatching {
                    val config = project.kelpConfig()?.demoApp ?: return

                    val devices = debugBridge.devices.filter { it.isOnline }.toList().ifEmpty {
                        showNoDevicesError(project, notificationGroup)
                        activateDeviceManagerToolWindow(project)
                        return
                    }

                    val device = if (devices.size == 1) {
                        devices.first()
                    } else {
                        chooseFromManyDevices(devices, editor) ?: return
                    }

                    val receiver = NullOutputReceiver.getReceiver()
                    progressIndicator.text = KelpBundle.message("turningOnScreenMessage")
                    device.executeShellCommand(
                        UNLOCK_SCREEN_COMMAND, receiver, SHELL_TIMEOUT_MS, SHELL_TIMEOUT_MS, TimeUnit.MILLISECONDS
                    )

                    apkInstallingStep(project, config, progressIndicator, device)

                    progressIndicator.text = taskName
                    val command = openComponentInDemoAppCommand(
                        config = config,
                        componentFqn = componentFqn.asString(),
                    )
                    device.executeShellCommand(
                        command, receiver, SHELL_TIMEOUT_MS, SHELL_TIMEOUT_MS, TimeUnit.MILLISECONDS
                    )

                    showOpenedNotification(componentFqn.shortName().asString(), project, notificationGroup)
                }.onFailure {
                    showGeneralError(it, project)
                }
            }
        }.queue()
    }

    /**
     * @return null, if device was not selected
     */
    private fun chooseFromManyDevices(devices: List<IDevice>, editor: Editor): IDevice? {
        val latch = CountDownLatch(1)
        var device: IDevice? = null
        val step = object : BaseListPopupStep<IDevice>(KelpBundle.message("chooseDevicePopupTitle"), devices) {
            override fun getTextFor(value: IDevice): String = value.name
            override fun onChosen(selectedValue: IDevice, finalChoice: Boolean): PopupStep<*>? {
                if (!finalChoice) return null
                device = selectedValue
                latch.countDown()
                return FINAL_CHOICE
            }

            override fun canceled() = latch.countDown()
        }
        invokeLater { showPopup(step, editor) }
        latch.await()
        return device
    }

    private fun apkInstallingStep(
        project: Project,
        config: KelpConfig.DemoApp,
        progressIndicator: ProgressIndicator,
        device: IDevice,
    ) {
        if (config.apkInstallation != true) return

        progressIndicator.text = KelpBundle.message("checkingInstalledAppVersionMessage")
        val latestVersion = getLatestVersion(project)
        val isLatestVersionInstalled = isLatestVersionInstalled(device, latestVersion, config.appPackageName)

        if (isLatestVersionInstalled) return

        progressIndicator.text = KelpBundle.message("uninstallingPreviousAppMessage")
        device.uninstallPackage(config.appPackageName)

        progressIndicator.text = KelpBundle.message("installingAppMessage")
        val apkPath = apkPath(project)
        device.installPackage(apkPath.toString(), true)
    }

    private fun getLatestVersion(project: Project): String {
        val apkFileName = apkPath(project).toFile().name
        return apkFileNameRegex.matchEntire(apkFileName)?.groups?.get("version")?.value
            ?: error(KelpBundle.message("demoAppLatestVersionNotFoundErrorMessage", apkFileName))
    }

    private fun apkPath(project: Project): Path {
        val apkFolderPath = pluginConfigDirPath(project) / "apk"
        return runReadAction {
            VirtualFileManager.getInstance()
                .findFileByNioPath(apkFolderPath)
                ?.children
                ?.find { it.name.matches(apkFileNameRegex) }
                ?.toNioPath()
                ?: error(KelpBundle.message("apkFileNotFoundErrorMessage", apkFolderPath))
        }
    }

    private val apkFileNameRegex = "demoApp-(?<version>.+?).apk".toRegex()

    private fun isLatestVersionInstalled(device: IDevice, latestVersion: String, demoAppPackageName: String): Boolean {
        val isLatestVersionInstalled = AtomicBoolean(false)
        val latch = CountDownLatch(1)
        device.executeShellCommand(
            "dumpsys package $demoAppPackageName | grep versionName",
            object : MultiLineReceiver() {
                override fun isCancelled(): Boolean = false
                override fun processNewLines(lines: Array<String>) {
                    val installedVersion = lines
                        .map { it.substringAfterLast("versionName=", missingDelimiterValue = "") }
                        .firstOrNull { it.isNotBlank() }

                    isLatestVersionInstalled.compareAndSet(false, installedVersion == latestVersion)
                    latch.countDown()
                }
            },
            SHELL_TIMEOUT_MS, SHELL_TIMEOUT_MS, TimeUnit.MILLISECONDS,
        )
        latch.await(SHELL_TIMEOUT_MS, TimeUnit.MILLISECONDS)
        return isLatestVersionInstalled.get()
    }

    private fun getDsComponentFQN(element: PsiElement): FqName? {
        val name =
            if (element.parent is KtNamedFunction) element.parent.kotlinFqName
            else element.parent.reference?.resolve()?.kotlinFqName
        return name?.takeUnless { it.asString().isBlank() }
    }

    // adb shell am start
    // -W -a android.intent.action.VIEW
    // -d "yourscheme://component/com.your.designsystem.package.components.Badge"
    // com.your.designsystem.package
    private fun openComponentInDemoAppCommand(config: KelpConfig.DemoApp, componentFqn: String): String {
        val packageName = config.appPackageName
        val deeplink = config.componentDeeplink.replace(DS_COMPONENT_FQN_DEEPLINK_PLACEHOLDER, componentFqn)
        return "am start -W -a android.intent.action.VIEW -d \"$deeplink\" $packageName"
    }

    private fun showPopup(step: ListPopupStep<*>, editor: Editor) {
        val popup = JBPopupFactory.getInstance().createListPopup(step)
        popup.showInBestPositionFor(editor)
    }

    private fun activateDeviceManagerToolWindow(project: Project) = invokeLater {
        ToolWindowManager.getInstance(project).getToolWindow("Device Manager 2")?.activate(null)
    }

    private fun showOpenedNotification(funSimpleName: String, project: Project, notificationGroup: NotificationGroup) =
        notificationGroup
            .createNotification(
                content = KelpBundle.message("openedNotificationContent", funSimpleName),
                type = NotificationType.INFORMATION,
            )
            .notify(project)

    private fun showGeneralError(throwable: Throwable, project: Project) = invokeLater {
        Messages.showErrorDialog(
            project,
            throwable.localizedMessage + "\n" + throwable.stackTraceToString(),
            KelpBundle.message("componentOpeningErrorTitle"),
        )
    }

    private fun showNoDSComponentFQN(project: Project, notificationGroup: NotificationGroup) = notificationGroup
        .createNotification(
            content = KelpBundle.message("noSuchComponentNotificationContent"),
            type = NotificationType.ERROR,
        )
        .notify(project)

    private fun showNoSdkError(project: Project, notificationGroup: NotificationGroup) = notificationGroup
        .createNotification(
            content = KelpBundle.message("noAndroidSdkNotificationContent"),
            type = NotificationType.ERROR,
        )
        .notify(project)

    private fun showNoDevicesError(project: Project, notificationGroup: NotificationGroup) = notificationGroup
        .createNotification(
            title = KelpBundle.message("noRunningDevicesNotificationTitle"),
            content = KelpBundle.message("noRunningDevicesNotificationContent"),
            type = NotificationType.ERROR,
        )
        .notify(project)

    private companion object {
        private const val DS_COMPONENT_FQN_DEEPLINK_PLACEHOLDER = "DS_COMPONENT_FQN_DEEPLINK_PLACEHOLDER"
        private const val UNLOCK_SCREEN_COMMAND = "input keyevent 82"
        private const val SHELL_TIMEOUT_MS = 6000L
    }
}
