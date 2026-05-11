package com.restaurant.management.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.restaurant.management.RestaurantApplication
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private val app = application as RestaurantApplication

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun clearMessage() {
        _message.value = null
    }

    fun login(
        loginRaw: String,
        password: String,
    ) = viewModelScope.launch {
        _message.value = null
        if (loginRaw.isBlank() || password.isBlank()) {
            _message.value = "Enter phone or username and password."
            return@launch
        }
        val id = app.accountsRepo.verifyLoginWithBackendThenLocal(loginRaw, password)
        if (id == null) {
            _message.value = "Wrong phone/username or password (or cannot reach server)."
            return@launch
        }
        app.openRestaurantForUser(id)
    }

    fun register(
        loginRaw: String,
        password: String,
        confirmPassword: String,
    ) = viewModelScope.launch {
        _message.value = null
        if (password != confirmPassword) {
            _message.value = "Passwords do not match."
            return@launch
        }
        app.accountsRepo
            .register(loginRaw, password)
            .onSuccess { userId -> app.openRestaurantForUser(userId) }
            .onFailure { e ->
                _message.value = e.message ?: "Could not create account."
            }
    }
}

class AuthViewModelFactory(
    private val application: Application,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
            return AuthViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
