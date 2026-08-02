dependencies {
    implementation(project(":core"))
    implementation(project(":virtualizer-protocol"))
    compileOnly(rootProject.libs.velocity.api)
}
