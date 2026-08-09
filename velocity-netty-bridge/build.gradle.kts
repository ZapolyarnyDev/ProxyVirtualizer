dependencies {
    implementation(project(":virtualizer-protocol-api"))
    compileOnly(rootProject.libs.velocity.api)
    compileOnly(rootProject.libs.netty.codec)
    testImplementation(rootProject.libs.velocity.api)
    testImplementation(rootProject.libs.netty.codec)
}
