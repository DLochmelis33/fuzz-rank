package me.dl33.fuzzrank.metrics

import sootup.core.inputlocation.AnalysisInputLocation
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

                val methodDesc = method.unifiedMethodDescriptor
                val metrics = metricsMap.getOrPut(methodDesc) { Metrics() }
                metrics.cyclomatic = cyclomaticComplexity
            }
        }
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
