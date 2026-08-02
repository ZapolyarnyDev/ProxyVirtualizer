dependencies {
    compileOnly(rootProject.libs.lombok)
    annotationProcessor(rootProject.libs.lombok)
    compileOnlyApi(rootProject.libs.jetbrains.annotations)
    compileOnly(rootProject.libs.velocity.api)
}

tasks.named<org.gradle.jvm.tasks.Jar>("jar") {
    archiveBaseName.set("ProxyVirtualizer-api")
}
