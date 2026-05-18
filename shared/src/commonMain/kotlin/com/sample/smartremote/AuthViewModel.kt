package com.sample.smartremote

import com.sample.smartremote.data.repository.AuthRepository
import dev.icerock.moko.mvvm.viewmodel.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _isAuthorized = MutableStateFlow(authRepository.isAuthorized())
    val isAuthorized = _isAuthorized.asStateFlow()

    private val _loginError = MutableStateFlow<String?>(null)
    val loginError = _loginError.asStateFlow()

    private val _isLoggingIn = MutableStateFlow(value = false)
    val isLoggingIn = _isLoggingIn.asStateFlow()

    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            _isLoggingIn.value = true
            _loginError.value = null

            val result = authRepository.signIn(email, password)
            result.onSuccess {
                _isAuthorized.value = true
            }
            result.onFailure { error ->
                _loginError.value = error.message ?: "Unknown error"
            }
            _isLoggingIn.value = false
        }
    }

    fun logout() {
        authRepository.logout()
        _isAuthorized.value = false
    }
}
