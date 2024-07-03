
rootProject.name = "maskinporten-client"

pluginManagement {
    val kotlinVersion: String by settings
    val ktlintVersion: String by settings

    plugins {
        kotlin("jvm") version kotlinVersion
        kotlin("plugin.serialization") version kotlinVersion
        id("org.jlleitschuh.gradle.ktlint") version ktlintVersion
        id("maven-publish")
    }
}
