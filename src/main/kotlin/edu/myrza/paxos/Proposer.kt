package edu.myrza.paxos

import edu.myrza.paxos.dto.DtoAcceptRequest
import edu.myrza.paxos.dto.DtoAcceptResponse
import edu.myrza.paxos.dto.DtoPromiseRequest
import edu.myrza.paxos.dto.DtoPromiseResponse
import io.vertx.core.AbstractVerticle
import io.vertx.core.Future
import io.vertx.core.eventbus.Message
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class Proposer(
    private val name: String,
    private var value: String,
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
                val request = Json.encodeToString(DtoPromiseRequest(round = round))
                eb.request<String>("paxos.acceptor.$it.promise", request)
            }
            .let { Future.join(it) }
            .compose { promisesFuture ->
                if (promisesFuture.failed()) {
                    println("Proposer $name failed (system error) at promises step")
                    return@compose promisesFuture
                }

                val promised = promisesFuture.list<Message<String>>()
                value = promised
                    .map { Json.decodeFromString<DtoPromiseResponse>(it.body()) }
                    .map { it as DtoPromiseResponse.Success }
                    .filter { it.value != null }
                    .maxByOrNull { it.round }
                    ?.value ?: value

                acceptors
                    .map {
                        println("Proposer $name proposed [ Acc: $it, N: $round, V: $value ]")
                        eb.request<String>("paxos.acceptor.$it.accept", Json.encodeToString(DtoAcceptRequest(round = round, value = value)))
                    }
                    .let {
                        proposeFuture -> Future.join(proposeFuture)
                    }
            }.onSuccess { proposesFuture ->
                if (proposesFuture.failed()) {
                    println("Proposer $name failed (system error) at proposes step")
                    return@onSuccess
                }

                val accepted = proposesFuture.list<Message<String>>()
                val allAccepted = accepted
                    .map { Json.decodeFromString<DtoAcceptResponse>(it.body()) }
                    .all { it.status == DtoAcceptResponse.Status.SUCCESS }

                if (!allAccepted) {
                    println("Proposer $name failed")
                    return@onSuccess
                }

                println("Proposer $name succeeded [ N: $round, V: $value ]")
            }
    }

    private fun round(): Long { return 1L }

}
