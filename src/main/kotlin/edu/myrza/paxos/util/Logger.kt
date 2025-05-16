package edu.myrza.paxos.util

object Logger {
    fun log(msg: String) {
        println("${Thread.currentThread().name}: $msg")
    }
}
