import dev.bmac.gradle.intellij.UpdateXmlTask
import org.jetbrains.changelog.Changelog
import org.jetbrains.changelog.markdownToHTML
import org.jetbrains.intellij.platform.gradle.tasks.BuildPluginTask
import org.jetbrains.intellij.platform.gradle.tasks.CustomRunIdeTask

fun properties(key: String) = providers.gradleProperty(key)
fun environment(key: String) = providers.environmentVariable(key)

plugins {
    alias(libs.plugins.kotlin)
    alias(libs.plugins.intelliJPlatform)
    alias(libs.plugins.changelog)
    alias(libs.plugins.qodana)
    alias(libs.plugins.kover)
    alias(libs.plugins.serialization)
    alias(libs.plugins.intellijPluginUploader)
}

group = properties("pluginGroup").get()
version = properties("pluginVersion").get()

// Configure project's dependencies
repositories {
    mavenCentral()

    intellijPlatform {
        defaultRepositories()
    }
}

// Dependencies are managed with Gradle version catalog - read more: https://docs.gradle.org/current/userguide/platforms.html#sub:version-catalog
dependencies {
    implementation(libs.annotations)
    implementation(libs.serialization.json)

    intellijPlatform {
        create(properties("platformType"), properties("platformVersion"))

        // Plugin Dependencies. Uses `platformBundledPlugins` property from the gradle.properties file for bundled IntelliJ Platform plugins.
        bundledPlugins(pluginsList(properties("platformBundledPlugins")))

        // Plugin Dependencies. Uses `platformPlugins` property from the gradle.properties file for plugin from JetBrains Marketplace.
        plugins(pluginsList(properties("platformPlugins")))

        instrumentationTools()
        pluginVerifier()
    }
}

private fun pluginsList(gradleProperty: Provider<String>) = gradleProperty.map {
    it.split(',').map(String::trim).filter(String::isNotEmpty)
}

// Set the JVM language level used to build the project. Use Java 11 for 2020.3+, and Java 17 for 2022.2+.
kotlin {
    jvmToolchain(17)
}

val kelpPluginDescription = providers.fileContents(layout.projectDirectory.file("README.md")).asText.map {
    val start = "-- Plugin description --"
    val end = "-- Plugin description end --"

    with(it.lines()) {
        if (!containsAll(listOf(start, end))) {
            throw GradleException("Plugin description section not found in README.md:\n$start ... $end")
        }
        subList(indexOf(start) + 1, indexOf(end)).joinToString("\n").let(::markdownToHTML)
    }
}

val kelpChangeNotes = properties("pluginVersion").map { pluginVersion ->
    with(changelog) {
        renderItem(
            (getOrNull(pluginVersion) ?: getUnreleased())
                .withHeader(false)
                .withEmptySections(false),
            Changelog.OutputType.HTML,
        )
    }
}

// Configure IntelliJ Platform Gradle Plugin - read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-extension.html
intellijPlatform {
    buildSearchableOptions = false

    pluginConfiguration {
        version = properties("pluginVersion")

        // Extract the -- Plugin description -- section from README.md and provide for the plugin's manifest
        description = kelpPluginDescription

        val changelog = project.changelog // local variable for configuration cache compatibility
        // Get the latest available change notes from the changelog file
        changeNotes = kelpChangeNotes.get()

        ideaVersion {
            sinceBuild = properties("pluginSinceBuild")
            untilBuild = properties("pluginUntilBuild")
        }
    }

    verifyPlugin {
        ides {
            recommended()
            local("/Applications/Android Studio Preview.app/Contents")
            local("/Applications/Android Studio.app/Contents")
        }
    }
}

// Configure Gradle Changelog Plugin - read more: https://github.com/JetBrains/gradle-changelog-plugin
changelog {
    groups.empty()
    repositoryUrl = properties("pluginRepositoryUrl")
}

tasks {
    wrapper {
        gradleVersion = properties("gradleVersion").get()
    }

    val runLocalIde by registering(CustomRunIdeTask::class) {
        localPath = file("/Applications/Android Studio Preview.app/Contents")
    }

    // Configure UI tests plugin
    // Read more: https://github.com/JetBrains/intellij-ui-test-robot
    testIdeUi {
        systemProperty("robot-server.port", "8082")
        systemProperty("ide.mac.message.dialogs.as.sheets", "false")
        systemProperty("jb.privacy.policy.text", "<!--999.999-->")
        systemProperty("jb.consents.confirmation.enabled", "false")
    }
}

val toGitHubReleaseDir = project.layout.buildDirectory.dir("github-release")

val updateLocalPluginXmlTask = tasks.register<UpdateXmlTask>("updateLocalPluginXml") {
    pluginName = properties("pluginName")
    pluginId = properties("pluginGroup")
    version = properties("pluginVersion")
    pluginDescription = kelpPluginDescription
    sinceBuild = properties("pluginSinceBuild")
    changeNotes = kelpChangeNotes.get()

    updateFile = toGitHubReleaseDir.map { it.file("updatePlugins.xml") }
    downloadUrl = "${properties("pluginRepositoryUrl").get()}/releases/latest/download/${pluginName.get()}-${version.get()}.zip"
}

tasks.register<Copy>("buildKelpIdePlugin") {
    dependsOn(updateLocalPluginXmlTask)
    from(project.tasks.named<BuildPluginTask>("buildPlugin").flatMap { it.archiveFile })
    into(toGitHubReleaseDir)
}

tasks.register("readVersion") {
    inputs.property("version", project.version)
    outputs.file(layout.buildDirectory.file("VERSION"))
    doLast {
        outputs.files.first().writeText(inputs.properties.values.first().toString())
    }
}
