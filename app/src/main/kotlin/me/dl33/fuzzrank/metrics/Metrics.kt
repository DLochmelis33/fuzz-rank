package me.dl33.fuzzrank.metrics

import java.nio.file.Path

/**
 * @param cyclomatic Standard cyclomatic complexity.
 * @param loopCount How many loops.
 * @param nestedLoopCount How many nested loops.
 * @param maxNestingOfLoops Maximum nesting level of loops.
 *
 * @param parameters Number of parameters of this function.
 * @param calleeParameters Total number of parameters of other functions called from this function.
 * @param nestedControlStructures The number of nested control structures pairs
 * (as in, count each pair where one structure is nested in the other).
 * @param maxNestingOfControlStructures Maximum nesting level of control structures.
 * @param maxControlDependentControlStructures ????? not explained in Leopard TODO
 * @param maxDataDependentControlStructures ????? anything that depends on function params OR captured vars?
 * also what does 'max' mean? does poisoning of expressions count? TODO
 * @param ifWithoutElseCount How many `if`-s are missing `else`.
 * @param variablesInControlPredicates How many of the declared variables are used in predicates of control structures.
 */
data class Metrics(
    // ============= complexity metrics =============

    var cyclomatic: Int = MISSING_VALUE,
    var loopCount: Int = MISSING_VALUE,
    var nestedLoopCount: Int = MISSING_VALUE,
    var maxNestingOfLoops: Int = MISSING_VALUE,

    // ============= vulnerability metrics =============

    var parameters: Int = MISSING_VALUE,
    var calleeParameters: Int = MISSING_VALUE,
    // skipping pointer arithmetic
    var nestedControlStructures: Int = MISSING_VALUE,
    var maxNestingOfControlStructures: Int = MISSING_VALUE,
    var maxControlDependentControlStructures: Int = MISSING_VALUE,
    var maxDataDependentControlStructures: Int = MISSING_VALUE,
    var ifWithoutElseCount: Int = MISSING_VALUE,
    var variablesInControlPredicates: Int = MISSING_VALUE,
) {
    companion object {
        const val MISSING_VALUE = -1

        fun calculate(sourcesDir: Path, jar: Path): MetricsMap {
            val metricsMap = MetricsMap()
            CFGCalc.calc(jar, metricsMap)
            ASTCalc.calc(sourcesDir, jar, metricsMap)
            return metricsMap
        }
    }
}

/**
 * Uniquely describes a method in the following format:
 *
 * `classFQN#methodName(paramSignature, paramSignature)`
 */
@JvmInline
value class UnifiedMethodDescriptor(val fqnSig: String) {
    init {
        // not fool-proof, but better than nothing
        require(regex.matches(fqnSig)) { "$fqnSig does not match regex" }
    }

    override fun toString(): String = fqnSig

    companion object {
        private val regex = Regex(".+#.+\\(.*\\)")
    }
}

typealias MetricsMap = MutableMap<UnifiedMethodDescriptor, Metrics>

fun MetricsMap(): MetricsMap = mutableMapOf()
