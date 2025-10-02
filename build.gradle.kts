plugins {
    kotlin("jvm") version "1.9.25"
    id("org.jetbrains.intellij.platform") version "2.3.0"
}

group = providers.gradleProperty("pluginGroup").get()
version = providers.gradleProperty("pluginVersion").get()

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        intellijIdeaCommunity("2024.3")
        bundledModule("intellij.platform.vcs.impl")
        pluginVerifier()
        zipSigner()
    }
}

kotlin {
    jvmToolchain(17)
}

intellijPlatform {
    pluginConfiguration {
        version.set(providers.gradleProperty("pluginVersion"))
        changeNotes.set(
            """
            Initial release. Adds toggle to mark changelists as [PAUSED] and decorate them as grey with pause icon.
            """.trimIndent()
        )
    }
}

// kør lokalt: ./gradlew runIde
// build ZIP: ./gradlew buildPlugin
