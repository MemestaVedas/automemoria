package com.automemoria.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.automemoria.BuildConfig
import com.automemoria.sync.SyncPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class SettingsUiState(
    val supabaseConfigured: Boolean = false,
    val supabaseUrlLabel: String = "Not configured",
    val userEmail: String = "",
    val lastSyncedLabel: String = "Never",
    val versionLabel: String = "",
    val isSigningOut: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val syncPreferences: SyncPreferences,
    private val supabase: SupabaseClient
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        SettingsUiState(
            versionLabel = "v${BuildConfig.VERSION_NAME}"
        )
    )
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val url = BuildConfig.SUPABASE_URL
            val configured = url.isNotBlank() && BuildConfig.SUPABASE_ANON_KEY.isNotBlank()
            val lastSyncRaw = runCatching { syncPreferences.getLastSyncTimestamp() }.getOrDefault("1970-01-01T00:00:00")
            val storedEmail = runCatching { syncPreferences.getUserEmail() }.getOrDefault("")
            val authEmail = supabase.auth.currentUserOrNull()?.email.orEmpty()
            val email = authEmail.ifBlank { storedEmail }

            _uiState.update {
                it.copy(
                    supabaseConfigured = configured,
                    supabaseUrlLabel = if (configured) maskUrl(url) else "Not configured",
                    userEmail = email,
                    lastSyncedLabel = formatLastSync(lastSyncRaw)
                )
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSigningOut = true) }
            runCatching {
                supabase.auth.signOut()
                syncPreferences.setUserEmail("")
            }
            _uiState.update { it.copy(isSigningOut = false) }
            refresh()
        }
    }

    private fun maskUrl(url: String): String {
        return try {
            val host = java.net.URI(url).host ?: return "Configured"
            val parts = host.split(".")
            if (parts.isEmpty()) return "Configured"
            val project = parts.first()
            val suffix = parts.drop(1).joinToString(".")
            "${project.take(6)}...$suffix"
        } catch (_: Exception) {
            "Configured"
        }
    }

    private fun formatLastSync(raw: String): String {
        if (raw == "1970-01-01T00:00:00") return "Never"
        return runCatching {
            val parsed = LocalDateTime.parse(raw)
            parsed.format(DateTimeFormatter.ofPattern("MMM d, yyyy HH:mm"))
        }.getOrElse { raw }
    }
}
