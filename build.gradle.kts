plugins {
    kotlin("jvm") version "1.9.23"
}

group = "no.nav.helsearbeidsgiver"
version = "0.1"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(21)
}