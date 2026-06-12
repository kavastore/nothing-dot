package tech.dotlab.dot.feature.key.service

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * In-process channel between the accessibility service and the "record trigger" screen.
 *
 * While [recording] is true the service publishes any caught keyCode here instead of acting on it,
 * so the UI can learn which key is the Essential Key.
 */
object KeyEventBus {

    private val _recording = MutableStateFlow(false)
    val recording: StateFlow<Boolean> = _recording.asStateFlow()

    private val _captured = MutableStateFlow<Int?>(null)
    val captured: StateFlow<Int?> = _captured.asStateFlow()

    /** Reports whether the accessibility service is currently connected. */
    val serviceConnected: MutableStateFlow<Boolean> = MutableStateFlow(false)

    fun startRecording() {
        _captured.value = null
        _recording.value = true
    }

    fun stopRecording() {
        _recording.value = false
    }

    fun publish(keyCode: Int) {
        _captured.value = keyCode
    }

    fun consumeCaptured() {
        _captured.value = null
    }
}
