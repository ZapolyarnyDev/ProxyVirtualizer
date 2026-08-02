import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.jvm.tasks.Jar

dependencies {
    implementation(project(":virtualizer-api"))
    implementation(project(":velocity-adapter"))
    compileOnly(rootProject.libs.velocity.api)
    annotationProcessor(rootProject.libs.velocity.api)
}

tasks.named<Jar>("jar") {
    dependsOn(":virtualizer-api:classes")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from(
        project(":virtualizer-api")
            .extensions
            .getByType(SourceSetContainer::class.java)
            .getByName("main")
            .output
    )
}

tasks.named<Jar>("jar") {
    archiveBaseName.set("ProxyVirtualizer")
}
