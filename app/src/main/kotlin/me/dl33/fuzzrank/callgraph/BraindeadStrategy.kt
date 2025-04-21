package me.dl33.fuzzrank.callgraph

import me.dl33.fuzzrank.metrics.UnifiedMethodDescriptor
import me.dl33.fuzzrank.metrics.sootSignature
import me.dl33.fuzzrank.metrics.unifiedMethodDescriptor
import sootup.callgraph.CallGraph
import sootup.core.signatures.MethodSignature

/**
 * Simply return the interesting methods themselves.
 */
object BraindeadStrategy : CallgraphAnalysisStrategy {
    override fun computeBestEntryPoints(
        callgraph: CallGraph,
        interestingMethods: List<UnifiedMethodDescriptor>
    ): List<UnifiedMethodDescriptor> {
        return interestingMethods
    }
}

/**
 * Return the interesting methods themselves, except skip methods reachable from previous ones.
 */
object BraindeadWithSkippingStrategy : CallgraphAnalysisStrategy {
    override fun computeBestEntryPoints(
        callgraph: CallGraph,
        interestingMethods: List<UnifiedMethodDescriptor>
    ): List<UnifiedMethodDescriptor> {
        val output = mutableListOf<UnifiedMethodDescriptor>()
        val reachableMethods = mutableSetOf<MethodSignature>()

        // calculating all reachable methods from each method every time would be O(V^2)
        // but we don't need all, we only need not-yet-seen ones
        // so by checking already reached methods, we go down to O(E) time yay

        fun MethodSignature.updateReachableMethods() {
            if (this@updateReachableMethods in reachableMethods) return
            // plain old dfs
            val queue = ArrayDeque<MethodSignature>()
            queue.add(this@updateReachableMethods)
            while (queue.isNotEmpty()) {
                val method = queue.removeFirst()
                if (method in reachableMethods) continue
                reachableMethods.add(method)

                callgraph.callsFrom(method)
                    .map { it.targetMethodSignature }
                    .forEach { queue.add(it) }
            }
        }

        for (interestingMethod in interestingMethods) {
            if (interestingMethod.sootSignature in reachableMethods) {
                continue
            }
            output.add(interestingMethod)
            interestingMethod.sootSignature.updateReachableMethods()
        }
        return output
    }
}
