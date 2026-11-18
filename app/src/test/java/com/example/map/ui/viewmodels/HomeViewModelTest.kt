package com.example.map.ui.viewmodels

import com.example.map.domain.model.UserProfile
import com.example.map.domain.usecase.AccountSession
import com.example.map.testing.FakeAccountRepository
import com.example.map.testing.FakeProfileStorage
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun loadProfileDisplaysNameAndInitials() = runTest(dispatcher) {
        val repository = FakeAccountRepository().apply {
            currentUserProfile = UserProfile(
                firstName = "Tomasz",
                lastName = "Kot"
            )
        }
        val accountSession = AccountSession(
            repository,
            FakeProfileStorage()
        )
        val viewModel = HomeViewModel(accountSession)

        advanceUntilIdle()

        val state = viewModel.uiState.value

        assertEquals("Tomasz Kot", state.fullName)
        assertEquals("TK", state.initials)
        assertFalse(state.isLoading)
        assertEquals(null, state.errorMessage)
    }

    @Test
    fun loadProfileShowsErrorWhenRepositoryFails() = runTest(dispatcher) {
        val repository = FakeAccountRepository().apply {
            currentUserProfileError = IOException("Offline")
        }
        val accountSession = AccountSession(
            repository,
            FakeProfileStorage()
        )
        val viewModel = HomeViewModel(accountSession)

        advanceUntilIdle()

        val state = viewModel.uiState.value

        assertFalse(state.isLoading)
        assertEquals(
            "Nie udało się pobrać danych profilu.",
            state.errorMessage
        )
    }
}