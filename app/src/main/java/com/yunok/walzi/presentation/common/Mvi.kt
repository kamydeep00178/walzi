package com.yunok.walzi.presentation.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/** Marker interfaces so every feature's contract reads the same way. */
interface MviIntent
interface MviState
interface MviEffect

/**
 * Generic MVI base ViewModel: Intent in -> handleIntent mutates State via
 * setState{} -> Compose collects `state` -> one-shot events (toast, navigate)
 * go through `effect`.
 */
abstract class BaseViewModel<Intent : MviIntent, State : MviState, Effect : MviEffect>(
    initialState: State
) : ViewModel() {

    private val _state = MutableStateFlow(initialState)
    val state: StateFlow<State> = _state.asStateFlow()

    private val _effect = Channel<Effect>(Channel.BUFFERED)
    val effect: Flow<Effect> = _effect.receiveAsFlow()

    fun sendIntent(intent: Intent) {
        viewModelScope.launch { handleIntent(intent) }
    }

    protected abstract suspend fun handleIntent(intent: Intent)

    protected fun setState(reducer: State.() -> State) {
        _state.value = _state.value.reducer()
    }

    protected fun setEffect(effect: Effect) {
        viewModelScope.launch { _effect.send(effect) }
    }

    protected val currentState: State get() = _state.value
}
