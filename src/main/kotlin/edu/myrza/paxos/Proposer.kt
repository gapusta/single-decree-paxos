package edu.myrza.paxos

import edu.myrza.paxos.dto.DtoAcceptRequest
import edu.myrza.paxos.dto.DtoFailResponse
import edu.myrza.paxos.dto.DtoPromiseRequest
import edu.myrza.paxos.dto.DtoPromiseResponse
import edu.myrza.paxos.exception.ErrorCodes
import edu.myrza.paxos.util.GlobalRoundGenerator
import edu.myrza.paxos.util.Logger
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
        Logger.log("Init $name")

        vertx.setPeriodic(1000) { timerId ->
            val eb = vertx.eventBus()
            val round = GlobalRoundGenerator.round()
            val majority = acceptors.shuffled().take(acceptors.size / 2 + 1)

            Logger.log("Proposer $name round started [ N : $round ]")
            majority
                .map { acceptor ->
                    Logger.log("$name->$acceptor promise($round)")

                    val request = Json.encodeToString(DtoPromiseRequest(round = round))
                    eb.request<String>("paxos.acceptor.$acceptor.promise", request)
                }
                .let { Future.join(it) }
                .compose { promisesFuture ->
                    value = promisesFuture.list<Message<String>>()
                        .map { Json.decodeFromString<DtoPromiseResponse>(it.body()) }
                        .filter { it.value != null }
                        .maxByOrNull { it.accepted }
                        ?.value ?: value

                    majority
                        .map { acceptor ->
                            Logger.log("$name->$acceptor propose($round, $value)")

                            eb.request<String>("paxos.acceptor.$acceptor.accept", Json.encodeToString(DtoAcceptRequest(round = round, value = value)))
                        }
                        .let { proposeFuture -> Future.join(proposeFuture) }
                }
                .onFailure {
                    // In Vert.x, a failed Future in a chain (via compose, andThen, or similar)
                    // automatically stops the chain unless you handle the failure with recover or onFailure
                    // and return a successful future.
                    if (it !is ReplyException || it.failureCode() != ErrorCodes.CUSTOM_ERROR) {
                        Logger.log("Unhandled exception: ${it.message}")
                        return@onFailure
                    }

                    val fail = Json.decodeFromString<DtoFailResponse>(it.message!!)
                    when (fail.type) {
                        DtoFailResponse.Type.PROMISED_HIGHER -> {
                            Logger.log("$name got disrupted [ N: $round ]")
                            // try again
                        }
                    }
                }
                .onSuccess {
                    Logger.log("Proposer $name succeeded [ N: $round, V: $value ]")

                    vertx.cancelTimer(timerId)
                }
        }
    }

}
