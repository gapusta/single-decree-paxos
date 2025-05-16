package edu.myrza.paxos

import io.vertx.core.Future
import io.vertx.core.Vertx

// TODO: Graceful shutdown
// TODO: id generator per Proposer?

fun main() {
    val vertx = Vertx.vertx()

    val acceptors = listOf(
        Acceptor(name = "A1"),
        Acceptor(name = "A2"),
        Acceptor(name = "A3"),
    )

    val acceptorNames = acceptors.map { it.name }.toSet()
    val proposers = listOf(
        Proposer(name = "P1", value = "P1Value", acceptors = acceptorNames),
        Proposer(name = "P2", value = "P2Value", acceptors = acceptorNames),
//        Proposer(name = "P3", value = "P3Value", acceptors = acceptorNames)
    )

    Future.all(
        acceptors.map { vertx.deployVerticle(it) }
    ).onSuccess {
        proposers.map { vertx.deployVerticle(it) }
    }
}
