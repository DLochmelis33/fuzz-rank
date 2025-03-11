package me.dl33.fuzzrank

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
