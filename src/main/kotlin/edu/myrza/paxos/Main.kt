package edu.myrza.paxos

import edu.myrza.paxos.util.ShutdownHandler
import io.vertx.core.Future
import io.vertx.core.Vertx
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.CompletableFuture
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

fun main() {
    startCoroutinesExample()
//    startSimulation()
}

fun startSimulation() {
    val vertx = Vertx.vertx()

    val acceptors = listOf(
        VerticleAcceptor(id = 1),
        VerticleAcceptor(id = 2),
        VerticleAcceptor(id = 3)
    )

    val acceptorNames = acceptors.map { it.name() }.toSet()
    val proposers = listOf(
        VerticleProposer(id = 1, value = "P1-value", acceptors = acceptorNames),
        VerticleProposer(id = 2, value = "P2-value", acceptors = acceptorNames),
        VerticleProposer(id = 3, value = "P3-value", acceptors = acceptorNames)
    )

    Future.all<Future<String>>(
        acceptors.map { vertx.deployVerticle(it) }
    ).onSuccess {
        proposers.forEach { vertx.deployVerticle(it) }
    }

    // shutdown handler
    Runtime.getRuntime().addShutdownHook(ShutdownHandler(vertx))
}

fun startCoroutinesExample() = runBlocking {
    launch {
        // suspendCancellableCoroutine does 2 things:
        // 1. calls the 'block' lambda. The 'block' makes the async operation callback to continue the coroutine when the operation is done
        // (the callback calls continuation.resumeWith(Result.success(true)))
        // 2. returns COROUTINE_SUSPENDED
        CompletableFuture.supplyAsync {
            Thread.sleep(2000)
            true
        }.apply {
            suspendCancellableCoroutine<Boolean> { continuation ->
                thenAccept {
                    continuation.resume(it)
                }
                exceptionally {
                    continuation.resumeWithException(it)
                    null
                }
            }
        }
        println("hey #2")
    }
    launch {
        delay(1000)
        println("hey #3")
    }
    println("hey #1")
}
