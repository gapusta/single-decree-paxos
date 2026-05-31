package edu.myrza.paxos.dto

import kotlinx.serialization.Serializable

@Serializable
data class DtoPromiseRequest(
    val propose: String,
    val round: Long
)
