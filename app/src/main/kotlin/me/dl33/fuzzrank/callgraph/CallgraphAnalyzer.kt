package me.dl33.fuzzrank.callgraph

import me.dl33.fuzzrank.metrics.UnifiedMethodDescriptor
import sootup.callgraph.CallGraph
import sootup.callgraph.ClassHierarchyAnalysisAlgorithm
import sootup.core.inputlocation.AnalysisInputLocation
import sootup.java.bytecode.frontend.inputlocation.DefaultRuntimeAnalysisInputLocation
import sootup.java.bytecode.frontend.inputlocation.JavaClassPathAnalysisInputLocation
import sootup.java.core.views.JavaView
import java.nio.file.Path
import kotlin.io.path.absolutePathString
import kotlin.streams.asSequence

object CallgraphAnalyzer {

    fun applyStrategy(
        jar: Path,
        interestingMethods: List<UnifiedMethodDescriptor>,
        analysisStrategy: CallgraphAnalysisStrategy
    ): List<UnifiedMethodDescriptor> {
        val inputLocation: AnalysisInputLocation = JavaClassPathAnalysisInputLocation(jar.absolutePathString())
        val view = JavaView(inputLocation)
        DefaultRuntimeAnalysisInputLocation()


        val allMethods = view.classes.asSequence()
            .flatMap { it.methods }
            .filter { it.isConcrete }
            .map { it.signature }
            .toList()
        val cg = ClassHierarchyAnalysisAlgorithm(view).initialize(allMethods)

        return analysisStrategy.computeBestEntryPoints(cg, interestingMethods)
    }

}

interface CallgraphAnalysisStrategy {
    fun computeBestEntryPoints(
        callgraph: CallGraph,
        interestingMethods: List<UnifiedMethodDescriptor>,
    ): List<UnifiedMethodDescriptor>

    val name get() = this::class.simpleName!!
}
