package me.dl33.fuzzrank.metrics

import java.nio.file.Path
import kotlin.collections.indices
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor

/**
 * @param cyclomatic Standard cyclomatic complexity.
 * @param loopCount How many loops.
 * @param nestedLoopCount How many nested loops.
 * @param maxNestingOfLoops Maximum nesting level of loops.
 *
 * @param parameters Number of parameters of this function.
 * TODO: "the number of variables prepared by the function as parameters of function calls" is is this same?
 * @param calleeParameters Total number of parameters of other functions called from this function.
 * TODO: currently does not include constructors
 * @param nestedControlStructuresPairs The number of nested control structures **pairs**
 * (as in, count each pair where one structure is nested in the other).
 * @param maxNestingOfControlStructures Maximum nesting level of control structures.
 * @param maxControlDependentControlStructures TODO ????? not explained in Leopard
 * @param maxDataDependentControlStructures TODO ????? anything that depends on function params OR captured vars?
 * also what does 'max' mean? does poisoning of expressions count?
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
    var nestedControlStructuresPairs: Int = MISSING_VALUE,
    var maxNestingOfControlStructures: Int = MISSING_VALUE,
    var maxControlDependentControlStructures: Int = MISSING_VALUE,
    var maxDataDependentControlStructures: Int = MISSING_VALUE,
    var ifWithoutElseCount: Int = MISSING_VALUE,
    var variablesInControlPredicates: Int = MISSING_VALUE,
) {
    companion object {
        const val MISSING_VALUE = -1_000_000 // not too much to overflow, but for sure more than any metric

        fun calculate(sourcesDir: Path, jar: Path): MetricsMap {
            val metricsMap = MetricsMap()
            CFGCalc.calc(jar, metricsMap)
            ASTCalc.calc(sourcesDir, jar, metricsMap)
            return metricsMap
        }
    }


    override fun toString(): String {
        val propNames = this::class.primaryConstructor!!.parameters.map { it.name }.toSet()
        val primaryProps = this::class.memberProperties.filter { it.name in propNames }
        return primaryProps.joinToString("\n - ", prefix = " - ") { prop ->
            "${prop.name} = ${prop.getter.call(this)}"
        }
    }

    val analysedCFG get() = cyclomatic != MISSING_VALUE
    val analysedAST get() = parameters != MISSING_VALUE

    val complexityScore
        get() = (cyclomatic + loopCount + nestedLoopCount + maxNestingOfLoops)
            .takeUnless { it < 0 } ?: error("some metrics for complexity score are missing!\n$this")

    val vulnerabilityScore
        get() = (parameters
                + calleeParameters
                + nestedControlStructuresPairs
                + maxNestingOfControlStructures
                + maxControlDependentControlStructures
                + maxDataDependentControlStructures
                + ifWithoutElseCount
                + variablesInControlPredicates)
            .takeUnless { it < 0 }
            ?: error("some metrics for vulnerability score are missing!\n$this")
}

/**
 * Uniquely describes a method in the following format:
 *
 * `classFQN::methodName(paramFQN, paramFQN)`
 *
 * In case of generic types and other nonsense where paramFQN is unavailable, anything goes :D
 */
@JvmInline
value class UnifiedMethodDescriptor(val fqnSig: String) {
    init {
        // not fool-proof, but better than nothing
        require(regex.matches(fqnSig)) { "$fqnSig does not match regex" }
    }

    override fun toString(): String = fqnSig

    companion object {
        private val regex = Regex(".+::.+\\(.*\\)")
    }
}

data class MethodWithMetrics(val method: UnifiedMethodDescriptor, val metrics: Metrics)

typealias MetricsMap = MutableMap<UnifiedMethodDescriptor, Metrics>

fun MetricsMap(): MetricsMap = mutableMapOf()

/**
 * @return list of [MethodWithMetrics], ordered with most vulnerable methods first.
 */
fun MetricsMap.binAndRank(): Sequence<MethodWithMetrics> {

    val binning = this
        .filter { (_, metrics) -> metrics.analysedCFG && metrics.analysedAST }
        .map { MethodWithMetrics(it.key, it.value) }
        .groupBy { (_, metrics) -> metrics.complexityScore }
        .mapValues { (_, group) -> group.sortedByDescending { (_, metrics) -> metrics.vulnerabilityScore } }

    // just like the paper says:
    // emit the first function from each bin, then second from each, and so on
    // treat functions with same score equally, so maybe >1 function from bin per round
    return sequence {
        val ptrs = MutableList(binning.size) { 0 }
        val bins = binning.entries.sortedBy { it.key }.map { it.value }

        rounds@ for (k in 1..Int.MAX_VALUE) {
            // k is not necessary, it just indicates round number
            var yielded = false
            bins@ for (i in bins.indices) {
                val bin = bins[i]
                // repeat only if next method has same score
                sameScore@ while (ptrs[i] in bin.indices) {
                    val result = bin[ptrs[i]]
                    yield(result)
                    yielded = true
                    ptrs[i]++
                    if (ptrs[i] in bin.indices) {
                        val nextResult = bin[ptrs[i]]
                        if (nextResult.metrics.vulnerabilityScore == result.metrics.vulnerabilityScore) {
                            continue@sameScore
                        }
                    }
                    break@sameScore
                }
            }
            if (!yielded) {
                break@rounds
            }
        }
    }
}
