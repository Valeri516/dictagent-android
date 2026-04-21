package com.valera.dictagent

import java.util.concurrent.atomic.AtomicReference

enum class State { IDLE, RECORDING, SENDING, ERROR }

object AppState {
    private val current = AtomicReference(State.IDLE)
    var lastFilePath: String? = null

    fun get() = current.get()
    fun set(s: State) = current.set(s)
    fun compareAndSet(expect: State, update: State) = current.compareAndSet(expect, update)
}
