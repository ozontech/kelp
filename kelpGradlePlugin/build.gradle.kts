plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
    id("com.gradle.plugin-publish") version "1.2.1"
}

group = "ru.ozon.kelp"
version = "0.0.1"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(17)
    explicitApi()
}

gradlePlugin {
    website.set("https://github.com/ozontech/kelp?tab=readme-ov-file#-gradle-plugin")
    vcsUrl.set("https://github.com/ozontech/kelp")
    plugins {
        create("kelp") {
            id = "ru.ozon.kelp"
            displayName = "Kelp"
            description = "Companion plugin for the Kelp IDE plugin for Design Systems that assists in IDE plugin " +
                    "presence detection and downloading of the demo app apk"
            tags = listOf("android", "ide", "idea", "apk")
            implementationClass = "ru.ozon.kelp.KelpGradlePlugin"
        }
    }
}

publishing {
    repositories {
        mavenLocal()
    }
}