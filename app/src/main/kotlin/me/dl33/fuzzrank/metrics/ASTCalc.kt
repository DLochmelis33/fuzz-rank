package me.dl33.fuzzrank.metrics

import com.github.javaparser.ParserConfiguration
import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration
import com.github.javaparser.ast.body.InitializerDeclaration
import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.stmt.ForEachStmt
import com.github.javaparser.ast.stmt.ForStmt
import com.github.javaparser.ast.stmt.WhileStmt
import com.github.javaparser.ast.visitor.VoidVisitorAdapter
import com.github.javaparser.symbolsolver.JavaSymbolSolver
import com.github.javaparser.symbolsolver.resolution.typesolvers.ClassLoaderTypeSolver
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver
import com.github.javaparser.symbolsolver.resolution.typesolvers.JarTypeSolver
import com.github.javaparser.utils.SourceRoot
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.jvm.optionals.getOrNull

object ASTCalc {

    fun calc(sourcesDir: Path, jar: Path, metricsMap: MetricsMap) {
        println("\ncalculating AST metrics for $sourcesDir")

        val sourceRoot = SourceRoot(sourcesDir)
        val typeSolver = CombinedTypeSolver().apply {
            add(ClassLoaderTypeSolver(ASTCalc::class.java.classLoader))
            add(JarTypeSolver(jar))
        }
        val symbolResolver = JavaSymbolSolver(typeSolver)
        val parserConfiguration = ParserConfiguration().apply {
            setSymbolResolver(symbolResolver)
        }

//        val parseResults = sourceRoot.tryToParse("", symbolResolver)
        val callback = SourceRoot.Callback { _, _, parseResult ->
            val cu = parseResult.result.get()
            handleCompilationUnit(cu, metricsMap)
            SourceRoot.Callback.Result.DONT_SAVE
        }
        sourceRoot.parse("", parserConfiguration, callback)
    }

    private fun handleCompilationUnit(cu: CompilationUnit, metricsMap: MetricsMap) {
        val visitor = object : VoidVisitorAdapter<MetricsMap>() {
            override fun visit(n: MethodDeclaration, collector: MetricsMap) {
                super.visit(n, collector)

                var loopCount = 0
                n.accept(LoopVisitor()) { loopCount++ }

                println("${n.unifiedMethodDescriptor} = $loopCount")
            }

            // TODO: ctors
            override fun visit(n: InitializerDeclaration, arg: MetricsMap) {
                super.visit(n, arg)
                var loopCount = 0
                n.accept(LoopVisitor()) { loopCount++ }
                println("init! $n = $loopCount")
            }
        }
        cu.accept(visitor, metricsMap)
    }

    private val MethodDeclaration.unifiedMethodDescriptor: UnifiedMethodDescriptor
        get() {
            // TODO
            val declaringClass = this.parentNode.getOrNull() as? ClassOrInterfaceDeclaration
            val declaringClassFQN = declaringClass?.fullyQualifiedName?.getOrNull() ?: ""

            val methodName = this.nameAsString

            val paramsDescriptor = this.toDescriptor().trim { !(it == ')' || it == '(') }

            return UnifiedMethodDescriptor("$declaringClassFQN#$methodName$paramsDescriptor")
        }

    private class LoopVisitor : VoidVisitorAdapter<() -> Unit>() {
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
}
