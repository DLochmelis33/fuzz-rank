plugins {
    alias(libs.plugins.kotlin.jvm)
    application
}

repositories {
    mavenCentral()
    google()
}

dependencies {
    implementation(libs.sootup.core)
    implementation(libs.sootup.java.core)
    implementation(libs.sootup.java.sourcecode.frontend)
    implementation(libs.sootup.java.bytecode.frontend)
    implementation(libs.sootup.jimple.frontend)
    implementation(libs.sootup.callgraph)
    implementation(libs.sootup.analysis)
    implementation(libs.sootup.qilin)
    implementation(libs.slf4j.api)
    implementation(libs.logback.classic)
    implementation(libs.javaparser)
}

application {
    mainClass = "org.jetbrains.research.MainKt"
}

tasks.withType<JavaExec> {
    systemProperty("projectDir", project.projectDir.absolutePath)
}
