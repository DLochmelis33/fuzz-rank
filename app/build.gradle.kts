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
}

application {
    mainClass = "org.research.plan.MainKt"
}
