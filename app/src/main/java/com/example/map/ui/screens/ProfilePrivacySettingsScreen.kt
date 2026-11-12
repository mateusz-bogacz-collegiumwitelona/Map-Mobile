package com.example.map.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.map.ui.components.Header
import com.example.map.ui.components.PrimaryButton
import com.example.map.ui.theme.BackgroundDark
import com.example.map.ui.theme.ButtonLight
import com.example.map.ui.theme.ErrorRed
import com.example.map.ui.theme.SurfaceGray
import com.example.map.ui.theme.TextGray
import com.example.map.ui.viewmodels.ProfilePrivacySettingsUiState

@Composable
fun ProfilePrivacySettingsScreen(
    uiState: ProfilePrivacySettingsUiState,
    onFirstNameChange: (String) -> Unit,
    onLastNameChange: (String) -> Unit,
    onVisibilityChange: (Boolean) -> Unit,
    onSaveClick: () -> Unit,
    onBackClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
    ) {
        Header(title = "Profil i prywatność", onBackClick = onBackClick)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
        ) {
            Text(text = "Dane konta", color = TextGray, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(8.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurfaceGray, RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {
                Text(text = "Adres e-mail", color = TextGray, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = uiState.email.ifBlank { "Nie ustawiono" },
                    color = Color.White,
                    fontSize = 16.sp
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text(text = "Dane profilu", color = TextGray, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(8.dp))

            ProfileNameField(
                value = uiState.firstName,
                onValueChange = onFirstNameChange,
                label = "Imię",
                error = uiState.firstNameError,
                enabled = !uiState.isSaving,
                imeAction = ImeAction.Next,
                testTag = "profile_first_name"
            )

            Spacer(modifier = Modifier.height(12.dp))

            ProfileNameField(
                value = uiState.lastName,
                onValueChange = onLastNameChange,
                label = "Nazwisko",
                error = uiState.lastNameError,
                enabled = !uiState.isSaving,
                imeAction = ImeAction.Done,
                testTag = "profile_last_name"
            )

            Spacer(modifier = Modifier.height(24.dp))
            Text(text = "Prywatność", color = TextGray, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurfaceGray, RoundedCornerShape(12.dp))
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Widoczność profilu", color = Color.White, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (uiState.isPublic) {
                            "Profil publiczny – widzą go inni użytkownicy."
                        } else {
                            "Profil prywatny – widzisz go tylko Ty."
                        },
                        color = TextGray,
                        fontSize = 14.sp
                    )
                }

                Switch(
                    checked = uiState.isPublic,
                    onCheckedChange = onVisibilityChange,
                    enabled = !uiState.isSaving,
                    modifier = Modifier.testTag("profile_visibility_toggle"),
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = BackgroundDark,
                        checkedTrackColor = ButtonLight,
                        uncheckedThumbColor = TextGray,
                        uncheckedTrackColor = BackgroundDark,
                        uncheckedBorderColor = TextGray,
                        disabledCheckedThumbColor = BackgroundDark,
                        disabledCheckedTrackColor = ButtonLight.copy(alpha = 0.4f),
                        disabledUncheckedThumbColor = TextGray.copy(alpha = 0.4f),
                        disabledUncheckedTrackColor = BackgroundDark,
                        disabledUncheckedBorderColor = TextGray.copy(alpha = 0.4f)
                    )
                )
            }

            if (uiState.errorMessage != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = uiState.errorMessage,
                    color = ErrorRed,
                    fontSize = 14.sp,
                    modifier = Modifier.testTag("profile_error")
                )
            }

            if (uiState.successMessage != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = uiState.successMessage,
                    color = ButtonLight,
                    fontSize = 14.sp,
                    modifier = Modifier.testTag("profile_success")
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            PrimaryButton(
                text = "Zapisz",
                onClick = onSaveClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("profile_save"),
                enabled = uiState.canSave,
                isLoading = uiState.isSaving
            )
        }
    }
}

@Composable
private fun ProfileNameField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    error: String?,
    enabled: Boolean,
    imeAction: ImeAction,
    testTag: String
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(text = label, color = TextGray) },
        singleLine = true,
        enabled = enabled,
        isError = error != null,
        supportingText = error?.let { message ->
            { Text(text = message, color = ErrorRed) }
        },
        keyboardOptions = KeyboardOptions(imeAction = imeAction),
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceGray, RoundedCornerShape(8.dp))
            .testTag(testTag),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Color.Transparent,
            unfocusedBorderColor = Color.Transparent,
            disabledBorderColor = Color.Transparent,
            errorBorderColor = ErrorRed,
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            disabledTextColor = Color.White.copy(alpha = 0.6f),
            cursorColor = ButtonLight,
            errorSupportingTextColor = ErrorRed
        )
    )
}
