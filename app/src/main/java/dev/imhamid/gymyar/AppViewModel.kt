package dev.imhamid.gymyar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.imhamid.gymyar.data.Api
import dev.imhamid.gymyar.data.Me
import dev.imhamid.gymyar.data.Repo
import dev.imhamid.gymyar.data.Res
import dev.imhamid.gymyar.data.Settings
import dev.imhamid.gymyar.data.Updater
import dev.imhamid.gymyar.ui.components.UiEvents
import dev.imhamid.gymyar.ui.theme.ThemeMode
import android.os.SystemClock
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File

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

    // ---- in-app update
    sealed interface UpdatePhase {
        data object Available : UpdatePhase
        data class Downloading(val progress: Float?) : UpdatePhase
        data class Ready(val file: File) : UpdatePhase
        data class Failed(val message: String) : UpdatePhase
    }

    data class UpdateState(val release: Updater.Release, val phase: UpdatePhase = UpdatePhase.Available, val dialog: Boolean = true)

    private val _update = MutableStateFlow<UpdateState?>(null)
    val update: StateFlow<UpdateState?> get() = _update

    private var lastCheck = 0L
    private var downloadJob: Job? = null

    /** Looks for a newer release; called whenever the app is opened (throttled so a quick app switch does not refetch). */
    fun checkForUpdate() {
        val now = SystemClock.elapsedRealtime()
        if (lastCheck != 0L && now - lastCheck < 10 * 60_000L) return
        lastCheck = now
        viewModelScope.launch {
            val rel = Updater.fetchLatest()
            if (rel == null) { lastCheck = 0L; return@launch }
            if (!Updater.isNewer(rel.version, Updater.currentVersion)) return@launch
            if (_update.value?.release?.version == rel.version) return@launch
            _update.value = UpdateState(rel)
        }
    }

    fun openUpdateDialog() { _update.value = _update.value?.copy(dialog = true) }
    fun closeUpdateDialog() { _update.value = _update.value?.copy(dialog = false) }

    fun startDownload() {
        val cur = _update.value ?: return
        if (downloadJob?.isActive == true) return
        _update.value = cur.copy(phase = UpdatePhase.Downloading(null))
        downloadJob = viewModelScope.launch {
            try {
                val f = Updater.download(GymyarApp.instance, cur.release) { p ->
                    _update.value = _update.value?.copy(phase = UpdatePhase.Downloading(p))
                }
                _update.value = _update.value?.copy(phase = UpdatePhase.Ready(f))
            } catch (e: CancellationException) {
                _update.value = _update.value?.copy(phase = UpdatePhase.Available)
                throw e
            } catch (e: Exception) {
                _update.value = _update.value?.copy(phase = UpdatePhase.Failed("دانلود انجام نشد. اتصال اینترنت را بررسی و دوباره تلاش کنید."))
            }
        }
    }

    fun cancelDownload() { downloadJob?.cancel() }

    fun installUpdate(context: android.content.Context) {
        val ready = (_update.value?.phase as? UpdatePhase.Ready) ?: return
        if (!Updater.canInstall(context)) {
            UiEvents.toast("اجازه‌ی نصب برنامه را برای باشگاه‌یار فعال کنید، سپس برگردید و دوباره «نصب» را بزنید.")
            Updater.openInstallPermissionSettings(context)
            return
        }
        runCatching { Updater.install(context, ready.file) }
            .onFailure { UiEvents.toast("نصب شروع نشد. دوباره تلاش کنید.") }
    }

    init {
        checkForUpdate()
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
