package com.yunok.walzi.presentation.duo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yunok.walzi.data.repository.DuoRepository
import com.yunok.walzi.domain.model.Duo
import com.yunok.walzi.util.AnalyticsTracker
import com.yunok.walzi.util.WallpaperApplier
import com.yunok.walzi.util.WallpaperTarget
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DuoUiState(
    val isLoading: Boolean = true,
    val hasError: Boolean = false,
    val title: String = "DUO",
    val subtitle: String = "",
    val duos: List<Duo> = emptyList(),
    /** Duo id + target currently being applied (spinner on that button). */
    val applying: Pair<String, WallpaperTarget>? = null
)

sealed interface DuoEvent {
    data class Message(val text: String, val undoable: Boolean = false) : DuoEvent
    /** A Duo was applied - a natural, frequency-capped moment for an interstitial. */
    data object Applied : DuoEvent
}

@HiltViewModel
class DuoViewModel @Inject constructor(
    private val repository: DuoRepository,
    private val applier: WallpaperApplier,
    private val analytics: AnalyticsTracker
) : ViewModel() {

    private val _state = MutableStateFlow(DuoUiState())
    val state: StateFlow<DuoUiState> = _state.asStateFlow()

    private val _events = Channel<DuoEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private var lastViewedId: String? = null

    init {
        load()
    }

    fun retry() = load(forceRefresh = true)

    private fun load(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, hasError = false) }
            try {
                val config = repository.getConfig()
                val duos = repository.getDuos(forceRefresh)
                _state.update {
                    it.copy(
                        isLoading = false,
                        hasError = false,
                        duos = duos,
                        title = config?.title ?: "DUO",
                        subtitle = config?.subtitle.orEmpty()
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _state.update { it.copy(isLoading = false, hasError = it.duos.isEmpty()) }
            }
        }
    }

    /** Analytics: the pager settled on [duo] (deduplicated). */
    fun onDuoViewed(duo: Duo) {
        if (duo.id == lastViewedId) return
        lastViewedId = duo.id
        analytics.duoView(duo.id, duo.title)
    }

    fun apply(duo: Duo, target: WallpaperTarget) {
        if (_state.value.applying != null) return
        viewModelScope.launch {
            _state.update { it.copy(applying = duo.id to target) }
            val result = applier.applyDuo(duo, target)
            analytics.duoSet(duo.id, duo.title, target, result.isSuccess)
            _state.update { it.copy(applying = null) }
            if (result.isSuccess) {
                val label = when (target) {
                    WallpaperTarget.BOTH -> "Duo set on Lock & Home"
                    WallpaperTarget.HOME -> "Set on Home screen"
                    WallpaperTarget.LOCK -> "Set on Lock screen"
                }
                _events.send(DuoEvent.Message(label, undoable = applier.canUndo))
                _events.send(DuoEvent.Applied)
            } else {
                _events.send(DuoEvent.Message("Couldn't set it. Try again."))
            }
        }
    }

    fun undo() {
        viewModelScope.launch {
            val result = applier.undo()
            analytics.undo(result.isSuccess)
            _events.send(
                DuoEvent.Message(if (result.isSuccess) "Previous wallpaper restored" else "Couldn't restore the previous wallpaper.")
            )
        }
    }
}
