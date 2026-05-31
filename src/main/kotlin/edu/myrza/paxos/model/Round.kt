package edu.myrza.paxos.model

import kotlinx.serialization.Serializable

@Serializable
data class Round(
    val round: Long,
    val proposerId: Long
): Comparable<Round> {

    override fun compareTo(other: Round): Int {
        if (round != other.round) {
            return round.compareTo(other.round)
        }
        return proposerId.compareTo(other.proposerId)
    }

}