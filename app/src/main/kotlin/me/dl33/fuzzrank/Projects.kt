package me.dl33.fuzzrank

import java.nio.file.Path

fun a_list(projectsDir: Path): Pair<Path, Path> {
    val projectA = projectsDir.resolve("a")
    val listSource = projectA.resolve("list/src/main/java")
    val listJar = projectA.resolve("list/build/libs/list.jar")
    return listSource to listJar
}

fun cbor_java(projectsDir: Path): Pair<Path, Path> {
    val project = projectsDir.resolve("cbor-java")
    val source = project.resolve("src/main/java/")
    val jar = project.resolve("target/cbor-0.9.jar")
    return source to jar
}
