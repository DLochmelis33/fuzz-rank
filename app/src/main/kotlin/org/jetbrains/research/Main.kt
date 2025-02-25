package org.jetbrains.research

import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.stmt.ForEachStmt
import com.github.javaparser.ast.stmt.ForStmt
import com.github.javaparser.ast.stmt.WhileStmt
import com.github.javaparser.ast.visitor.VoidVisitorAdapter
import com.github.javaparser.utils.SourceRoot
import sootup.core.inputlocation.AnalysisInputLocation
import sootup.java.bytecode.inputlocation.JavaClassPathAnalysisInputLocation
import sootup.java.core.views.JavaView
import java.nio.file.Path
import kotlin.io.path.*
import kotlin.jvm.optionals.getOrNull

fun main() {
    val thisProjectDir = Path(System.getProperty("projectDir")!!.toString())
    val javaProjectsDir = thisProjectDir.parent.resolve("javaProjects")
    val projectA = javaProjectsDir.resolve("a")

    haha(projectA.resolve("list/src/main/java"))
    return

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

fun haha(javaSourceDir: Path) {
    val sourceRoot = SourceRoot(javaSourceDir)
//    val defaultParserConfig = sourceRoot.parserConfiguration
//    sourceRoot.parse("", defaultParserConfig, SourceRoot.Callback { p1, p2, pr ->
//        SourceRoot.Callback.Result.DONT_SAVE
//    })
    val parseResults = sourceRoot.tryToParse()
    val cus = parseResults.mapNotNull { it.result.getOrNull() }

    class LoopVisitor : VoidVisitorAdapter<() -> Unit>() {
        override fun visit(n: ForStmt, action: () -> Unit) {
            action()
            super.visit(n, action)
        }

        override fun visit(n: ForEachStmt, action: () -> Unit) {
            action()
            super.visit(n, action)
        }

        override fun visit(n: WhileStmt, action: () -> Unit) {
            action()
            super.visit(n, action)
        }
    }

    val data = mutableMapOf<MethodDeclaration, Int>()
    val visitor = object : VoidVisitorAdapter<MutableMap<MethodDeclaration, Int>>() {
        override fun visit(n: MethodDeclaration, collector: MutableMap<MethodDeclaration, Int>) {
            super.visit(n, collector)

            var loopCount = 0
            n.accept(LoopVisitor()) { loopCount++ }

            collector[n] = loopCount
        }
    }

    for (cu in cus) {
        cu.accept(visitor, data)
    }

    data.entries.joinToString("\n") { (k, v) -> "${k.signature} = $v" }.let { println(it) }
}
