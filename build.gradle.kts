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
        testFramework(org.jetbrains.intellij.platform.gradle.TestFrameworkType.Platform)
    }
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.mockito:mockito-core:5.7.0")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.1.0")
}

kotlin {
    jvmToolchain(21)
}

intellijPlatform {
    pluginConfiguration {
        version.set(providers.gradleProperty("pluginVersion"))
        
        ideaVersion {
            sinceBuild.set("243")
        }
        
        changeNotes.set(
            """
            <h3>0.1.3</h3>
            <ul>
              <li>Fix: Prevent irrelevant changes from being restored when unpausing</li>
            </ul>
            <h3>0.1.2</h3>
            <ul>
              <li>Add IDE version compatibility declaration for marketplace</li>
            </ul>
            <h3>0.1.1</h3>
            <ul>
              <li>Fix: Correctly identify changelist when pausing files/folders instead of defaulting to "Changes" changelist</li>
            </ul>
            <h3>0.1.0</h3>
            <ul>
              <li>Initial release</li>
              <li>Toggle pause/unpause changelists with Ctrl+Alt+P</li>
              <li>Shelves changes while keeping changelist visible</li>
              <li>Restores changes to original changelist when unpaused</li>
              <li>Background thread processing to avoid EDT blocking</li>
            </ul>
            """.trimIndent()
        )
    }

    publishing {
        token.set(providers.environmentVariable("JETBRAINS_TOKEN"))
        channels.set(providers.gradleProperty("pluginVersion").map { version ->
            listOf(if (version.contains("-beta") || version.contains("-rc")) "beta" else "default")
        })
    }

    pluginVerification {
        ides {
            recommended()
        }
    }
}

// kør lokalt: ./gradlew runIde
// build ZIP: ./gradlew buildPlugin
// publish: ./gradlew publishPlugin
