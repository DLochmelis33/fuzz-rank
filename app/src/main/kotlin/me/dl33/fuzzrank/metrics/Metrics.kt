package me.dl33.fuzzrank.metrics

import java.nio.file.Path

data class Metrics(
    var cyclomatic: Int = MISSING_VALUE,
    var loopCount: Int = MISSING_VALUE,
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
 * `classFQN#methodName(paramFQN, paramFQN)`
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
