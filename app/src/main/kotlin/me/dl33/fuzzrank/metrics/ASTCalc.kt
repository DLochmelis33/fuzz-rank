package me.dl33.fuzzrank.metrics

import com.github.javaparser.ParserConfiguration
import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.ImportDeclaration
import com.github.javaparser.ast.Node
import com.github.javaparser.ast.NodeList
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration
import com.github.javaparser.ast.body.ConstructorDeclaration
import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.body.Parameter
import com.github.javaparser.ast.expr.ConditionalExpr
import com.github.javaparser.ast.expr.MethodCallExpr
import com.github.javaparser.ast.expr.NameExpr
import com.github.javaparser.ast.expr.SwitchExpr
import com.github.javaparser.ast.stmt.*
import com.github.javaparser.ast.visitor.VoidVisitorAdapter
import com.github.javaparser.resolution.UnsolvedSymbolException
import com.github.javaparser.resolution.types.ResolvedType
import com.github.javaparser.symbolsolver.JavaSymbolSolver
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver
import com.github.javaparser.symbolsolver.resolution.typesolvers.JarTypeSolver
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver
import com.github.javaparser.utils.SourceRoot
import me.dl33.fuzzrank.DepthCnt
import me.dl33.fuzzrank.IntCnt
import java.nio.file.Path
import java.util.Optional
import kotlin.jvm.optionals.getOrNull
import kotlin.math.max

object ASTCalc {

    fun calc(sourcesDir: Path, jar: Path, metricsMap: MetricsMap) {
        println("\ncalculating AST metrics for $sourcesDir")

        val sourceRoot = SourceRoot(sourcesDir)
        val typeSolver = CombinedTypeSolver().apply {
            add(ReflectionTypeSolver())
            add(JarTypeSolver(jar))
        }
        val symbolResolver = JavaSymbolSolver(typeSolver)
        val parserConfiguration = ParserConfiguration()
            .setSymbolResolver(symbolResolver)
            .setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_21)

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

                if (n.isAbstract) return

                val descriptor = n.unifiedMethodDescriptor
                val metrics = metricsMap.getOrPut(descriptor) { Metrics() }

                metrics.parameters = n.parameters.size

                visitMethodEntryPoint(n, metrics)
            }

            // TODO: ctors
            override fun visit(n: ConstructorDeclaration, metricsMap: MetricsMap) {
                super.visit(n, metricsMap)
                val descriptor = n.unifiedMethodDescriptor
                val metrics = metricsMap.getOrPut(descriptor) { Metrics() }

                metrics.parameters = n.parameters.size

                visitMethodEntryPoint(n, metrics)
            }

            private fun visitMethodEntryPoint(
                n: Node,
                metrics: Metrics
            ) {
                val loopAccumulator = LoopVisitor.Accumulator()
                n.accept(LoopVisitor(), loopAccumulator)

                // CD2: loop structures
                metrics.loopCount = loopAccumulator.totalCnt
                metrics.nestedLoopCount = loopAccumulator.nestedCnt
                // one loop ==> no nesting ==> 0 max nesting
                metrics.maxNestingOfLoops = max(0, loopAccumulator.depthCnt.maxValue - 1)

                // VD1: dependency
                val calleeParametersCnt = IntCnt()
                n.accept(InvocationVisitor(), calleeParametersCnt)
                metrics.calleeParameters = calleeParametersCnt.value

                // VD3: control structures
                val controlAcc = ControlStructVisitor.Accumulator()
                n.accept(ControlStructVisitor(), controlAcc)
                metrics.nestedControlStructuresPairs = controlAcc.totalNestedPairsCnt
                metrics.maxNestingOfControlStructures = max(0, controlAcc.depth.maxValue - 1)

                // cheating: in 90% of cases control structures are data-dependent
                // exceptions to this rule are like `for(direction in Direction.values())`
                // so for now let's consider all structures control-dependent
                metrics.maxDataDependentControlStructures = controlAcc.totalControlStmts
                // TODO: I think we can calculate it for real without a full dataflow analysis
                // thanks to SootUp `stmt.getUses()`

                metrics.ifWithoutElseCount = controlAcc.ifWithoutElseCnt
                metrics.variablesInControlPredicates = controlAcc.variablesInPredicates.size
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

        override fun visit(n: DoStmt, acc: Accumulator) = visitLoopNode(acc) { super.visit(n, acc) }
    }

    class InvocationVisitor : VoidVisitorAdapter<IntCnt>() {
        override fun visit(n: MethodCallExpr, arg: IntCnt) {
            arg.value += n.arguments.size
            super.visit(n, arg)
        }
    }

    class ControlStructVisitor : VoidVisitorAdapter<ControlStructVisitor.Accumulator>() {

        /*
        Java control structures are:
        - if / else / else if
        - _ ? _ : _
        - switch
        - for / foreach / while / do while
        - break / continue
         */

        data class Accumulator(
            var totalNestedPairsCnt: Int = 0,
            val depth: DepthCnt = DepthCnt(),
            var ifWithoutElseCnt: Int = 0,
            val variablesInPredicates: MutableSet<Node> = mutableSetOf(),
            var totalControlStmts: Int = 0,
        ) {
            fun updateNestedCnt() {
                totalControlStmts++
                // we are calculating pairs, so for each struct we add all outer structs
                val depth = depth.currentValue
                if (depth > 0) {
                    totalNestedPairsCnt += depth
                }
            }
        }

        private val varVisitor = VariableAccessVisitor()

        override fun visit(n: IfStmt, acc: Accumulator) {
            if (!n.hasElseBlock()) acc.ifWithoutElseCnt++

            acc.updateNestedCnt()

            n.condition.accept(varVisitor, acc.variablesInPredicates)

            acc.depth.stepIn()
            super.visit(n, acc)
            acc.depth.stepOut()
        }

        override fun visit(n: ConditionalExpr, acc: Accumulator) {
            acc.updateNestedCnt()

            n.condition.accept(varVisitor, acc.variablesInPredicates)

            acc.depth.stepIn()
            super.visit(n, acc)
            acc.depth.stepOut()
        }

        override fun visit(n: SwitchStmt, acc: Accumulator) {
            acc.updateNestedCnt()

            n.selector.accept(varVisitor, acc.variablesInPredicates)

            acc.depth.stepIn()
            super.visit(n, acc)
            acc.depth.stepOut()
        }

        // wow java has these?
        override fun visit(n: SwitchExpr, acc: Accumulator) {
            acc.updateNestedCnt()

            n.selector.accept(varVisitor, acc.variablesInPredicates)

            acc.depth.stepIn()
            super.visit(n, acc)
            acc.depth.stepOut()
        }

        private fun visitUnconditionalNode(acc: Accumulator, continueVisit: () -> Unit) {
            acc.updateNestedCnt()
            acc.depth.stepIn()
            continueVisit()
            acc.depth.stepOut()
        }

        override fun visit(n: ForStmt, acc: Accumulator) = visitUnconditionalNode(acc) { super.visit(n, acc) }
        override fun visit(n: ForEachStmt, acc: Accumulator) = visitUnconditionalNode(acc) { super.visit(n, acc) }
        override fun visit(n: WhileStmt, acc: Accumulator) = visitUnconditionalNode(acc) { super.visit(n, acc) }
        override fun visit(n: DoStmt, acc: Accumulator) = visitUnconditionalNode(acc) { super.visit(n, acc) }

        override fun visit(n: BreakStmt, acc: Accumulator) = visitUnconditionalNode(acc) { super.visit(n, acc) }
        override fun visit(n: ContinueStmt, acc: Accumulator) = visitUnconditionalNode(acc) { super.visit(n, acc) }
    }

    class VariableAccessVisitor : VoidVisitorAdapter<MutableSet<Node>>() {
        // variables are "names"
        override fun visit(n: NameExpr, nodes: MutableSet<Node>) {
            super.visit(n, nodes)
            try {
                val decl = n.resolve()
                // apparently Leopard considers parameters as variables, see example in their paper
                if (decl.isVariable || decl.isParameter) {
                    val astDecl = decl.toAst().getOrNull()
                    if (astDecl != null) nodes.add(astDecl)
                }
            } catch (_: UnsolvedSymbolException) {
            }
        }
    }
}

private fun makeDescriptor(
    parentNode: Optional<Node>,
    methodName: String,
    parameters: NodeList<Parameter>,
): UnifiedMethodDescriptor {
    val declaringClass = parentNode.getOrNull() as? ClassOrInterfaceDeclaration
    val declaringClassFQN = declaringClass?.fullyQualifiedName?.getOrNull() ?: "<unknown>"

    val paramsString = parameters.joinToString(", ") {
        try {
            val type = it.resolve().type

            if (type.isTypeVariable) {
                val tp = type.asTypeParameter()
                when {
                    tp.isUnbounded -> "java.lang.Object"
                    tp.hasUpperBound() -> tp.upperBound.describeOptimized()
                    else -> "<generic>"
                }
            } else {
                type.describeOptimized()
            }
        } catch (_: UnsolvedSymbolException) {
            // method probably uses something from a library not included in the JAR, nothing we can do
            "<unresolved>"
        }
    }

    return UnifiedMethodDescriptor("$declaringClassFQN::$methodName($paramsString)")
}

private val MethodDeclaration.unifiedMethodDescriptor: UnifiedMethodDescriptor
    get() = makeDescriptor(parentNode, nameAsString, parameters)

private val ConstructorDeclaration.unifiedMethodDescriptor: UnifiedMethodDescriptor
    get() = makeDescriptor(parentNode, "<init>", parameters)

private fun ResolvedType.describeOptimized() = describe()
    // in bytecode...
    .takeWhile { it != '<' } // ...generics will be erased
    .replace("...", "[]") // ...variadics will be arrays
