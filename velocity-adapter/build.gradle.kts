dependencies {
    implementation(project(":core"))
    compileOnly(rootProject.libs.velocity.api)
    testImplementation(rootProject.libs.velocity.api)
}
