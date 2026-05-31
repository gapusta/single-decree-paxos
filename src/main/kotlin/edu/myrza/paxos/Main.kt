package edu.myrza.paxos

import edu.myrza.paxos.util.ShutdownHandler
import io.vertx.core.Future
import io.vertx.core.Vertx

fun main() {
    val vertx = Vertx.vertx()

    val acceptors = listOf(
        VerticleAcceptor(name = "A1"),
        VerticleAcceptor(name = "A2"),
        VerticleAcceptor(name = "A3"),
    )

    val acceptorNames = acceptors.map { it.name }.toSet()
    val proposers = listOf(
        VerticleProposer(id = 1, value = "P1's value", acceptors = acceptorNames),
        VerticleProposer(id = 2, value = "P2's value", acceptors = acceptorNames),
        VerticleProposer(id = 3, value = "P3's value", acceptors = acceptorNames)
    )

    Future.all<Future<String>>(
        acceptors.map { vertx.deployVerticle(it) }
    ).onSuccess {
        proposers.forEach { vertx.deployVerticle(it) }
    }

    // shutdown handler
    Runtime.getRuntime().addShutdownHook(ShutdownHandler(vertx))
}
