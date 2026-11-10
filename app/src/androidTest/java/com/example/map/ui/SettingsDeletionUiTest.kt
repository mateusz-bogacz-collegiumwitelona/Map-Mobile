package com.example.map.ui

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasProgressBarRangeInfo
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.map.domain.model.AccountDeletionError
import com.example.map.domain.model.AccountDeletionResult
import com.example.map.domain.usecase.AccountSession
import com.example.map.testing.FakeAccountRepository
import com.example.map.testing.FakeProfileStorage
import com.example.map.ui.screens.SettingsScreen
import com.example.map.ui.viewmodels.SettingsViewModel
import kotlinx.coroutines.CompletableDeferred
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsDeletionUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var repository: FakeAccountRepository
    private lateinit var profileStorage: FakeProfileStorage
    private lateinit var viewModel: SettingsViewModel

    @Before
    fun setUp() {
        repository = FakeAccountRepository()
        profileStorage = FakeProfileStorage("user@example.com")
        viewModel = SettingsViewModel(AccountSession(repository, profileStorage))

        composeRule.setContent {
            val uiState by viewModel.uiState.collectAsState()
            SettingsScreen(
                uiState = uiState,
                onBackClick = {},
                onChangeEmailClick = {},
                onDeleteAccountClick = viewModel::showDeleteDialog,
                onDismissDeleteDialog = viewModel::dismissDeleteDialog,
                onConfirmDeleteAccount = viewModel::deleteAccount
            )
        }
    }

    @Test
    fun deleteRowOpensWarningWithoutCallingApi() {
        composeRule.onNodeWithTag("settings_delete_row").performClick()

        composeRule.onNodeWithText("Usunąć konto?").assertIsDisplayed()
        assertTrue(viewModel.uiState.value.isDeleteDialogVisible)
        assertEquals(0, repository.deletionCalls)
    }

    @Test
    fun cancelClosesDialogWithoutCallingApiOrClearingProfile() {
        composeRule.onNodeWithTag("settings_delete_row").performClick()
        composeRule.onNodeWithTag("settings_delete_cancel").performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            !viewModel.uiState.value.isDeleteDialogVisible
        }
        composeRule.onAllNodesWithText("Usunąć konto?").assertCountEquals(0)
        assertEquals(0, repository.deletionCalls)
        assertEquals("user@example.com", profileStorage.getEmail())
    }

    @Test
    fun confirmCallsDeletionApiAndReportsDeleted() {
        composeRule.onNodeWithTag("settings_delete_row").performClick()
        composeRule.onNodeWithTag("settings_delete_confirm").performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            viewModel.uiState.value.isAccountDeleted
        }
        assertEquals(1, repository.deletionCalls)
        assertFalse(viewModel.uiState.value.isDeleteDialogVisible)
        composeRule.onAllNodesWithText("Usunąć konto?").assertCountEquals(0)
    }

    @Test
    fun failureKeepsDialogOpenAndShowsErrorInsideIt() {
        repository.deletionResult =
            AccountDeletionResult.Failure(AccountDeletionError.NETWORK)

        composeRule.onNodeWithTag("settings_delete_row").performClick()
        composeRule.onNodeWithTag("settings_delete_confirm").performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            viewModel.uiState.value.deleteErrorMessage != null
        }
        composeRule.onNodeWithText("Usunąć konto?").assertIsDisplayed()
        composeRule.onNodeWithText("Brak połączenia z internetem. Spróbuj ponownie.")
            .assertIsDisplayed()
        assertTrue(viewModel.uiState.value.isDeleteDialogVisible)
        assertFalse(viewModel.uiState.value.isAccountDeleted)
    }

    @Test
    fun loadingDisablesDialogActionsAndShowsProgress() {
        repository.deletionGate = CompletableDeferred()

        composeRule.onNodeWithTag("settings_delete_row").performClick()
        composeRule.onNodeWithTag("settings_delete_confirm").performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            viewModel.uiState.value.isDeletingAccount
        }
        composeRule.onNodeWithTag("settings_delete_confirm").assertIsNotEnabled()
        composeRule.onNodeWithTag("settings_delete_cancel").assertIsNotEnabled()
        composeRule.onNode(
            hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate)
        ).assertIsDisplayed()

        composeRule.runOnIdle {
            repository.deletionGate?.complete(Unit)
        }
        composeRule.waitUntil(timeoutMillis = 5_000) {
            viewModel.uiState.value.isAccountDeleted
        }
    }
}
