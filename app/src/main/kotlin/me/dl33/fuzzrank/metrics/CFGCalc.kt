package me.dl33.fuzzrank.metrics

import sootup.core.graph.BasicBlock
import sootup.core.inputlocation.AnalysisInputLocation
import sootup.core.jimple.common.stmt.JIfStmt
import sootup.core.jimple.common.stmt.Stmt
import sootup.core.types.ArrayType
import sootup.core.types.ClassType
import sootup.core.types.PrimitiveType
import sootup.core.types.Type
import sootup.java.bytecode.inputlocation.JavaClassPathAnalysisInputLocation
import sootup.java.core.JavaSootMethod
import sootup.java.core.views.JavaView
import java.nio.file.Path
import kotlin.io.path.absolutePathString

object CFGCalc {
    fun calc(jar: Path, metricsMap: MetricsMap) {
        println("\ncalculating CFG metrics for $jar")
        val inputLocation: AnalysisInputLocation = JavaClassPathAnalysisInputLocation(jar.absolutePathString())
        val view = JavaView(inputLocation)

        val allClasses = view.classes
        for (sootClass in allClasses) {
            println("analysing $sootClass")
            for (method in sootClass.methods) {

                val methodDesc = method.unifiedMethodDescriptor
                val metrics = metricsMap.getOrPut(methodDesc) { Metrics() }
                with(metrics) {
                    cyclomatic = calcCyclomaticComplexity(method)
                    maxControlDependentControlStructures = calcControlDependentControlStructures(method)
                    maxDataDependentControlStructures = calcDataDependentControlStructures(method)
                }
            }
        }
    }

    private fun calcCyclomaticComplexity(method: JavaSootMethod): Int {
        val stmtGraph = method.body.stmtGraph

        // cyclomatic complexity = #edges - #nodes + #components
        // sootup guarantees 1 component per method body CANNOT FUCKING CONFIRM I LOST ITTTTT
        // that makes sense at least
        // but wiki says +2
        // and checking it for paths manually confirms +2

        var E = 0
        var N = 0
        stmtGraph.blocks.forEach { stmt ->
            E += stmt.successors.size
            N++
        }
        return E - N + 2
    }

    private fun calcControlDependentControlStructures(method: JavaSootMethod): Int {
        // select a control structure node from graph and throw it out
        // run DFS trying to reach a terminal node
        // if possible, then there exists a path that avoids selected node ==> it is control-dependent
        // if not, then this node is unavoidable ==> it is not control-dependent
        // (technically also there are unreachable nodes, they are not dependent, but they are dead code anyway)

        val stmtGraph = method.body.stmtGraph
        val controlStmts = mutableSetOf<Stmt>()
        for (stmt in stmtGraph.stmts) {
            // "control structure" <==> more than one successor
            if (stmt.expectedSuccessorCount > 1) controlStmts += stmt
        }
        var controlDependentControlStmts = 0
        for (selectedStmt in controlStmts) {
            // dfs
            var stack = ArrayDeque<Stmt>().apply { add(stmtGraph.startingStmt) }
            val visited = mutableSetOf<Stmt>()
            dfs@ while (stack.isNotEmpty()) {
                val currentStmt = stack.removeFirst()
                visited += currentStmt
                val nextStmts = stmtGraph.successors(currentStmt)
                if (nextStmts.isEmpty()) {
                    // terminal node reached, selectedStmt is control-dependent
                    controlDependentControlStmts++
                    break@dfs
                }
                next@ for (nextStmt in nextStmts) {
                    if (nextStmt in visited) continue@next
                    if (nextStmt == selectedStmt) continue@next
                    stack.addLast(nextStmt)
                }
            }
        }
        return controlDependentControlStmts
    }

    private fun calcDataDependentControlStructures(method: JavaSootMethod): Int {
        // I think we need a "taint analysis" for this...
        // ... but we have `stmt.getUses()`, which we can repeatedly use, and this should be enough!
        // NOOOO `uses` means "which Value-s this expression uses"
        // still this is probably still doable if we first associate all nodes with its uses... meh?
        // TODO: that

//        val paramVars = method.body.parameterLocals
//        val a = paramVars.first()
//        method.body.stmts.first() as JIfStmt

//        val stmtGraph = method.body.stmtGraph
        return Metrics.MISSING_VALUE
    }

    private val JavaSootMethod.unifiedMethodDescriptor: UnifiedMethodDescriptor
        get() {
            // TODO
            val classFQN = this.declaringClassType.fullyQualifiedName
            val methodName = this.name

            val parameterTypes = this.signature.parameterTypes
            val parameterString = parameterTypes.joinToString(", ") { type ->
                type.unifiedName
            }

            return UnifiedMethodDescriptor("$classFQN::$methodName($parameterString)".replace("$", "."))
        }

    private val Type.unifiedName: String
        get() {
            // on CFG level generics are fortunately gone
            // OTOH they have to match AST naming BRUH
            // in bytecode generics are reduced to VARIOUS objects (ex. extends String)
            return when (this) {
                is ClassType -> fullyQualifiedName
                is PrimitiveType -> name
                is ArrayType -> elementType.unifiedName + "[]"
                else -> TODO("not yet supported type $this")
            }
        }
}
