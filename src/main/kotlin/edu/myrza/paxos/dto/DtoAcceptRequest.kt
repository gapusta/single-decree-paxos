package edu.myrza.paxos.dto

import kotlinx.serialization.Serializable

@Serializable
data class DtoAcceptRequest(
    val round: Long,
    val value: String
)
