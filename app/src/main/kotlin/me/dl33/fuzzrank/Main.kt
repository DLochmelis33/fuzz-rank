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
}
