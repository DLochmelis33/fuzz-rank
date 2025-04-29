package me.dl33.fuzzrank

import kotlinx.serialization.Serializable
import me.dl33.fuzzrank.callgraph.SimpleWithSkippingStrategy
import me.dl33.fuzzrank.callgraph.CallgraphAnalyzer
import me.dl33.fuzzrank.callgraph.MinCoverStrategy
import me.dl33.fuzzrank.callgraph.MinCoverWeightedStrategy
import me.dl33.fuzzrank.callgraph.SimpleStrategy
import me.dl33.fuzzrank.metrics.*
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds

// ======= HYPERPARAMETERS =======

val topKs = listOf(0.01, 0.05, 0.1, 0.2)
val strategies = listOf(
    SimpleStrategy,
    SimpleWithSkippingStrategy,
    MinCoverStrategy,
    MinCoverWeightedStrategy,
)

// ===============================

fun main() {
    val datasetDescriptionFile = PROJECT_DIR.resolve("dataset/benchmarks.json")
    val dataset = readDataset(datasetDescriptionFile)
    val outputDir = PROJECT_DIR.resolve("experiments/results/rankings")
    outputDir.createDirectories()

    val doneProjectsNames = mutableSetOf<String>()
    for (project in dataset) {
        if (project.name in doneProjectsNames) continue

        println("project ${project.name}")
        try {
            val start = System.currentTimeMillis()
            val results = analyzeProject(
                classesDir = Path(project.bin),
                sourcesDir = Path(project.src),
            )
            val outputFile = outputDir.resolve(project.buildId + ".json")
            outputFile.writeText(json.encodeToString(results))

            doneProjectsNames.add(project.name)
            val end = System.currentTimeMillis()
            println(" successfully analyzed in ${(end - start).milliseconds}")

        } catch (e: Throwable) {
            println(" failed to analyze!")
            e.printStackTrace()
        }
    }
}

@Serializable
class AnalysisResult(
    val strategyName: String,
    val topK: Double,
    val entryPoints: List<UnifiedMethodDescriptor>,
)

fun analyzeProject(
    classesDir: Path,
    sourcesDir: Path,
): List<AnalysisResult> {

    val metricsMap = MetricsMap()
    val skipFQNs = setOf(
        "org.traccar.protobuf",
        "net.snowflake.client.jdbc.internal"
    )
    CFGCalc.calc(classesDir, metricsMap, skipFQNs)
    ASTCalc.calc(sourcesDir, classesDir, metricsMap, skipFQNs)

    val ranked = metricsMap.binAndRank().toList()

    return strategies.flatMap { s ->
        topKs.map { topK ->
            val interesting = ranked.take((ranked.size * topK).roundToInt())
            val entryPoints = CallgraphAnalyzer.applyStrategy(classesDir, interesting.map { it.method }, s)
            AnalysisResult(s.name, topK, entryPoints)
        }
    }
}
