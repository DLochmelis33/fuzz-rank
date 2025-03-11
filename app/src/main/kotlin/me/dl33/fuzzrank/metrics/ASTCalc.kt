package me.dl33.fuzzrank.metrics

import com.github.javaparser.ParserConfiguration
import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.Node
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
import me.dl33.fuzzrank.DepthCnt
import me.dl33.fuzzrank.IntCnt
import java.nio.file.Path
import kotlin.jvm.optionals.getOrNull
import kotlin.math.max

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
            override fun visit(n: MethodDeclaration, metricsMap: MetricsMap) {
                super.visit(n, metricsMap)
                visitMethodEntryPoint(n, n.unifiedMethodDescriptor, metricsMap)
            }

            // TODO: ctors
            override fun visit(n: InitializerDeclaration, arg: MetricsMap) {
                super.visit(n, arg)
                // TODO
            }

            private fun visitMethodEntryPoint(
                n: Node,
                methodDescriptor: UnifiedMethodDescriptor,
                metricsMap: MetricsMap
            ) {
                val metrics = metricsMap.getOrPut(methodDescriptor) { Metrics() }

                val loopAccumulator = LoopVisitor.Accumulator()
                n.accept(LoopVisitor() ,loopAccumulator)

                metrics.loopCount = loopAccumulator.totalCnt
                metrics.nestedLoopCount = loopAccumulator.nestedCnt
                // one loop ==> no nesting ==> 0 max nesting
                metrics.maxNestingOfLoops = max(0, loopAccumulator.depthCnt.maxValue - 1)

                println("$methodDescriptor => $loopAccumulator")
            }
        }
        cu.accept(visitor, metricsMap)
    }

    private class LoopVisitor : VoidVisitorAdapter<LoopVisitor.Accumulator>() {

        data class Accumulator(
            var totalCnt: Int = 0,
            var nestedCnt: Int = 0,
            val depthCnt: DepthCnt = DepthCnt(),
        )

        private inline fun visitLoopNode(acc: Accumulator, continueVisit: () -> Unit) {
            acc.totalCnt++
            if (acc.depthCnt.currentValue > 0) {
                acc.nestedCnt++
            }
            acc.depthCnt.stepIn()
            continueVisit()
            acc.depthCnt.stepOut()
        }

        override fun visit(n: ForStmt, acc: Accumulator) = visitLoopNode(acc) { super.visit(n, acc) }

        override fun visit(n: ForEachStmt, acc: Accumulator) = visitLoopNode(acc) { super.visit(n, acc) }

        override fun visit(n: WhileStmt, acc: Accumulator) = visitLoopNode(acc) { super.visit(n, acc) }
    }
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
