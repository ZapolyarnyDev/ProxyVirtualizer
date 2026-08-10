import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.jvm.tasks.Jar

plugins {
    alias(libs.plugins.shadow)
}

dependencies {
    implementation(project(":virtualizer-api"))
    implementation(project(":core"))
    implementation(project(":velocity-adapter"))
    implementation(project(":velocity-netty-bridge"))
    implementation(project(":virtualizer-protocol-api"))
    compileOnly(rootProject.libs.velocity.api)
    annotationProcessor(rootProject.libs.velocity.api)
    testImplementation(rootProject.libs.velocity.api)
    testImplementation(rootProject.libs.netty.codec)
}

tasks.named<Jar>("jar") {
    archiveBaseName.set("ProxyVirtualizer")
    archiveClassifier.set("plain")
}

tasks.named<ShadowJar>("shadowJar") {
    archiveBaseName.set("ProxyVirtualizer")
    archiveClassifier.set("")
}
