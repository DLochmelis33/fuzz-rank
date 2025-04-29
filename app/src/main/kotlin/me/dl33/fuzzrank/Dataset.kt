package me.dl33.fuzzrank

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.nio.file.Path
import kotlin.io.path.readText

val json = Json {
    ignoreUnknownKeys = true
    prettyPrint = true
}

@Serializable
class DatasetProject(
    val name: String,
    val root: String,
    @SerialName("build_id")
    val buildId: String,
    val src: String,
    val bin: String,
    val classPath: List<String>,
)

fun readDataset(descriptionFile: Path): List<DatasetProject> {
    return json.decodeFromString<List<DatasetProject>>(descriptionFile.readText())
}
