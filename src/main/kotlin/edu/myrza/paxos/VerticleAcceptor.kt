package edu.myrza.paxos

import edu.myrza.paxos.dto.*
import edu.myrza.paxos.exception.ErrorCodes
import edu.myrza.paxos.util.Logger
import io.vertx.core.AbstractVerticle
import kotlinx.serialization.json.Json

class VerticleAcceptor(val name: String): AbstractVerticle() {

    private var promised: Long = 0L
    private var accepted: Long = 0L
    private var value: String? = null

    override fun start() {
        Logger.log("Init acceptor $name")

        vertx.eventBus().consumer<String>("paxos.acceptor.$name.prepare") { message ->
            val request = Json.decodeFromString<DtoPromiseRequest>(message.body())
            val round = request.round

            if (promised < round) {
                promised = round
                Logger.log("Acceptor $name promised to ${request.propose} [Np: $promised, Na: $accepted, Va: $value]")
                message.reply(Json.encodeToString(DtoPromiseResponse(accepted = accepted, value = value)))
            } else {
                message.fail(
                    ErrorCodes.CUSTOM_ERROR,
                    Json.encodeToString(DtoFailResponse(DtoFailResponse.Phase.PREPARE, DtoFailResponse.Type.PROMISED_HIGHER))
                )
            }
        }

        vertx.eventBus().consumer<String>("paxos.acceptor.$name.accept") { message ->
            val request = Json.decodeFromString<DtoAcceptRequest>(message.body())

            if (promised <= request.round) {
                Logger.log("Acceptor $name accepted ${request.proposer} [N: $promised, old : [Na: $accepted, Va: $value], new : [Na: ${request.round}, Va: ${request.value}]]")
                accepted = request.round
                value = request.value
                message.reply(Json.encodeToString(DtoAcceptResponse()))
            } else {
                message.fail(
                    ErrorCodes.CUSTOM_ERROR,
                    Json.encodeToString(DtoFailResponse(DtoFailResponse.Phase.ACCEPT, DtoFailResponse.Type.PROMISED_HIGHER))
                )
            }
        }
    }

}
