
rootProject.name = "maskinporten-client"

pluginManagement {
    val kotlinVersion: String by settings

    plugins {
        kotlin("jvm") version kotlinVersion
        id("maven-publish")
    }
}