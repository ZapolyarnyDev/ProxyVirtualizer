val projectGroup = "io.github.zapolyarnydev"
val projectVersion = "1.1.2"

plugins {
    id("java")
    id("proxyvirtualizer.spotless-conventions")
}

group = projectGroup
version = projectVersion

subprojects {
    apply(plugin = "proxyvirtualizer.java-conventions")
    apply(plugin = "proxyvirtualizer.spotless-conventions")

    group = projectGroup
    version = projectVersion

    dependencies {
        compileOnly(rootProject.libs.lombok)
        annotationProcessor(rootProject.libs.lombok)

        compileOnly(rootProject.libs.velocity.api)
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

tasks.register("installGitHooks") {
    group = "git"
    description = "Installs the project's Git hooks"

    doLast {
        providers.exec {
            commandLine("git", "config", "--local", "core.hooksPath", ".githooks")
        }.result.get().assertNormalExitValue()
    }
}
