import org.gradle.jvm.tasks.Jar

dependencies {
    implementation(project(":virtualizer-api"))
    implementation(project(":velocity-adapter"))
    compileOnly(rootProject.libs.velocity.api)
    annotationProcessor(rootProject.libs.velocity.api)
}

tasks.named<Jar>("jar") {
    archiveBaseName.set("ProxyVirtualizer")
}
