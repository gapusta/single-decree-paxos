package edu.myrza.paxos.util

import io.vertx.core.Vertx
import java.util.concurrent.TimeUnit

class ShutdownHandler(
    private val vertx: Vertx
): Thread() {

    override fun run() {
        try {
            vertx.close()
                .toCompletionStage()
                .toCompletableFuture()
                .get(10, TimeUnit.SECONDS)

            Logger.log("Graceful shutdown success")
        } catch (ex: Exception) {
            Logger.log("Graceful shutdown error: ${ex.message ?: ""}. ${ex.cause?.message ?: ""}")
        }
    }

}
