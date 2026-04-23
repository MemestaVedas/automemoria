package com.automemoria.ui.setup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.automemoria.sync.SyncPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SetupUiState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = true,
    val isSubmitting: Boolean = false,
    val isAuthenticated: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class SetupViewModel @Inject constructor(
    private val supabase: SupabaseClient,
    private val syncPreferences: SyncPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(SetupUiState())
    val uiState: StateFlow<SetupUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val savedEmail = runCatching { syncPreferences.getUserEmail() }.getOrDefault("")
            if (savedEmail.isNotBlank()) {
                _uiState.update { it.copy(email = savedEmail) }
            }

            supabase.auth.awaitInitialization()
            supabase.auth.sessionStatus.collect { status ->
                when (status) {
                    SessionStatus.Initializing -> {
                        _uiState.update { it.copy(isLoading = true) }
                    }

                    is SessionStatus.Authenticated -> {
                        val currentEmail = status.session.user?.email.orEmpty()
                        if (currentEmail.isNotBlank()) {
                            runCatching { syncPreferences.setUserEmail(currentEmail) }
                        }
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                isSubmitting = false,
                                isAuthenticated = true,
                                error = null,
                                email = if (currentEmail.isNotBlank()) currentEmail else it.email,
                                password = ""
                            )
                        }
                    }

                    is SessionStatus.NotAuthenticated -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                isSubmitting = false,
                                isAuthenticated = false
                            )
                        }
                    }

                    is SessionStatus.RefreshFailure -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                isSubmitting = false,
                                isAuthenticated = false,
                                error = "Session refresh failed. Please sign in again."
                            )
                        }
                    }
                }
            }
        }
    }

    fun onEmailChange(value: String) {
        _uiState.update { it.copy(email = value, error = null) }
    }

    fun onPasswordChange(value: String) {
        _uiState.update { it.copy(password = value, error = null) }
    }

    fun signIn() {
        val email = _uiState.value.email.trim()
        val password = _uiState.value.password
        if (email.isBlank() || password.isBlank()) {
            _uiState.update { it.copy(error = "Email and password are required.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, error = null) }
            try {
                supabase.auth.signInWith(Email) {
                    this.email = email
                    this.password = password
                }
                runCatching { syncPreferences.setUserEmail(email) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSubmitting = false,
                        error = e.message ?: "Sign-in failed."
                    )
                }
            }
        }
    }
}
