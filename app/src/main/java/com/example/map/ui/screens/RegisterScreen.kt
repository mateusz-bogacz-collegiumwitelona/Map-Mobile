package com.example.map.ui.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.map.R
import com.example.map.ui.components.PrimaryButton
import com.example.map.ui.theme.BackgroundDark
import com.example.map.ui.theme.ButtonLight
import com.example.map.ui.theme.SurfaceGray
import com.example.map.ui.theme.TextGray

@Composable
fun RegisterScreen(
    onRegisterSuccess: (email: String) -> Unit,
    onNavigateBackToLogin: () -> Unit,
    viewModel: RegisterViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var isPasswordVisible by rememberSaveable { mutableStateOf(false) }
    var isConfirmPasswordVisible by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(uiState.isRegistered) {
        if (uiState.isRegistered) {
            onRegisterSuccess(uiState.email.trim())
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(SurfaceGray)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = R.drawable.map_logo_white),
                contentDescription = "Logo Map",
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Rejestracja",
                color = Color.White,
                fontSize = 20.sp
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Utwórz konto",
                    color = TextGray,
                    fontSize = 26.sp
                )

                Button(
                    onClick = onNavigateBackToLogin,
                    enabled = !uiState.isLoading,
                    modifier = Modifier
                        .size(44.dp)
                        .testTag("register_back_button"),
                    contentPadding = PaddingValues(0.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ButtonLight,
                        contentColor = TextGray
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Wróć do logowania",
                        tint = TextGray
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            RegisterField(
                value = uiState.firstName,
                onValueChange = viewModel::onFirstNameChange,
                label = "Imię",
                error = uiState.firstNameError,
                enabled = !uiState.isLoading,
                testTag = "register_first_name"
            )

            RegisterField(
                value = uiState.lastName,
                onValueChange = viewModel::onLastNameChange,
                label = "Nazwisko",
                error = uiState.lastNameError,
                enabled = !uiState.isLoading,
                testTag = "register_last_name"
            )

            RegisterField(
                value = uiState.email,
                onValueChange = viewModel::onEmailChange,
                label = "E-mail",
                error = uiState.emailError,
                enabled = !uiState.isLoading,
                keyboardType = KeyboardType.Email,
                testTag = "register_email"
            )

            RegisterField(
                value = uiState.password,
                onValueChange = viewModel::onPasswordChange,
                label = "Hasło",
                error = uiState.passwordError,
                enabled = !uiState.isLoading,
                keyboardType = KeyboardType.Password,
                visualTransformation = if (isPasswordVisible) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                trailingIcon = {
                    IconButton(
                        onClick = { isPasswordVisible = !isPasswordVisible },
                        enabled = !uiState.isLoading
                    ) {
                        Icon(
                            imageVector = if (isPasswordVisible) {
                                Icons.Filled.VisibilityOff
                            } else {
                                Icons.Filled.Visibility
                            },
                            contentDescription = if (isPasswordVisible) {
                                "Ukryj hasło"
                            } else {
                                "Pokaż hasło"
                            }
                        )
                    }
                },
                testTag = "register_password"
            )

            RegisterField(
                value = uiState.confirmPassword,
                onValueChange = viewModel::onConfirmPasswordChange,
                label = "Powtórz hasło",
                error = uiState.confirmPasswordError,
                enabled = !uiState.isLoading,
                keyboardType = KeyboardType.Password,
                visualTransformation = if (isConfirmPasswordVisible) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                trailingIcon = {
                    IconButton(
                        onClick = { isConfirmPasswordVisible = !isConfirmPasswordVisible },
                        enabled = !uiState.isLoading
                    ) {
                        Icon(
                            imageVector = if (isConfirmPasswordVisible) {
                                Icons.Filled.VisibilityOff
                            } else {
                                Icons.Filled.Visibility
                            },
                            contentDescription = if (isConfirmPasswordVisible) {
                                "Ukryj powtórzone hasło"
                            } else {
                                "Pokaż powtórzone hasło"
                            }
                        )
                    }
                },
                testTag = "register_confirm_password"
            )

            RegisterField(
                value = uiState.bio,
                onValueChange = viewModel::onBioChange,
                label = "Bio (opcjonalne)",
                error = uiState.bioError,
                enabled = !uiState.isLoading,
                singleLine = false,
                minLines = 2,
                maxLines = 4,
                testTag = "register_bio"
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurfaceGray, RoundedCornerShape(8.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Profil publiczny",
                        color = Color.White,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "Inni użytkownicy mogą zobaczyć Twój profil",
                        color = TextGray,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Switch(
                    checked = uiState.isPublic,
                    onCheckedChange = viewModel::onIsPublicChange,
                    enabled = !uiState.isLoading,
                    modifier = Modifier.testTag("register_visibility_toggle")
                )
            }

            uiState.errorMessage?.let { error ->
                Text(
                    text = error,
                    color = Color(0xFFFF7777),
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            PrimaryButton(
                text = "Zarejestruj się",
                onClick = viewModel::register,
                enabled = !uiState.isLoading,
                isLoading = uiState.isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("register_submit")
            )
        }
    }
}

@Composable
private fun RegisterField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    error: String?,
    enabled: Boolean,
    testTag: String,
    singleLine: Boolean = true,
    minLines: Int = 1,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    keyboardType: KeyboardType = KeyboardType.Text,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailingIcon: (@Composable (() -> Unit))? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = singleLine,
        minLines = minLines,
        maxLines = maxLines,
        enabled = enabled,
        isError = error != null,
        supportingText = error?.let { msg -> { Text(msg) } },
        visualTransformation = visualTransformation,
        trailingIcon = trailingIcon,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            disabledTextColor = Color.White.copy(alpha = 0.6f),
            focusedContainerColor = SurfaceGray,
            unfocusedContainerColor = SurfaceGray,
            disabledContainerColor = SurfaceGray.copy(alpha = 0.6f),
            focusedLabelColor = TextGray,
            unfocusedLabelColor = TextGray,
            focusedTrailingIconColor = TextGray,
            unfocusedTrailingIconColor = TextGray,
            cursorColor = ButtonLight,
            errorTextColor = Color.White,
            errorBorderColor = Color(0xFFFF7777),
            errorSupportingTextColor = Color(0xFFFF7777)
        )
    )
}