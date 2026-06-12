package tech.dotlab.dot.core.model

/**
 * Essential Key gestures the remapper can bind actions to.
 *
 * [SINGLE] is captured by the system (Essential Space) unless an [UnlockMethod] other than
 * [UnlockMethod.None] is active, so the UI gates it accordingly.
 */
enum class Gesture {
    SINGLE,
    DOUBLE,
    TRIPLE,
    LONG_PRESS,
}
