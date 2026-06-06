package edu.myrza.paxos

import edu.myrza.paxos.dto.DtoAcceptRequest
import edu.myrza.paxos.dto.DtoFailResponse
import edu.myrza.paxos.dto.DtoPromiseRequest
import edu.myrza.paxos.dto.DtoPromiseResponse
import edu.myrza.paxos.exception.ErrorCodes
import edu.myrza.paxos.model.Round
import edu.myrza.paxos.util.Logger
import io.vertx.core.eventbus.Message
import io.vertx.core.eventbus.ReplyException
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.awaitResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.util.concurrent.atomic.AtomicLong

class VerticleProposer(
    private val id: Long,
    private var value: String,
    private val acceptors: Set<String>
): CoroutineVerticle() {

    private lateinit var job: Job
    private val roundGenerator = AtomicLong(0L)

    override suspend fun start() {
        job = launch { run() }
        Logger.log("Init proposer ${name()}")
    }

    override suspend fun stop() {
        job.cancel()
    }

    private fun name() = "P$id"

    private suspend fun run() {
        delay(1000)

        val name = name()

        while (true) {
            val eb = vertx.eventBus()
            val majority = acceptors.shuffled().take(acceptors.size / 2 + 1)
            val round = Round(
                proposerId = id,
                round = roundGenerator.incrementAndGet()
            )

            Logger.log("$name round started [ N : $round ]")

            val promises = mutableListOf<Message<String>>()

            try {
                // 1. prepare
                majority.forEach { acceptor ->
                    Logger.log("$name is sending prepare($round) to $acceptor")

                    val promise = awaitResult<Message<String>> {
                        eb.request<String>(
                            "paxos.acceptor.$acceptor.prepare",
                            Json.encodeToString(
                                DtoPromiseRequest(
                                    propose = name,
                                    round = round
                                )
                            )
                        ).onComplete(it)
                    }

                    promises.add(promise)
                }

                // 2. find already accepted propose with the highest N and propose its value or propose our value
                val value = promises.map { Json.decodeFromString<DtoPromiseResponse>(it.body()) }
                    .filter { it.value != null }
                    .maxByOrNull { it.accepted }
                    ?.value ?: value

                // 3. propose
                majority.forEach { acceptor ->
                    Logger.log("$name is sending accept($round, $value) to $acceptor")

                    awaitResult<Message<String>> {
                        eb.request<String>(
                            "paxos.acceptor.$acceptor.accept",
                            Json.encodeToString(
                                DtoAcceptRequest(
                                    proposer = name,
                                    round = round,
                                    value = value
                                )
                            )
                        ).onComplete(it)
                    }
                }

                Logger.log("Proposer $name succeeded [ N: $round, V: $value ]")

                break
            } catch (ex: Throwable) {
                if (ex !is ReplyException || ex.failureCode() != ErrorCodes.CUSTOM_ERROR) {
                    Logger.log("Unexpected exception: ${ex.message}")
                    break
                }

                val fail = Json.decodeFromString<DtoFailResponse>(ex.message!!)
                when (fail.type) {
                    DtoFailResponse.Type.PROMISED_HIGHER -> {
                        Logger.log("$name got disrupted [ N: $round ]")
                        // try again
                        delay(5000)
                    }
                }
            }
        }
    }
}
