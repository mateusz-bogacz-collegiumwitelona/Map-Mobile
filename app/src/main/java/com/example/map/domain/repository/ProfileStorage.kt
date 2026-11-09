package com.example.map.domain.repository

interface ProfileStorage {
    fun getEmail(): String?
    fun saveEmail(email: String)
    fun clear()
}
