package com.example.map.domain.usecase

import com.example.map.domain.model.ProfileUpdateError
import com.example.map.domain.model.ProfileUpdateResult
import com.example.map.domain.model.UserProfile
import com.example.map.testing.FakeProfileStorage
import com.example.map.testing.FakeUserProfileRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class ProfileSessionTest {

    private val storedProfile = UserProfile(
        email = "user@example.com",
        firstName = "Jan",
        lastName = "Kowalski",
        isPublic = false
    )

    @Test
    fun profileIsLoadedFromStorage() {
        val session = ProfileSession(
            FakeUserProfileRepository(),
            FakeProfileStorage(storedProfile)
        )

        assertEquals(storedProfile, session.profile.value)
    }

    @Test
    fun emptyStorageProducesEmptyProfile() {
        val session = ProfileSession(FakeUserProfileRepository(), FakeProfileStorage())

        assertEquals(UserProfile(), session.profile.value)
    }

    @Test
    fun rememberEmailNormalizesAndPersists() {
        val storage = FakeProfileStorage(storedProfile)
        val session = ProfileSession(FakeUserProfileRepository(), storage)

        session.rememberEmail("  new@example.com ")

        assertEquals("new@example.com", storage.getEmail())
        assertEquals("new@example.com", session.profile.value.email)
    }

    @Test
    fun blankEmailIsIgnored() {
        val storage = FakeProfileStorage(storedProfile)
        val session = ProfileSession(FakeUserProfileRepository(), storage)

        session.rememberEmail("   ")

        assertEquals("user@example.com", session.profile.value.email)
    }

    @Test
    fun successfulNameUpdatePersistsAndEmits() = runTest {
        val repository = FakeUserProfileRepository()
        val storage = FakeProfileStorage(storedProfile)
        val session = ProfileSession(repository, storage)

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
        val storage = FakeProfileStorage(storedProfile)
        val session = ProfileSession(repository, storage)

        val result = session.updateNames("Anna", "Nowak")

        assertEquals(ProfileUpdateResult.Failure(ProfileUpdateError.NETWORK), result)
        assertEquals(storedProfile, session.profile.value)
        assertEquals(storedProfile, storage.getProfile())
    }

    @Test
    fun successfulVisibilityUpdatePersistsAndEmits() = runTest {
        val repository = FakeUserProfileRepository()
        val storage = FakeProfileStorage(storedProfile)
        val session = ProfileSession(repository, storage)

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
        val storage = FakeProfileStorage(storedProfile)
        val session = ProfileSession(repository, storage)

        session.updateVisibility(isPublic = true)

        assertFalse(session.profile.value.isPublic)
        assertFalse(storage.getProfile()!!.isPublic)
    }

    @Test
    fun refreshCopiesRemoteProfileAndKeepsEmail() = runTest {
        val repository = FakeUserProfileRepository().apply {
            remoteProfile = UserProfile(
                firstName = "Anna",
                lastName = "Nowak",
                isPublic = true
            )
        }
        val storage = FakeProfileStorage(storedProfile)
        val session = ProfileSession(repository, storage)

        session.refresh()

        val expected = storedProfile.copy(
            firstName = "Anna",
            lastName = "Nowak",
            isPublic = true
        )
        assertEquals(expected, session.profile.value)
        assertEquals(expected, storage.getProfile())
    }

    @Test
    fun refreshFailureKeepsLocalProfile() = runTest {
        val repository = FakeUserProfileRepository().apply { remoteProfile = null }
        val storage = FakeProfileStorage(storedProfile)
        val session = ProfileSession(repository, storage)

        session.refresh()

        assertEquals(storedProfile, session.profile.value)
    }

    @Test
    fun clearRemovesStoredProfile() {
        val storage = FakeProfileStorage(storedProfile)
        val session = ProfileSession(FakeUserProfileRepository(), storage)

        session.clear()

        assertNull(storage.getProfile())
        assertEquals(UserProfile(), session.profile.value)
    }
}
