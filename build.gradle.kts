import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

val name: String by project
val version: String by project
val targetMC: String by project

plugins {
    kotlin("jvm") version "1.9.21"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

kotlin {
    jvmToolchain(21)
}

repositories {
    mavenCentral()
    maven("https://repo.purpurmc.org/snapshots")
}

dependencies {
    /* Essentials Dependency */
    implementation(kotlin("stdlib")) // Kotlin Standard Library : Apache-2.0 License
    implementation(kotlin("reflect")) // Kotlin Reflection : Apache-2.0 License
    /* Essentials Dependency */

//    compileOnly(fileTree(mapOf("dir" to "libs/compileOnly", "include" to listOf("*.jar")))) // Load all jars in libs folder (Local Libraries)
    compileOnly("org.purpurmc.purpur", "purpur-api", "${targetMC}-R0.1-SNAPSHOT") // PurpurMC API : MIT License
    compileOnly("dev.jorel", "commandapi-bukkit-core", "9.3.0") // CommandAPI Dev Only : MIT License

//    implementation(fileTree(mapOf("dir" to "libs/implementation", "include" to listOf("*.jar")))) // Load all jars in libs folder (Local Libraries)
    implementation("dev.jorel", "commandapi-bukkit-shade", "9.3.0") // CommandAPI Shade : MIT License
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

    register<ShadowJar>("purpurJar") {
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