package edu.myrza.paxos

import edu.myrza.paxos.dto.DtoAccept
import edu.myrza.paxos.dto.DtoLastAccepted
import io.vertx.core.AbstractVerticle
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class Acceptor(val name: String): AbstractVerticle() {

    private var promised: Long = 0L
    private var accepted: Long = 0L
    private var value: String? = null

    override fun start() {
        // promise
        vertx.eventBus().consumer<Long>("paxos.acceptor.$name.promise") { message ->
            val round = message.body()
            if (promised < round) {
                promised = round
                println("Acceptor $name promised [Np: $promised, Na: $accepted, Va: $value]")
                message.reply(Json.encodeToString(DtoLastAccepted.Ok(round = accepted, value = value)))
            } else {
                message.reply(DtoLastAccepted.Fail)
            }
        }

        // accept
        vertx.eventBus().consumer<String>("paxos.acceptor.$name.accept") { message ->
            val msg = Json.decodeFromString<DtoAccept>(message.body())

            if (promised <= msg.round) {
                accepted = msg.round
                value = msg.value
                println("Acceptor $name accepted [N: $promised, new Na: $accepted, new Va: $value]")

                // TODO: What should acceptors return at the end of phase 2?
                message.reply("Accepted")
            } else {
                message.reply("Failed")
            }
        }
    }

}
