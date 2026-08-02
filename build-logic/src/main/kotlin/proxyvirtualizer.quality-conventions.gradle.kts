import org.gradle.api.tasks.compile.JavaCompile

val strict = providers.gradleProperty("strict").map(String::toBoolean).orElse(false)

tasks.withType<JavaCompile>().configureEach {
    options.compilerArgs.add("-Xlint:all,-processing")
    if (strict.get()) {
        options.compilerArgs.add("-Werror")
    }
}

pluginManager.withPlugin("com.diffplug.spotless") {
    tasks.named("check") {
        dependsOn("spotlessCheck")
    }
}
