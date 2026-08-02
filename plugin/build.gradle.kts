import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.jvm.tasks.Jar

dependencies {
    implementation(project(":api"))
    compileOnly(rootProject.libs.velocity.api)
    annotationProcessor(rootProject.libs.velocity.api)
}

tasks.named<Jar>("jar") {
    dependsOn(":api:classes")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from(project(":api").extensions.getByType(SourceSetContainer::class.java).getByName("main").output)
}

tasks.named<Jar>("jar") {
    archiveBaseName.set("ProxyVirtualizer")
}
