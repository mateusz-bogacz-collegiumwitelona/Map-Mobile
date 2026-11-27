package com.example.map.ui.viewmodels

data class HomeUiState(
    val firstName: String = "",
    val lastName: String = "",
    val isLoading: Boolean = true,
    val errorMessage: String? = null
) {
    val fullName: String
        get() = listOf(firstName.trim(), lastName.trim())
            .filter(String::isNotEmpty)
            .joinToString(" ")

    val initials: String
        get() = buildString {
            firstName.trim().firstOrNull()?.let { append(it.uppercaseChar()) }
            lastName.trim().firstOrNull()?.let { append(it.uppercaseChar()) }
        }
    }