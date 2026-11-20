package dev.imhamid.gymyar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.imhamid.gymyar.data.Api
import dev.imhamid.gymyar.data.Me
import dev.imhamid.gymyar.data.Repo
import dev.imhamid.gymyar.data.Res
import dev.imhamid.gymyar.data.Settings
import dev.imhamid.gymyar.ui.theme.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** Session + current user + theme. Everything else is loaded per screen. */
class AppViewModel : ViewModel() {

    sealed interface State {
        data object Loading : State
        data object LoggedOut : State
        data class Ready(val me: Me, val offline: Boolean) : State
        data class Failed(val message: String) : State
    }

    private val _state = MutableStateFlow<State>(if (Api.signedIn.value) State.Loading else State.LoggedOut)
    val state: StateFlow<State> get() = _state

    private val _theme = MutableStateFlow(Settings.themeMode)
    val themeMode: StateFlow<ThemeMode> get() = _theme

    init {
        viewModelScope.launch {
            Api.signedIn.collect { signedIn ->
                if (!signedIn) {
                    _state.value = State.LoggedOut
                } else if (_state.value !is State.Ready) {
                    load()
                }
            }
        }
    }

    fun load() {
        _state.value = State.Loading
        viewModelScope.launch {
            when (val r = Repo.me()) {
                is Res.Ok -> _state.value = State.Ready(r.value, r.stale)
                is Res.Fail -> if (Api.signedIn.value) _state.value = State.Failed(r.message)
            }
        }
    }

    /** After the user edited their name. */
    fun refreshMe() {
        viewModelScope.launch {
            val r = Repo.me()
            if (r is Res.Ok) _state.value = State.Ready(r.value, r.stale)
        }
    }

    fun setTheme(mode: ThemeMode) {
        Settings.themeMode = mode
        _theme.value = mode
    }

    fun logout() {
        viewModelScope.launch { Api.signOut() }
    }
}
