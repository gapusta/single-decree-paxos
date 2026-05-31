package edu.myrza.paxos.dto

import edu.myrza.paxos.model.Round
import kotlinx.serialization.Serializable

@Serializable
data class DtoPromiseRequest(
    val propose: String,
    val round: Round
)
