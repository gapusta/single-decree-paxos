package edu.myrza.paxos

import edu.myrza.paxos.dto.DtoAcceptRequest
import edu.myrza.paxos.dto.DtoFailResponse
import edu.myrza.paxos.dto.DtoPromiseRequest
import edu.myrza.paxos.dto.DtoPromiseResponse
import edu.myrza.paxos.exception.ErrorCodes
import edu.myrza.paxos.model.Round
import edu.myrza.paxos.util.Logger
import io.vertx.core.AbstractVerticle
import io.vertx.core.Future
import io.vertx.core.eventbus.Message
import io.vertx.core.eventbus.ReplyException
import kotlinx.serialization.json.Json
import java.util.concurrent.atomic.AtomicLong

class VerticleProposer(
    private val id: Long,
    private var value: String,
    private val acceptors: Set<String>
): AbstractVerticle() {

    private val roundGenerator = AtomicLong(0L)

    override fun start() {
        val name = "P$id"

        Logger.log("Init proposer $name")

        vertx.setPeriodic(1000) { timerId ->
            val eb = vertx.eventBus()
            val majority = acceptors.shuffled().take(acceptors.size / 2 + 1)
            val round = Round(
                proposerId = id,
                round = roundGenerator.incrementAndGet()
            )

            Logger.log("$name round started [ N : $round ]")
            majority
                .map { acceptor ->
                    Logger.log("$name is sending prepare($round) to $acceptor ")

                    val request = Json.encodeToString(
                        DtoPromiseRequest(
                            propose = name,
                            round = round
                        )
                    )

                    eb.request<String>("paxos.acceptor.$acceptor.prepare", request)
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
                            Logger.log("$name is sending accept($round, $value) to $acceptor")

                            val request = Json.encodeToString(
                                DtoAcceptRequest(
                                    proposer = name,
                                    round = round,
                                    value = value
                                )
                            )

                            eb.request<String>("paxos.acceptor.$acceptor.accept", request)
                        }
                        .let { proposeFuture -> Future.join(proposeFuture) }
                }
                .onFailure {
                    // In Vert.x, a failed Future in a chain (via compose, andThen, or similar)
                    // automatically stops the chain unless you handle the failure with recover or onFailure
                    // and return a successful future.
                    if (it !is ReplyException || it.failureCode() != ErrorCodes.CUSTOM_ERROR) {
                        Logger.log("Unexpected exception: ${it.message}")
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
