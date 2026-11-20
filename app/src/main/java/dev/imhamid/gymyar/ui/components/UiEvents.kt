package dev.imhamid.gymyar.ui.components

import kotlinx.coroutines.flow.MutableSharedFlow

/** One-shot messages shown as a snackbar by the root screen. */
object UiEvents {
    val messages = MutableSharedFlow<String>(extraBufferCapacity = 8)
    fun toast(message: String) {
        messages.tryEmit(message)
    }
}
