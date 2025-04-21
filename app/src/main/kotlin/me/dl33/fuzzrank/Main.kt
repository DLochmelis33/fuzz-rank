package me.dl33.fuzzrank

import me.dl33.fuzzrank.callgraph.BraindeadWithSkippingStrategy
import me.dl33.fuzzrank.callgraph.CallgraphAnalyzer
import me.dl33.fuzzrank.callgraph.MinCoverStrategy
import me.dl33.fuzzrank.metrics.Metrics
import me.dl33.fuzzrank.metrics.binAndRank
import java.io.File
import java.io.PrintStream
import kotlin.io.path.Path

fun main() {
//    val out = PrintStream(File("a.out").outputStream())
//    System.setOut(out)

    val thisProjectDir = Path(System.getProperty("projectDir")!!.toString())
    val javaProjectsDir = thisProjectDir.parent.resolve("javaProjects")

    val (source, jar) = snowflake_jdbc(javaProjectsDir)
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
}
