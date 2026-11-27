package com.example.map.domain.session

import com.example.map.domain.model.auth.RegistrationError
import com.example.map.domain.model.auth.RegistrationResult
import com.example.map.testing.FakeAuthRepository
import com.example.map.testing.FakeTokenStorage
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class AuthSessionRegisterTest {

    private lateinit var repository: FakeAuthRepository
    private lateinit var session: AuthSession

    @Before
    fun setUp() {
        repository = FakeAuthRepository()
        session = AuthSession(
            tokenStorage = FakeTokenStorage(),
            authRepository = repository
        )
    }

    @Test
    fun `register trims fields and bio before delegating to repository`() = runTest {
        val result = session.register(
            firstName = "  Jan  ",
            lastName = "  Kowalski\n",
            email = "  jan.kowalski@example.com  ",
            password = " Password1! ",
            confirmPassword = " Password1! ",
            bio = "  Lubię mapy  ",
            isPublic = true
        )

        assertEquals(RegistrationResult.Success, result)
        assertEquals(1, repository.registerCalls)
        assertEquals("Jan", repository.lastRegisteredFirstName)
        assertEquals("Kowalski", repository.lastRegisteredLastName)
        assertEquals("jan.kowalski@example.com", repository.lastRegisteredEmail)
        assertEquals(" Password1! ", repository.lastRegisteredPassword)
        assertEquals(" Password1! ", repository.lastRegisteredConfirmPassword)
        assertEquals("Lubię mapy", repository.lastRegisteredBio)
        assertEquals(true, repository.lastRegisteredIsPublic)
    }

    @Test
    fun `register keeps null bio as null and propagates failure`() = runTest {
        val expectedFailure = RegistrationResult.Failure(
            error = RegistrationError.RATE_LIMITED,
            message = "Too many requests"
        )
        repository.registerResult = expectedFailure

        val result = session.register(
            firstName = "Anna",
            lastName = "Nowak",
            email = "anna@example.com",
            password = "Password1!",
            confirmPassword = "Password1!",
            bio = null,
            isPublic = false
        )

        assertEquals(expectedFailure, result)
        assertNull(repository.lastRegisteredBio)
        assertEquals(false, repository.lastRegisteredIsPublic)
    }
}