package me.dl33.fuzzrank

import me.dl33.fuzzrank.metrics.Metrics
import me.dl33.fuzzrank.metrics.binAndRank
import java.io.File
import java.io.PrintStream
import kotlin.io.path.Path

fun main() {
    val out = PrintStream(File("a.out").outputStream())
    System.setOut(out)

    val thisProjectDir = Path(System.getProperty("projectDir")!!.toString())
    val javaProjectsDir = thisProjectDir.parent.resolve("javaProjects")

    val (source, jar) = traccar(javaProjectsDir)
    val metricsMap = Metrics.calculate(source, jar, setOf("org.traccar.protobuf"))

    val onlyAST = metricsMap
        .filterValues { it.analysedAST && !it.analysedCFG }
        .keys
    val onlyCFG = metricsMap
        .filterValues { !it.analysedAST && it.analysedCFG }
        .keys
    println("\nonly AST:\n${onlyAST.joinToString("\n - ", prefix = " - ")}")
    println("\nonly CFG:\n${onlyCFG.joinToString("\n - ", prefix = " - ")}")

    val ranked = metricsMap.binAndRank().toList()
    println("\nranking of methods:")
    val rankedStr = ranked.joinToString("\n - ", prefix = " - ") { (method, metrics) ->
        "$method = (${metrics.complexityScore} / ${metrics.vulnerabilityScore})"
    }
//    println(rankedStr)

    println("\ntotal / ranked / only AST / only CFG = ${metricsMap.size} / ${ranked.size} / ${onlyAST.size} / ${onlyCFG.size}")
}


