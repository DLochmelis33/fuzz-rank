package me.dl33.fuzzrank

import me.dl33.fuzzrank.metrics.Metrics
import kotlin.io.path.Path

fun main() {
    val thisProjectDir = Path(System.getProperty("projectDir")!!.toString())
    val javaProjectsDir = thisProjectDir.parent.resolve("javaProjects")
    val projectA = javaProjectsDir.resolve("a")

    val listSource = projectA.resolve("list/src/main/java")
    val listJar = projectA.resolve("list/build/libs/list.jar")

    val metricsMap = Metrics.calculate(listSource, listJar)
    metricsMap.forEach { method, metrics ->
        println("metrics for $method:")
        if (metrics.analysedCFG && metrics.analysedAST) {
            println(metrics)
        } else {
            val status = when {
                metrics.analysedCFG -> "CFG only"
                metrics.analysedAST -> "AST only"
                else -> "WTF"
            }
            println("INCOMPLETE: $status")
        }
        println()
    }

    val onlyAST = metricsMap
        .filterValues { it.analysedAST && !it.analysedCFG }
        .keys
        .joinToString("\n - ", prefix = " - ")
    val onlyCFG = metricsMap
        .filterValues { !it.analysedAST && it.analysedCFG }
        .keys
        .joinToString("\n - ", prefix = " - ")
    println("only AST:\n$onlyAST")
    println("only CFG:\n$onlyCFG")
}


