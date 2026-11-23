package com.example.map.domain.session

import com.example.map.domain.model.ProfileUpdateError
import com.example.map.domain.model.ProfileUpdateResult
import com.example.map.domain.model.UserProfile
import com.example.map.testing.FakeAccountRepository
import com.example.map.testing.FakeProfileStorage
import com.example.map.testing.FakeUserProfileRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class AccountSessionTest {

    private val storedProfile = UserProfile(
        firstName = "Jan",
        lastName = "Kowalski",
        isPublic = false
    )

    private fun session(
        repository: FakeUserProfileRepository = FakeUserProfileRepository(),
        storage: FakeProfileStorage = FakeProfileStorage(
            initialProfile = storedProfile,
            initialEmail = "user@example.com"
        )
    ): AccountSession = AccountSession(
        accountRepository = FakeAccountRepository(),
        profileRepository = repository,
        profileStorage = storage
    )

    @Test
    fun profileAndEmailAreLoadedFromStorage() {
        val session = session()

        assertEquals("user@example.com", session.email.value)
        assertEquals(storedProfile, session.profile.value)
    }

    @Test
    fun emptyStorageProducesEmptyProfile() {
        val session = session(storage = FakeProfileStorage())

        assertEquals("", session.email.value)
        assertEquals(UserProfile(), session.profile.value)
    }

    @Test
    fun rememberEmailNormalizesAndPersists() {
        val storage = FakeProfileStorage(
            initialProfile = storedProfile,
            initialEmail = "user@example.com"
        )
        val session = session(storage = storage)

        session.rememberEmail("  new@example.com ")

        assertEquals("new@example.com", storage.getEmail())
        assertEquals("new@example.com", session.email.value)
    }

    @Test
    fun blankEmailIsIgnored() {
        val session = session()

        session.rememberEmail("   ")

        assertEquals("user@example.com", session.email.value)
    }

    @Test
    fun successfulNameUpdatePersistsAndEmits() = runTest {
        val repository = FakeUserProfileRepository()
        val storage = FakeProfileStorage(
            initialProfile = storedProfile,
            initialEmail = "user@example.com"
        )
        val session = session(repository = repository, storage = storage)

        val result = session.updateNames("  Anna ", " Nowak  ")

        assertEquals(ProfileUpdateResult.Success, result)
        assertEquals("Anna" to "Nowak", repository.lastNames)
        assertEquals("Anna", session.profile.value.firstName)
        assertEquals("Nowak", session.profile.value.lastName)
        assertEquals("Anna", storage.getProfile()?.firstName)
    }

    @Test
    fun failedNameUpdateKeepsStoredProfile() = runTest {
        val repository = FakeUserProfileRepository().apply {
            updateNamesResult = ProfileUpdateResult.Failure(ProfileUpdateError.NETWORK)
        }
        val storage = FakeProfileStorage(
            initialProfile = storedProfile,
            initialEmail = "user@example.com"
        )
        val session = session(repository = repository, storage = storage)

        val result = session.updateNames("Anna", "Nowak")

        assertEquals(ProfileUpdateResult.Failure(ProfileUpdateError.NETWORK), result)
        assertEquals(storedProfile, session.profile.value)
        assertEquals(storedProfile, storage.getProfile())
    }

    @Test
    fun successfulVisibilityUpdatePersistsAndEmits() = runTest {
        val repository = FakeUserProfileRepository()
        val storage = FakeProfileStorage(
            initialProfile = storedProfile,
            initialEmail = "user@example.com"
        )
        val session = session(repository = repository, storage = storage)

        val result = session.updateVisibility(isPublic = true)

        assertEquals(ProfileUpdateResult.Success, result)
        assertEquals(true, repository.lastVisibility)
        assertEquals(true, session.profile.value.isPublic)
        assertEquals(true, storage.getProfile()?.isPublic)
    }

    @Test
    fun failedVisibilityUpdateKeepsStoredProfile() = runTest {
        val repository = FakeUserProfileRepository().apply {
            setVisibilityResult =
                ProfileUpdateResult.Failure(ProfileUpdateError.UNAUTHORIZED)
        }
        val storage = FakeProfileStorage(
            initialProfile = storedProfile,
            initialEmail = "user@example.com"
        )
        val session = session(repository = repository, storage = storage)

        session.updateVisibility(isPublic = true)

        assertFalse(session.profile.value.isPublic)
        assertEquals(storedProfile, storage.getProfile())
    }

    @Test
    fun refreshReplacesProfileFromBackend() = runTest {
        val repository = FakeUserProfileRepository().apply {
            remoteProfile = UserProfile(
                firstName = "Anna",
                lastName = "Nowak",
                isPublic = true
            )
        }
        val storage = FakeProfileStorage(
            initialProfile = storedProfile,
            initialEmail = "user@example.com"
        )
        val session = session(repository = repository, storage = storage)

        session.refreshProfile()

        val expected = UserProfile(
            firstName = "Anna",
            lastName = "Nowak",
            isPublic = true
        )
        assertEquals(expected, session.profile.value)
        assertEquals(expected, storage.getProfile())
        assertEquals("user@example.com", session.email.value)
    }

    @Test
    fun refreshFailureKeepsLocalProfile() = runTest {
        val repository = FakeUserProfileRepository().apply { remoteProfile = null }
        val session = session(repository = repository)

        session.refreshProfile()

        assertEquals(storedProfile, session.profile.value)
    }

    @Test
    fun clearRemovesStoredProfileAndEmail() {
        val storage = FakeProfileStorage(
            initialProfile = storedProfile,
            initialEmail = "user@example.com"
        )
        val session = session(storage = storage)

        session.clear()

        assertNull(storage.getProfile())
        assertNull(storage.getEmail())
        assertEquals(UserProfile(), session.profile.value)
        assertEquals("", session.email.value)
    }

    @Test
    fun lateRefreshResponseAfterClearDoesNotRepopulateSession() = runTest {
        val gate = CompletableDeferred<Unit>()
        val repository = FakeUserProfileRepository().apply {
            remoteProfile = UserProfile(
                firstName = "Anna",
                lastName = "Nowak",
                isPublic = true
            )
            getProfileGate = gate
        }
        val storage = FakeProfileStorage(
            initialProfile = storedProfile,
            initialEmail = "user@example.com"
        )
        val session = session(repository = repository, storage = storage)

        val refresh = launch { session.refreshProfile() }
        runCurrent()
        assertEquals(1, repository.getProfileCalls)

        session.clear()

        gate.complete(Unit)
        refresh.join()

        assertNull(storage.getProfile())
        assertNull(storage.getEmail())
        assertEquals(UserProfile(), session.profile.value)
        assertEquals("", session.email.value)
    }

    @Test
    fun nameUpdateFinishingAfterClearIsNotPersisted() = runTest {
        val gate = CompletableDeferred<Unit>()
        val repository = FakeUserProfileRepository().apply {
            updateNamesGate = gate
        }
        val storage = FakeProfileStorage(
            initialProfile = storedProfile,
            initialEmail = "user@example.com"
        )
        val session = session(repository = repository, storage = storage)

        val update = launch { session.updateNames("Anna", "Nowak") }
        runCurrent()

        session.clear()

        gate.complete(Unit)
        update.join()

        assertNull(storage.getProfile())
        assertEquals(UserProfile(), session.profile.value)
    }
}
