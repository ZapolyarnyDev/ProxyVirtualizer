val projectGroup = "io.github.zapolyarnydev"
val projectVersion = "1.1.2"

plugins {
    id("base")
    id("proxyvirtualizer.spotless-conventions")
    id("proxyvirtualizer.quality-conventions")
}

group = projectGroup
version = projectVersion

subprojects {
    apply(plugin = "proxyvirtualizer.java-conventions")
    apply(plugin = "proxyvirtualizer.spotless-conventions")
    apply(plugin = "proxyvirtualizer.quality-conventions")

    group = projectGroup
    version = projectVersion
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
