package edu.myrza.paxos.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class DtoPromiseResponse {
    @Serializable
    @SerialName("success")
    data class Success (
        val round: Long,
        val value: String?
    ): DtoPromiseResponse()

    @Serializable
    @SerialName("fail")
    data object Fail : DtoPromiseResponse()
}
