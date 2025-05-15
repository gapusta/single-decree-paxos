package edu.myrza.paxos

import edu.myrza.paxos.dto.DtoAcceptRequest
import edu.myrza.paxos.dto.DtoAcceptResponse
import edu.myrza.paxos.dto.DtoPromiseRequest
import edu.myrza.paxos.dto.DtoPromiseResponse
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

            val result = if (promised < round) {
                println("Acceptor $name promised [Np: $promised, Na: $accepted, Va: $value]")
                promised = round
                DtoPromiseResponse.Success(round = accepted, value = value)
            } else {
                DtoPromiseResponse.Fail
            }

            message.reply(Json.encodeToString(result))
        }

        // accept
        vertx.eventBus().consumer<String>("paxos.acceptor.$name.accept") { message ->
            val request = Json.decodeFromString<DtoAcceptRequest>(message.body())

            val result = if (promised <= request.round) {
                println("Acceptor $name accepted [N: $promised, old : [Na: $accepted, Va: $value], new : [Na: ${request.round}, Va: ${request.value}]]")
                accepted = request.round
                value = request.value
                DtoAcceptResponse.success()
            } else {
                DtoAcceptResponse.fail()
            }

            message.reply(Json.encodeToString(result))
        }
    }

}
