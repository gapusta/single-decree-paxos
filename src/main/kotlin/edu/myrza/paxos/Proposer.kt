package edu.myrza.paxos

import edu.myrza.paxos.dto.DtoAcceptRequest
import edu.myrza.paxos.dto.DtoFailResponse
import edu.myrza.paxos.dto.DtoPromiseRequest
import edu.myrza.paxos.dto.DtoPromiseResponse
import io.vertx.core.AbstractVerticle
import io.vertx.core.Future
import io.vertx.core.eventbus.Message
import io.vertx.core.eventbus.ReplyException
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
                value = promisesFuture.list<Message<String>>()
                    .map { Json.decodeFromString<DtoPromiseResponse>(it.body()) }
                    .filter { it.value != null }
                    .maxByOrNull { it.accepted }
                    ?.value ?: value

                acceptors
                    .map {
                        println("Proposer $name proposed [ Acc: $it, N: $round, V: $value ]")
                        eb.request<String>("paxos.acceptor.$it.accept", Json.encodeToString(DtoAcceptRequest(round = round, value = value)))
                    }
                    .let { proposeFuture -> Future.join(proposeFuture) }
            }
            .onFailure {
                // In Vert.x, a failed Future in a chain (via compose, andThen, or similar)
                // automatically stops the chain unless you handle the failure with recover or onFailure
                // and return a successful future.
                if (it !is ReplyException) {
                    return@onFailure
                }

                val fail = Json.decodeFromString<DtoFailResponse>(it.message!!)
                when (fail.type) {
                    DtoFailResponse.Type.PROMISED_HIGHER -> { } // try again
                }
            }
            .onSuccess {
                println("Proposer $name succeeded [ N: $round, V: $value ]")
                // TODO: cancel timer
            }
    }

    private fun round(): Long { return 1L }

}
