package me.dl33.fuzzrank

import me.dl33.fuzzrank.callgraph.CallgraphAnalyzer
import me.dl33.fuzzrank.callgraph.MinCoverStrategy
import me.dl33.fuzzrank.callgraph.MinCoverWeightedStrategy
import me.dl33.fuzzrank.metrics.Metrics
import me.dl33.fuzzrank.metrics.binAndRank
import kotlin.io.path.Path

fun main() {
//    val out = PrintStream(File("a.out").outputStream())
//    System.setOut(out)

    val javaProjectsDir = PROJECT_DIR.resolve("javaProjects")

    val (source, jar) = traccar(javaProjectsDir)

    val metricsMap = Metrics.calculate(
        source,
        jar,
        setOf("org.traccar.protobuf", "net.snowflake.client.jdbc.internal")
    )

    val onlyAST = metricsMap
        .filterValues { it.analysedAST && !it.analysedCFG }
        .keys
    val onlyCFG = metricsMap
        .filterValues { !it.analysedAST && it.analysedCFG }
        .keys
//    println("\nonly AST:\n${onlyAST.joinToString("\n - ", prefix = " - ")}")
//    println("\nonly CFG:\n${onlyCFG.joinToString("\n - ", prefix = " - ")}")

    val ranked = metricsMap.binAndRank().toList()
    val rankedStr = ranked.joinToString("\n - ", prefix = " - ") { (method, metrics) ->
        "$method = (${metrics.complexityScore} / ${metrics.vulnerabilityScore})"
    }
//    println("\nranking of methods:\n$rankedStr")

    println("\ntotal / ranked / only AST / only CFG = ${metricsMap.size} / ${ranked.size} / ${onlyAST.size} / ${onlyCFG.size}")

    val interesting = ranked.take(ranked.size / 10).map { it.method }
    println("interesting size: ${interesting.size}")

    val strategicEntryPoints = CallgraphAnalyzer.applyStrategy(jar, interesting, MinCoverStrategy)
    println("strategic size: ${strategicEntryPoints.size}")
//    println(strategicEntryPoints.joinToString("\n"))

    val a = CallgraphAnalyzer.applyStrategy(jar, interesting, MinCoverStrategy)
    val b = CallgraphAnalyzer.applyStrategy(jar, interesting, MinCoverWeightedStrategy)
    val diff = a.union(b).minus(a.intersect(b))
    println("a / b / diff = ${a.size} / ${b.size} / ${diff.size} ($diff)")
}
