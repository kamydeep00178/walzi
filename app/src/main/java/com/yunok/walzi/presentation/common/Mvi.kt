package com.yunok.walzi.presentation.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yunok.walzi.util.CrashReporter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Marker interfaces so every feature's contract reads the same way. */
interface MviIntent
interface MviState
interface MviEffect

/**
 * Like runCatching, but never swallows coroutine cancellation - catching it would keep a
 * cancelled coroutine running (and leave e.g. loading flags in the wrong state).
 */
suspend inline fun <T> runSuspendCatching(block: () -> T): Result<T> =
    try {
        Result.success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }

/**
 * Generic MVI base ViewModel: Intent in -> handleIntent mutates State via
 * setState{} -> Compose collects `state` -> one-shot events (toast, navigate)
 * go through `effect`.
 *
 * Any exception that escapes a [sendIntent] / [launchSafely] coroutine is reported to
 * Crashlytics as a non-fatal instead of killing the process. Code that has meaningful state to
 * restore on failure (loading flags, error banners) should still catch locally - this is only
 * the last line of defence.
 */
abstract class BaseViewModel<Intent : MviIntent, State : MviState, Effect : MviEffect>(
    initialState: State
) : ViewModel() {

    private val _state = MutableStateFlow(initialState)
    val state: StateFlow<State> = _state.asStateFlow()

    private val _effect = Channel<Effect>(Channel.BUFFERED)
    val effect: Flow<Effect> = _effect.receiveAsFlow()

    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        CrashReporter.record(throwable)
    }

    fun sendIntent(intent: Intent) {
        viewModelScope.launch(exceptionHandler) { handleIntent(intent) }
    }

    protected abstract suspend fun handleIntent(intent: Intent)

    /** viewModelScope.launch with the crash-safe handler attached. */
    protected fun launchSafely(block: suspend CoroutineScope.() -> Unit): Job =
        viewModelScope.launch(exceptionHandler, block = block)

    protected fun setState(reducer: State.() -> State) {
        _state.update { it.reducer() }
    }

    protected fun setEffect(effect: Effect) {
        viewModelScope.launch { _effect.send(effect) }
    }

    protected val currentState: State get() = _state.value
}
