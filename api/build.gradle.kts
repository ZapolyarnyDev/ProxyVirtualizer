dependencies {
    compileOnly(rootProject.libs.lombok)
    annotationProcessor(rootProject.libs.lombok)
    compileOnly(rootProject.libs.velocity.api)
}

tasks.named<org.gradle.jvm.tasks.Jar>("jar") {
    archiveBaseName.set("ProxyVirtualizer-api")
}
