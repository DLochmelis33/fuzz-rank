package me.dl33.fuzzrank.metrics

import kotlinx.serialization.Serializable

/**
 * Uniquely describes a method in the following format:
 *
 * `classFQN::methodName(paramFQN, paramFQN)`
 *
 * In case of generic types and other nonsense where paramFQN is unavailable, anything goes :D
 */
@Serializable
@JvmInline
value class UnifiedMethodDescriptor(val fqnSig: String) {
    init {
        // not fool-proof, but better than nothing
        require(regex.matches(fqnSig)) { "$fqnSig does not match regex" }
    }

    override fun toString(): String = fqnSig

    companion object {
        private val regex = Regex(".+::.+\\(.*\\)")
    }
}
