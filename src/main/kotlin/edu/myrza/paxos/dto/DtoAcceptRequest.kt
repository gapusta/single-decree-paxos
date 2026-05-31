package edu.myrza.paxos.dto

import kotlinx.serialization.Serializable

@Serializable
data class DtoAcceptRequest(
    val proposer: String,
    val round: Long,
    val value: String
)
