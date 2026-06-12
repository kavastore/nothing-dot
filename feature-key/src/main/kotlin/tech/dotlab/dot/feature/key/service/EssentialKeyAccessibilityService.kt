package tech.dotlab.dot.feature.key.service

import android.accessibilityservice.AccessibilityService
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import tech.dotlab.dot.core.model.Gesture
import tech.dotlab.dot.feature.key.action.ActionRegistry
import tech.dotlab.dot.feature.key.data.KeyConfig
import tech.dotlab.dot.feature.key.data.KeyPrefs
import tech.dotlab.dot.feature.key.data.UnlockState

/**
 * Intercepts Essential Key presses and maps multi-tap / long-press gestures to actions.
 *
 * Lite contract: we never consume the event (`onKeyEvent` always returns `false`), so the system's
 * own single-press handling keeps working. We only recognise DOUBLE / TRIPLE / LONG_PRESS within a
 * short window; SINGLE is left to the system until a deeper unlock method is available.
 */
class EssentialKeyAccessibilityService : AccessibilityService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val handler = Handler(Looper.getMainLooper())

    @Volatile
    private var config: KeyConfig? = null

    private var tapCount = 0
    private var longPressFired = false
    private val resolveTaps = Runnable {
        when (tapCount) {
            // SINGLE is only ours once the key is freed (Essential Space packages disabled);
            // otherwise the system owns single press and we must not fight it.
            1 -> if (UnlockState.isFreed(applicationContext)) trigger(Gesture.SINGLE)
            2 -> trigger(Gesture.DOUBLE)
            3 -> trigger(Gesture.TRIPLE)
        }
        tapCount = 0
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        KeyEventBus.serviceConnected.value = true
        val prefs = KeyPrefs(applicationContext)
        prefs.config
            .onEach { config = it }
            .launchIn(scope)
    }

    override fun onKeyEvent(event: KeyEvent): Boolean {
        val recording = KeyEventBus.recording.value
        if (recording) {
            if (event.action == KeyEvent.ACTION_DOWN) KeyEventBus.publish(event.keyCode)
            return false
        }

        val target = config?.keyCode ?: return false
        if (event.keyCode != target) return false

        when (event.action) {
            KeyEvent.ACTION_DOWN -> {
                if (event.isLongPress) {
                    longPressFired = true
                    handler.removeCallbacks(resolveTaps)
                    tapCount = 0
                    trigger(Gesture.LONG_PRESS)
                }
            }

            KeyEvent.ACTION_UP -> {
                if (longPressFired) {
                    longPressFired = false
                } else {
                    tapCount++
                    handler.removeCallbacks(resolveTaps)
                    handler.postDelayed(resolveTaps, MULTI_TAP_WINDOW_MS)
                }
            }
        }
        return false
    }

    private fun trigger(gesture: Gesture) {
        val actionId = config?.bindings?.get(gesture) ?: return
        ActionRegistry.byId(actionId)?.run(this)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit

    override fun onInterrupt() = Unit

    override fun onUnbind(intent: android.content.Intent?): Boolean {
        KeyEventBus.serviceConnected.value = false
        handler.removeCallbacks(resolveTaps)
        scope.cancel()
        return super.onUnbind(intent)
    }

    private companion object {
        const val MULTI_TAP_WINDOW_MS = 300L
    }
}
