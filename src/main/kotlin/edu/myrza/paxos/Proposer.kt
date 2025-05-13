package edu.myrza.paxos

import edu.myrza.paxos.dto.DtoAccept
import edu.myrza.paxos.dto.DtoLastAccepted
import io.vertx.core.AbstractVerticle
import io.vertx.core.CompositeFuture
import io.vertx.core.Future
import io.vertx.core.eventbus.Message
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class Proposer(
    private val name: String,
    private val value: String,
    private val acceptors: Set<String>
): AbstractVerticle() {

    override fun start() {
        val eb = vertx.eventBus()
        val round = round()

        val majority = acceptors.size / 2 + 1
        val acceptors = acceptors.shuffled().take(majority)

        acceptors
            .map {
                println("Proposer $name ask promise from Acceptor $it [ N: $round]")
                eb.request<String>("paxos.acceptor.$it.promise", round)
            }
            .let { Future.join(it) }
            .compose { promisesFuture ->
                if (promisesFuture.failed()) {
                    println("Failed...")
                    return@compose promisesFuture
                }

                val accepted = promisesFuture.list<Message<String>>()
                val value = accepted.map { Json.decodeFromString<DtoLastAccepted.Ok>(it.body()) }.filter { it.value != null }.maxByOrNull { it.round }?.value ?: value

                acceptors
                    .map {
                        println("Proposer $name proposed [ Acc: $it, N: $round, V: $value ]")
                        eb.request<String>("paxos.acceptor.$it.accept", Json.encodeToString(DtoAccept(round = round, value = value)))
                    }
                    .let {
                        proposesFuture -> Future.join(proposesFuture)
                    }
            }.onSuccess {
                println("Done!")
            }
    }

    private fun round(): Long { return 1L }

}
