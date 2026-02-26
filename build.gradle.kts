val projectGroup = "io.github.zapolyarnydev"
val projectVersion = "1.1.1"

plugins {
    id("java")
}

group = projectGroup
version = projectVersion

subprojects {
    apply(plugin = "java-library")

    group = projectGroup
    version = projectVersion

    dependencies {
        compileOnly(rootProject.libs.lombok)
        annotationProcessor(rootProject.libs.lombok)

        compileOnly(rootProject.libs.velocity.api)
    }

    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(21))
        }
        withSourcesJar()
    }

    val strict = providers.gradleProperty("strict").map { it.toBoolean() }.orElse(false)

    tasks.withType<JavaCompile>().configureEach {
        options.compilerArgs.addAll(listOf("-Xlint:all"))
        if (strict.get())
            options.compilerArgs.add("-Werror")
    }
}

tasks.named<Jar>("jar") {
    enabled = false
}