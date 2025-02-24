package org.jetbrains.research

import sootup.core.inputlocation.AnalysisInputLocation
import sootup.java.bytecode.inputlocation.JavaClassPathAnalysisInputLocation
import sootup.java.core.views.JavaView
import java.nio.file.Path
import kotlin.io.path.*

fun main() {
    val thisProjectDir = Path(System.getProperty("projectDir")!!.toString())
    val javaProjectsDir = thisProjectDir.parent.resolve("javaProjects")
    val projectA = javaProjectsDir.resolve("a")

    val jarsToAnalyze = listOf("app", "list", "utilities")
        .map { projectA.resolve("$it/build/libs/$it.jar") }
    println(jarsToAnalyze.joinToString("\n"))

    val listJar = jarsToAnalyze[1]
    calcShit(listJar)
}

fun calcShit(jarPath: Path) {
    println("analyzing $jarPath")
    val inputLocation: AnalysisInputLocation = JavaClassPathAnalysisInputLocation(jarPath.absolutePathString())
    val view = JavaView(inputLocation)

//    val linkedListClassType = view.identifierFactory.getClassType("org.example.list.LinkedList")
//    val sootClass = view.getClass(linkedListClassType).get()

    val allClasses = view.classes
    for (sootClass in allClasses) {
        for (method in sootClass.methods) {
            val stmtGraph = method.body.stmtGraph

            // cyclomatic complexity = #edges - #nodes + #components
            // sootup guarantees 1 component per method body CANNOT FUCKING CONFIRM I LOST ITTTTT
            // that makes sense at least
            // but wiki says +2
            // and checking it for paths manually confirms 2

            var E = 0
            var N = 0
            stmtGraph.blocks.forEach { stmt ->
                E += stmt.successors.size
                N++
            }
            val cyclomaticComplexity = E - N + 2
            println("method ${method} ==> $cyclomaticComplexity")
        }
    }
}
