package edu.myrza.paxos.dto

import kotlinx.serialization.Serializable

@Serializable
data class DtoFailResponse(val phase: Phase, val type: Type) {
    enum class Phase {
        PROMISE,
        ACCEPT
    }
    enum class Type {
        PROMISED_HIGHER
    }

    companion object {
        fun promisedHigher(phase: Phase) = DtoFailResponse(phase = phase, type = Type.PROMISED_HIGHER)
    }
}
