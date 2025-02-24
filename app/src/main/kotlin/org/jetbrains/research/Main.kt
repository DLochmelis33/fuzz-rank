package org.jetbrains.research

import sootup.core.inputlocation.AnalysisInputLocation
import sootup.java.bytecode.inputlocation.JavaClassPathAnalysisInputLocation
import sootup.java.core.views.JavaView
import sootup.java.sourcecode.inputlocation.JavaSourcePathAnalysisInputLocation
import kotlin.io.path.*

fun main() {
    val thisProjectDir = Path(System.getProperty("projectDir")!!.toString())
    val javaProjectsDir = thisProjectDir.parent.resolve("javaProjects")
    val projectA = javaProjectsDir.resolve("a")

    val jarsToAnalyze = listOf("app", "list", "utilities")
        .map { projectA.resolve("$it/build/libs/$it.jar").absolutePathString() }
    println(jarsToAnalyze.joinToString("\n"))

    val listJar = jarsToAnalyze[1]
    val inputLocation: AnalysisInputLocation = JavaClassPathAnalysisInputLocation(listJar)
    println("input location")
    val view = JavaView(inputLocation)
    println("java view")
    val linkedListClassType = view.identifierFactory.getClassType("org.example.list.LinkedList")
    println("class type")
    val sootClass = view.getClass(linkedListClassType).get()
    println("soot class")
    sootClass.methods.forEach { method ->
        println(method.name)
    }

}
