package edu.myrza.paxos.util

import java.util.concurrent.atomic.AtomicLong

object GlobalRoundGenerator {
    private val sequence = AtomicLong(0L)

    fun round() = sequence.incrementAndGet()
}
