import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

val name: String by project
val version: String by project
val targetPaperAPI: String by project

plugins {
    kotlin("jvm") version "1.9.21"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

kotlin {
    jvmToolchain(17)
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    /* Essentials Dependency */
    compileOnly(kotlin("stdlib")) // Kotlin Standard Library : Apache-2.0 License
    compileOnly(kotlin("reflect")) // Kotlin Reflection : Apache-2.0 License
    compileOnly("org.jetbrains.kotlinx", "kotlinx-coroutines-core", "1.7.3") // Kotlin Coroutines : Apache-2.0 License
    /* Essentials Dependency */

    compileOnly(fileTree(mapOf("dir" to "libs/compileOnly", "include" to listOf("*.jar")))) // Load all jars in libs folder (Local Libraries)
    compileOnly("io.papermc.paper", "paper-api", "${targetPaperAPI}-R0.1-SNAPSHOT") // PaperMC API : MIT License
    compileOnly("dev.jorel", "commandapi-bukkit-core", "9.3.0") // CommandAPI Dev Only : MIT License
    /**
     * compileOnly("io.github.monun", "heartbeat-coroutines", "<latest_version>") // Heartbeat Coroutine : GPL-3.0 License
     * Please Add this to your README.md if you use this library
     *
     * > N. heartbeat-coroutines - `GPL-3.0 License`
     * >    * https://github.com/monun/heartbeat-coroutines
     * >    * https://github.com/monun/heartbeat-coroutines/blob/master/LICENSE.md
     *
     * And Add this to your plugin.yml (libraries)
     * - io.github.monun:heartbeat-coroutines:<latest_version>
     */
    /**
     * compileOnly("io.github.monun", "invfx-api", "<latest_version>") // InvFX API : GPL-3.0 License
     * Please Add this to your README.md if you use this library
     *
     * > N. invfx - `GPL-3.0 License`
     * >    * https://github.com/monun/invfx
     * >    * https://github.com/monun/invfx/blob/master/LICENSE.md
     *
     * And Add this to your plugin.yml (libraries)
     * - io.github.monun:invfx-core:<latest_version>
     */

    implementation(fileTree(mapOf("dir" to "libs/implementation", "include" to listOf("*.jar")))) // Load all jars in libs folder (Local Libraries)
}

tasks {
    processResources {
        filteringCharset = "UTF-8"

        filesMatching("**/*.yml") {
            expand(project.properties)
        }
    }

    project.delete(
        file("build/resources")
    )

    register<ShadowJar>("paperJar") {
        archiveBaseName.set(project.name)
        archiveVersion.set(version)
        archiveClassifier.set("")
        from(sourceSets["main"].output, "LICENSE", "README.MD")

        configurations = listOf(project.configurations.runtimeClasspath.get()) // Add all dependencies to the jar

        doLast {
            copy {
                val archiveFileName = "${project.name}-dev.jar"

                from(archiveFile)
                rename { archiveFileName }

                val newPluginFileLocation = File("\\\\192.168.123.107\\Users\\User\\Desktop\\DEV\\plugins") // DevServer
//                val newPluginFileLocation = File(rootDir, ".dev/plugins") // Local

                if (File(newPluginFileLocation, archiveFileName).exists()) {
                    into(File(newPluginFileLocation, "update"))
                    File(newPluginFileLocation, "update/RELOAD").delete()
                } else {
                    into(newPluginFileLocation)
                }
            }
        }
    }
}