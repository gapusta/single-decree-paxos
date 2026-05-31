package edu.myrza.paxos.dto

import edu.myrza.paxos.model.Round
import kotlinx.serialization.Serializable

@Serializable
data class DtoAcceptRequest(
    val proposer: String,
    val round: Round,
    val value: String
)
