package edu.myrza.paxos.dto

import kotlinx.serialization.Serializable

@Serializable
data class DtoAcceptResponse(
    val status: Status
) {
    enum class Status {
        SUCCESS,
        FAIL
    }

    companion object {
        fun success() = DtoAcceptResponse(status = Status.SUCCESS)

        fun fail() = DtoAcceptResponse(status = Status.FAIL)
    }
}
