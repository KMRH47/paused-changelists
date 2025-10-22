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
            <h3>0.1.5</h3>
            <ul>
              <li>Improve changelog clarity</li>
            </ul>
            <h3>0.1.4</h3>
            <ul>
              <li>Fix: Restored wrong version of changes when pausing multiple times</li>
            </ul>
            <h3>0.1.3</h3>
            <ul>
              <li>Fix: Incorrect changes restored when switching branches</li>
            </ul>
            <h3>0.1.2</h3>
            <ul>
              <li>Fix: Plugin compatibility with newer IDE versions</li>
            </ul>
            <h3>0.1.1</h3>
            <ul>
              <li>Fix: Pausing specific files now affects correct changelist</li>
            </ul>
            <h3>0.1.0</h3>
            <ul>
              <li>Initial release</li>
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
