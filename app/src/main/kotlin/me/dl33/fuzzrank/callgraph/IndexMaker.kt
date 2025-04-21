package me.dl33.fuzzrank.callgraph

class IndexMaker<T>(objects: Collection<T>) {

    private var cnt = 0
    private val i2o = mutableMapOf<T, Int>()
    private val o2i = mutableMapOf<Int, T>()

    init {
        for (o in objects) {
            cnt++
            i2o[o] = cnt
            o2i[cnt] = o
        }
    }

    fun indexOf(o: T): Int = i2o[o] ?: error("object $o not indexed")

    fun indexOfOrPut(o: T): Int = i2o.getOrPut(o) { cnt++ }

    fun toObject(i: Int): T = o2i[i] ?: error("index $i not associated")
}
