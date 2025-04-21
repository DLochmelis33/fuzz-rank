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
                var maxCov = 0
                var maxCovBitset: BitSet? = null
                var maxCovCandidate: MethodSignature? = null
                for ((c, bs) in candidatesReachableInteresting) {
                    val cov = with(tmp) {
                        clear()
                        or(uncovered)
                        val before = cardinality()
                        andNot(bs) // removes covered nodes
                        val after = cardinality()
                        before - after
                    }
                    if (cov > maxCov) {
                        maxCov = cov
                        maxCovBitset = bs
                        maxCovCandidate = c
                    }
                }
                if (maxCov == 0) {
                    // impossible to cover anything else, we have to stop
                    println("warn: did not cover ${uncovered.cardinality()} interesting methods")
                    return@buildList
                    // likely reason: candidate not chosen (recursion)
                }
                uncovered.andNot(maxCovBitset!!)
                this@buildList.add(maxCovCandidate!!)
            }
        }

        // step 4: profit
        return bestCover.map { it.unifiedMethodDescriptor }
    }

}
