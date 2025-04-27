package me.dl33.fuzzrank

import me.dl33.fuzzrank.callgraph.BraindeadWithSkippingStrategy
import me.dl33.fuzzrank.callgraph.CallgraphAnalyzer
import me.dl33.fuzzrank.callgraph.MinCoverStrategy
import me.dl33.fuzzrank.callgraph.MinCoverWeightedStrategy
import me.dl33.fuzzrank.metrics.ASTCalc
import me.dl33.fuzzrank.metrics.CFGCalc
import me.dl33.fuzzrank.metrics.Metrics
import me.dl33.fuzzrank.metrics.MetricsMap
import me.dl33.fuzzrank.metrics.binAndRank
import kotlin.io.path.Path
import kotlin.io.path.createParentDirectories
import kotlin.io.path.writeText
import kotlin.math.roundToInt

fun main() = mainnn(
    arrayOf(
        "assertj",
        "tmp/rankings",
        "C:/Users/dloch/prog/maga/thesis/fuzz-rank/dataset\\assertj-vavr-cd521160aa\\src\\main\\java",
        "C:/Users/dloch/prog/maga/thesis/fuzz-rank/dataset\\assertj-vavr-cd521160aa\\target\\classes",
        "0.1",
    )
)

fun mainnn(args: Array<String>) {
    val projectName = args[0]
    val workdir = Path(args[1])
    val sourcesDir = Path(args[2])
    val classesDir = Path(args[3])
    val topK = args[4].toDouble()

    val metricsMap = MetricsMap()
    val skipFQNs = setOf("org.traccar.protobuf", "net.snowflake.client.jdbc.internal")
    CFGCalc.calc(classesDir, metricsMap, skipFQNs)
    ASTCalc.calc(sourcesDir, classesDir, metricsMap, skipFQNs)

    val ranked = metricsMap.binAndRank().toList()
    val interesting = ranked.take((ranked.size * topK).roundToInt())

    val strategies = listOf(
        BraindeadWithSkippingStrategy,
        MinCoverStrategy,
        MinCoverWeightedStrategy,
    )
    val resultsStr = strategies.map { s ->
        val entryPoints = CallgraphAnalyzer.applyStrategy(classesDir, interesting.map { it.method }, s)
        """
        {
            "strategy": "${s.name}",
            "entryPoints": [${entryPoints.joinToString(",") { "\"$it\"" }}]
        }
        """.trim()
    }
    val finalJson = """
{
    "projectName": "$projectName",
    "topK": $topK,
    "methodsTotal": ${metricsMap.size},
    "methodsRanked": ${ranked.size},
    "results": [
        ${resultsStr.joinToString(",\n\t\t")}
    ]
}
    """.trimIndent()

    val outputFile = workdir.resolve("$projectName.json")
    outputFile.createParentDirectories()
    println("writing result into $outputFile")
    outputFile.writeText(finalJson)
}


fun maina() {
//    val out = PrintStream(File("a.out").outputStream())
//    System.setOut(out)

    val thisProjectDir = Path(System.getProperty("projectDir")!!.toString())
    val javaProjectsDir = thisProjectDir.parent.resolve("javaProjects")

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
