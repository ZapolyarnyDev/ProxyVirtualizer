import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.tasks.testing.Test
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent

val libraries = rootProject.extensions.getByType<VersionCatalogsExtension>().named("libs")

dependencies {
    add("testImplementation", libraries.findLibrary("junit-jupiter").get())
    add("testImplementation", libraries.findLibrary("assertj-core").get())
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()

    testLogging {
        events(TestLogEvent.FAILED, TestLogEvent.SKIPPED)
        exceptionFormat = TestExceptionFormat.FULL
        showCauses = true
        showStackTraces = true
    }
}
