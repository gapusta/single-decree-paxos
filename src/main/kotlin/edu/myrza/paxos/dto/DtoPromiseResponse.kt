package edu.myrza.paxos.dto

import edu.myrza.paxos.model.Round
import kotlinx.serialization.Serializable

@Serializable
data class DtoPromiseResponse (
    val accepted: Round,
    val value: String?
)
