package tech.dotlab.dot.feature.key.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import tech.dotlab.dot.core.model.Gesture
import tech.dotlab.dot.core.model.UnlockMethod
import tech.dotlab.dot.feature.key.data.KeyConfig
import tech.dotlab.dot.feature.key.data.KeyLevel
import tech.dotlab.dot.feature.key.data.KeyPrefs
import tech.dotlab.dot.feature.key.data.UnlockState
import tech.dotlab.dot.feature.key.service.KeyEventBus

data class KeyUiState(
    val config: KeyConfig = KeyConfig(keyCode = null, bindings = emptyMap(), unlockMethod = UnlockMethod.None),
    val serviceConnected: Boolean = false,
    val level: KeyLevel = KeyLevel.OFF,
) {
    /** Lite is "on" only when the accessibility service is actually connected. */
    val active: Boolean get() = serviceConnected
    val freed: Boolean get() = level == KeyLevel.FULL
}

class KeyViewModel(
    private val prefs: KeyPrefs,
    private val appContext: Context,
) : ViewModel() {

    // Bumped after Shizuku/ADB changes so the derived level re-reads package state.
    private val refreshTick = MutableStateFlow(0)

    val state: StateFlow<KeyUiState> =
        combine(prefs.config, KeyEventBus.serviceConnected, refreshTick) { config, connected, _ ->
            KeyUiState(
                config = config,
                serviceConnected = connected,
                level = UnlockState.level(appContext, connected),
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), KeyUiState())

    fun refresh() {
        refreshTick.value += 1
    }

    fun bind(gesture: Gesture, actionId: String) = viewModelScope.launch {
        prefs.bind(gesture, actionId)
    }

    fun clearBinding(gesture: Gesture) = viewModelScope.launch {
        prefs.clearBinding(gesture)
    }

    fun setKeyCode(keyCode: Int) = viewModelScope.launch {
        prefs.setKeyCode(keyCode)
    }

    companion object {
        fun factory(context: Context): ViewModelProvider.Factory {
            val appContext = context.applicationContext
            return viewModelFactory {
                initializer { KeyViewModel(KeyPrefs(appContext), appContext) }
            }
        }
    }
}
