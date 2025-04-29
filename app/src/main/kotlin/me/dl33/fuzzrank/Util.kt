package me.dl33.fuzzrank

import kotlin.io.path.Path

data class IntCnt(var value: Int = 0)

class DepthCnt {
    var currentValue = 0
        private set
    var maxValue = 0
        private set

    fun stepIn() {
        currentValue++
        if (currentValue > maxValue) {
            maxValue = currentValue
        }
    }

    fun stepOut() {
        currentValue--
    }

    override fun toString() = "DepthCnt(maxValue=$maxValue)"
}

val PROJECT_DIR = Path(System.getProperty("projectDir")?.toString() ?: error("projectDir not set"))
