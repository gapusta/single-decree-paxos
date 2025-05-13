package edu.myrza.paxos

import io.vertx.core.Future
import io.vertx.core.Vertx

/*
*  First approximation:
*   1. Acceptors and proposers will be separate objects (3 proposers and 3 acceptors)
*   2. First we will deploy acceptors and then proposers (proposers will start immediately when deployed)
*   3. Proposers will randomly choose 2 of the 3 acceptors and send them messages
*   4. Proposers and clients are blend together in our simulation. Proposers will keep
*   running Proposer.propose(value: String) until they succeed
*
*  Proposer.propose(String value):
*       1. prepare(N)
*       2. accept(Na, Va)
*
*  Acceptor:
*       1. callback to prepare
*       2. callback to accept
*
* */

// TODO: Failure handling

fun main() {
    val vertx = Vertx.vertx()

    val first = Acceptor(name = "A1")
    val second = Acceptor(name = "A2")
    val third = Acceptor(name = "A3")

    val proposer = Proposer(
        name = "P1",
        value = "Galya",
        acceptors = setOf(first.name, second.name, third.name)
    )

    Future.all(
        vertx.deployVerticle(first),
        vertx.deployVerticle(second),
        vertx.deployVerticle(third)
    ).onSuccess {
        vertx.deployVerticle(proposer)
    }
}
