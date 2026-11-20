package dev.imhamid.gymyar.ui.coach

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.imhamid.gymyar.data.CoachHome
import dev.imhamid.gymyar.data.Repo
import dev.imhamid.gymyar.data.Res
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import java.text.Collator
import java.util.Locale

class CoachViewModel : ViewModel() {

    data class UiState(
        val loaded: Boolean = false,
        val data: CoachHome = CoachHome(),
        val stale: Boolean = false,
        val error: String? = null,
        val refreshing: Boolean = true,
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> get() = _state

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(refreshing = true) }
            when (val r = Repo.coachHome()) {
                is Res.Ok -> _state.value = UiState(loaded = true, data = r.value, stale = r.stale, error = null, refreshing = false)
                is Res.Fail -> _state.update { it.copy(refreshing = false, error = r.message) }
            }
        }
    }

    /** Runs a change on the server, reloads on success. Returns the error message, or null. */
    suspend fun act(fn: String, params: JsonObject = JsonObject(emptyMap())): String? =
        when (val r = Repo.act(fn, params)) {
            is Res.Ok -> { refresh(); null }
            is Res.Fail -> r.message
        }

    companion object {
        private val collator: Collator = Collator.getInstance(Locale("fa"))
        fun <T> sortByName(list: List<T>, name: (T) -> String): List<T> = list.sortedWith { a, b -> collator.compare(name(a), name(b)) }
    }
}
