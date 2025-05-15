package edu.myrza.paxos

import edu.myrza.paxos.dto.*
import io.vertx.core.AbstractVerticle
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class Acceptor(val name: String): AbstractVerticle() {

    private var promised: Long = 0L
    private var accepted: Long = 0L
    private var value: String? = null

    override fun start() {
        // promise
        vertx.eventBus().consumer<String>("paxos.acceptor.$name.promise") { message ->
            val request = Json.decodeFromString<DtoPromiseRequest>(message.body())
            val round = request.round

            if (promised < round) {
                println("Acceptor $name promised [Np: $promised, Na: $accepted, Va: $value]")
                promised = round
                message.reply(Json.encodeToString(DtoPromiseResponse(accepted = accepted, value = value)))
            } else {
                message.fail(
                    ErrorCodes.CUSTOM_ERROR,
                    Json.encodeToString(DtoFailResponse(DtoFailResponse.Phase.PROMISE, DtoFailResponse.Type.PROMISED_HIGHER))
                )
            }
        }

        // accept
        vertx.eventBus().consumer<String>("paxos.acceptor.$name.accept") { message ->
            val request = Json.decodeFromString<DtoAcceptRequest>(message.body())

            if (promised <= request.round) {
                println("Acceptor $name accepted [N: $promised, old : [Na: $accepted, Va: $value], new : [Na: ${request.round}, Va: ${request.value}]]")
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
