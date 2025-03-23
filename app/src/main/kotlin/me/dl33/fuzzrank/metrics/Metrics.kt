package me.dl33.fuzzrank.metrics

import java.nio.file.Path
import kotlin.reflect.KProperty
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
        const val MISSING_VALUE = -1

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

typealias MetricsMap = MutableMap<UnifiedMethodDescriptor, Metrics>

fun MetricsMap(): MetricsMap = mutableMapOf()
