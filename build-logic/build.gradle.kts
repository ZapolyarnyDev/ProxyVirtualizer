plugins {
    `kotlin-dsl`
}

kotlin {
    jvmToolchain(21)
}

repositories {
    gradlePluginPortal()
}

dependencies {
    implementation("com.diffplug.spotless:spotless-plugin-gradle:8.7.0")
}
