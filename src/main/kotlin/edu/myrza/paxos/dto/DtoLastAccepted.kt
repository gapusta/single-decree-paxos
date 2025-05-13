package edu.myrza.paxos.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class DtoLastAccepted {
    @Serializable
    @SerialName("ok")
    data class Ok (
        val round: Long,
        val value: String?
    ): DtoLastAccepted()

    @Serializable
    @SerialName("fail")
    data object Fail : DtoLastAccepted()
}
