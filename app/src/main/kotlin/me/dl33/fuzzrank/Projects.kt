package me.dl33.fuzzrank

import java.nio.file.Path

fun a_list(projectsDir: Path): Pair<Path, Path> {
    val projectA = projectsDir.resolve("a")
    val listSource = projectA.resolve("list/src/main/java")
    val listJar = projectA.resolve("list/build/libs/list.jar")
    return listSource to listJar
}

fun a_kaprekar(projectsDir: Path): Pair<Path, Path> {
    val projectA = projectsDir.resolve("a")
    val listSource = projectA.resolve("kaprekar/src/main/java")
    val listJar = projectA.resolve("kaprekar/build/libs/kaprekar.jar")
    return listSource to listJar
}

fun cbor_java(projectsDir: Path): Pair<Path, Path> {
    val project = projectsDir.resolve("cbor-java")
    val source = project.resolve("src/main/java/")
    val jar = project.resolve("target/cbor-0.9.jar")
    return source to jar
}

fun word_wrap(projectsDir: Path): Pair<Path, Path> {
    val project = projectsDir.resolve("word-wrap")
    val source = project.resolve("src/main/java/")
    val jar = project.resolve("target/word-wrap-0.1.14-SNAPSHOT.jar")
    return source to jar
}

fun the_algorithms(projectsDir: Path): Pair<Path, Path> {
    val project = projectsDir.resolve("TheAlgorithms")
    val source = project.resolve("src/main/java/")
    val jar = project.resolve("target/Java-1.0-SNAPSHOT.jar")
    return source to jar
}

fun jsoup(projectsDir: Path): Pair<Path, Path> {
    val project = projectsDir.resolve("jsoup")
    val source = project.resolve("src/main/java/")
    val jar = project.resolve("target/jsoup-1.20.1-SNAPSHOT.jar")
    return source to jar
}

fun traccar(projectsDir: Path): Pair<Path, Path> {
    val project = projectsDir.resolve("traccar")
    val source = project.resolve("src/main/java/")
    val jar = project.resolve("target/tracker-server.jar")
    return source to jar
}

fun snowflake_jdbc(projectsDir: Path): Pair<Path, Path> {
    val project = projectsDir.resolve("snowflake-jdbc")
    val source = project.resolve("src/main/java/")
    val jar = project.resolve("target/snowflake-jdbc-thin.jar")
    return source to jar
}
