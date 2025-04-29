plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    application
}

repositories {
    mavenCentral()
    google()
}

dependencies {
    implementation(kotlin("reflect"))

    implementation(libs.sootup.core)
    implementation(libs.sootup.java.core)
    implementation(libs.sootup.java.sourcecode.frontend)
    implementation(libs.sootup.java.bytecode.frontend)
//    implementation(libs.sootup.jimple.frontend)
    implementation(libs.sootup.callgraph)
//    implementation(libs.sootup.analysis)
    implementation(libs.sootup.qilin)
    implementation(libs.slf4j.api)
    implementation(libs.logback.classic)
    implementation(libs.javaparser)
    implementation(libs.kotlin.serialization.json)
}

application {
    mainClass = "me.dl33.fuzzrank.MainKt"
}

tasks.withType<JavaExec> {
    systemProperty("projectDir", project.projectDir.parent)
}

tasks.register<JavaExec>("runOld") {
    group = "application"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass = "me.dl33.fuzzrank.OldMainKt"
}
