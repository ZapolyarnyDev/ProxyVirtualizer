pluginManagement {
    includeBuild("build-logic")
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
        maven("https://repo.papermc.io/repository/maven-public/") { name = "papermc" }
    }
}

rootProject.name = "ProxyVirtualizer"
include("velocity-plugin", "virtualizer-api")
include("virtualizer-protocol-api")
include("virtualizer-protocol")
include("core")
include("velocity-adapter")
