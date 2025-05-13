package edu.myrza.paxos.dto

import kotlinx.serialization.Serializable

@Serializable
data class DtoAccept(
    val round: Long,
    val value: String
)
