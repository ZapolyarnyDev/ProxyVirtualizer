dependencies {
    compileOnly(rootProject.libs.lombok)
    annotationProcessor(rootProject.libs.lombok)

    compileOnly(rootProject.libs.jetbrains.annotations)

    implementation(project(":virtualizer-protocol-api"))
}
