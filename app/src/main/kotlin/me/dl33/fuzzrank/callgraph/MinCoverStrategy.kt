package me.dl33.fuzzrank.callgraph

import me.dl33.fuzzrank.metrics.UnifiedMethodDescriptor
import me.dl33.fuzzrank.metrics.sootSignature
import me.dl33.fuzzrank.metrics.unifiedMethodDescriptor
import sootup.callgraph.CallGraph
import sootup.core.signatures.MethodSignature
import java.util.BitSet

/**
 * Choose a minimal set of methods, from which all interesting are reachable.
 * This is NP-hard (see set-cover problem), so we use a simple greedy approximation for it.
 */
object MinCoverStrategy : CallgraphAnalysisStrategy {
    override fun computeBestEntryPoints(
        callgraph: CallGraph,
        interestingMethods: List<UnifiedMethodDescriptor>
    ): List<UnifiedMethodDescriptor> {
        // set all weights to 1.0 for unweighted set-cover
        return solveApproxSetCover(callgraph, interestingMethods) { 1.0 }
    }
}

object MinCoverWeightedStrategy : CallgraphAnalysisStrategy {
    override fun computeBestEntryPoints(
        callgraph: CallGraph,
        interestingMethods: List<UnifiedMethodDescriptor>
    ): List<UnifiedMethodDescriptor> {
        val costs = mutableMapOf<MethodSignature, Double>()
        val interestingSet = interestingMethods.map { it.sootSignature }.toSet()
        return solveApproxSetCover(callgraph, interestingMethods) { method ->
            costs[method]?.let { return@solveApproxSetCover it }
            // assumption: prob of reaching interesting ~ # of interesting / # of reachable
            // calculate both with bfs
            var interestingCnt = 0
            var reachableCnt = 0
            val queue = ArrayDeque<MethodSignature>(listOf(method))
            val visited = mutableSetOf<MethodSignature>()
            while (queue.isNotEmpty()) {
                val m = queue.removeFirst()
                if (m in visited) continue
                visited.add(m)
                reachableCnt++
                if (m in interestingSet) interestingCnt++
                callgraph.callsFrom(m).forEach { queue.add(it.targetMethodSignature) }
            }
            // let's put cost = 1 / prob
            val cost = reachableCnt.toDouble() / interestingCnt
            costs[method] = cost
            cost
        }
    }
}

// greedy weighted set-cover
private fun solveApproxSetCover(
    callgraph: CallGraph,
    interestingMethods: List<UnifiedMethodDescriptor>,
    costCalculator: (MethodSignature) -> Double,
): List<UnifiedMethodDescriptor> {

    val interestingSignatures = interestingMethods.map { it.sootSignature }
    val indexer = IndexMaker(interestingSignatures)

    // switching into graph naming:
    // graph G, vertices v, interesting = marked subset M, target set S
    // for each v we pre-calculate R(v): reachable marked nodes.
    // then we solve approx set-cover on Rs.

    // claim: node with predecessor is never in S
    // therefore there won't be too many candidates
    // therefore can make the straightforward impl
    // it will take O(# of candidates * |S| * set intersection)
    // by using bitsets we'll make set intersection O(1)

    // ...UNLESS we are in a giant loop OR last function is recursive
    // but honestly this will be so rare, and in these cases we just don't cover smth, no big deal

    // first calculate R(v) by dfs on reverse callgraph
    // TODO: how?... we have cycles in graph, simple dfs does not work?
    // i mean we can dfs from each vertex but then it's O(n^2) which is very unlikely to cut it
    // we can first find candidates, then dfs from each candidate...

    // step 1: find candidate nodes. use bfs on reverse G with init queue = M.
    val candidates = buildSet<MethodSignature> {
        val queue = ArrayDeque<MethodSignature>(interestingSignatures)
        val visited = mutableSetOf<MethodSignature>()
        while (queue.isNotEmpty()) {
            val v = queue.removeFirst()
            if (v in visited) continue
            visited.add(v)
            val prev = callgraph.callsTo(v)
            if (prev.isEmpty()) {
                this@buildSet.add(v)
                continue
            }
            for (call in prev) {
                queue.add(call.sourceMethodSignature)
            }
        }
    }

    // step 2: calculate R(c) for each candidate. use bitsets
    val candidatesReachableInteresting = buildMap<MethodSignature, BitSet> {
        // TODO: reuse info between candidate runs
        for (c in candidates) {
            val reachable = BitSet()
            val visited = mutableSetOf<MethodSignature>()
            val queue = ArrayDeque<MethodSignature>(listOf(c))
            while (queue.isNotEmpty()) {
                val v = queue.removeFirst()
                if (v in visited) continue
                visited.add(v)
                if (v in interestingSignatures) {
                    reachable[indexer.indexOf(v)] = true
                }
                val next = callgraph.callsFrom(v)
                for (call in next) {
                    queue.add(call.targetMethodSignature)
                }
            }
            this@buildMap.put(c, reachable)
        }
    }

    // step 3: greedy picking of candidate sets
    val bestCover = buildList<MethodSignature> {
        val uncovered = BitSet()
        interestingSignatures.forEach { s -> uncovered.set(indexer.indexOf(s)) }
        while (!uncovered.isEmpty) {
            val tmp = uncovered.clone() as BitSet
            var maxScore = 0.0
            var maxBitset: BitSet? = null
            var maxCandidate: MethodSignature? = null
            for ((c, bs) in candidatesReachableInteresting) {
                val cov = with(tmp) {
                    clear()
                    or(uncovered)
                    val before = cardinality()
                    andNot(bs) // removes covered nodes
                    val after = cardinality()
                    before - after
                }
                val score = cov / costCalculator(c)
                if (score > maxScore) {
                    maxScore = score
                    maxBitset = bs
                    maxCandidate = c
                }
            }
            if (maxScore < 1e-9) {
                // impossible to cover anything else, we have to stop
                println("warn: did not cover ${uncovered.cardinality()} interesting methods")
                return@buildList
                // likely reason: candidate not chosen (recursion)
            }
            uncovered.andNot(maxBitset!!)
            this@buildList.add(maxCandidate!!)
        }
    }

    // step 4: profit
    return bestCover.map { it.unifiedMethodDescriptor }
}
