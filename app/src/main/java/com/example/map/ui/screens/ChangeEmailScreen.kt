package com.example.map.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.map.ui.components.Header
import com.example.map.ui.components.PrimaryButton
import com.example.map.ui.theme.BackgroundDark
import com.example.map.ui.theme.ButtonLight
import com.example.map.ui.theme.ErrorRed
import com.example.map.ui.theme.SurfaceGray
import com.example.map.ui.theme.TextGray
import com.example.map.ui.viewmodels.ChangeEmailStep
import com.example.map.ui.viewmodels.ChangeEmailUiState

@Composable
fun ChangeEmailScreen(
    uiState: ChangeEmailUiState,
    onNewEmailChange: (String) -> Unit,
    onCodeChange: (String) -> Unit,
    onSubmitEmail: () -> Unit,
    onSubmitCode: () -> Unit,
    onBackToEmail: () -> Unit,
    onDone: () -> Unit,
    onBackClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
    ) {
        Header(title = "Zmiana e-mail", onBackClick = onBackClick)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
        ) {
            when (uiState.step) {
                ChangeEmailStep.EMAIL_ENTRY -> EmailEntryStep(
                    uiState = uiState,
                    onNewEmailChange = onNewEmailChange,
                    onSubmitEmail = onSubmitEmail
                )

                ChangeEmailStep.CODE_VERIFICATION -> VerificationStep(
                    uiState = uiState,
                    onCodeChange = onCodeChange,
                    onSubmitCode = onSubmitCode,
                    onBackToEmail = onBackToEmail
                )

                ChangeEmailStep.SUCCESS -> SuccessStep(
                    uiState = uiState,
                    onDone = onDone
                )
            }
        }
    }
}

@Composable
private fun EmailEntryStep(
    uiState: ChangeEmailUiState,
    onNewEmailChange: (String) -> Unit,
    onSubmitEmail: () -> Unit
) {
    Text(text = "Zmiana adresu e-mail", color = Color.White, fontSize = 22.sp)
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = "Na nowy adres wyślemy 6-cyfrowy kod weryfikacyjny. " +
            "Obecny adres pozostanie aktywny do momentu potwierdzenia zmiany.",
        color = TextGray,
        fontSize = 14.sp
    )

    Spacer(modifier = Modifier.height(24.dp))
    Text(text = "Obecny adres", color = TextGray, fontSize = 12.sp)
    Text(
        text = uiState.currentEmail.ifBlank { "Nie ustawiono" },
        color = Color.White,
        fontSize = 16.sp
    )

    Spacer(modifier = Modifier.height(24.dp))
    EmailChangeTextField(
        value = uiState.newEmail,
        onValueChange = onNewEmailChange,
        placeholder = "Nowy adres e-mail",
        keyboardType = KeyboardType.Email,
        isError = uiState.emailValidationError != null,
        supportingText = uiState.emailValidationError,
        enabled = !uiState.isSubmitting,
        onDone = onSubmitEmail
    )

    if (uiState.errorMessage != null) {
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = uiState.errorMessage, color = ErrorRed, fontSize = 14.sp)
    }

    Spacer(modifier = Modifier.height(24.dp))
    PrimaryButton(
        text = "Wyślij kod weryfikacyjny",
        onClick = onSubmitEmail,
        modifier = Modifier.fillMaxWidth(),
        enabled = uiState.canSubmitEmail,
        isLoading = uiState.isSubmitting
    )
}

@Composable
private fun VerificationStep(
    uiState: ChangeEmailUiState,
    onCodeChange: (String) -> Unit,
    onSubmitCode: () -> Unit,
    onBackToEmail: () -> Unit
) {
    Text(text = "Potwierdź nowy adres", color = Color.White, fontSize = 22.sp)
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = "Wysłaliśmy 6-cyfrowy kod na adres:",
        color = TextGray,
        fontSize = 14.sp
    )
    Text(text = uiState.normalizedNewEmail, color = Color.White, fontSize = 16.sp)
    Spacer(modifier = Modifier.height(8.dp))
    Text(text = "Kod jest ważny 15 minut.", color = TextGray, fontSize = 14.sp)

    Spacer(modifier = Modifier.height(24.dp))
    EmailChangeTextField(
        value = uiState.code,
        onValueChange = onCodeChange,
        placeholder = "Kod weryfikacyjny",
        keyboardType = KeyboardType.Number,
        isError = uiState.errorMessage != null,
        supportingText = uiState.errorMessage,
        enabled = !uiState.isSubmitting,
        onDone = onSubmitCode
    )

    Spacer(modifier = Modifier.height(24.dp))
    PrimaryButton(
        text = "Potwierdź zmianę",
        onClick = onSubmitCode,
        modifier = Modifier.fillMaxWidth(),
        enabled = uiState.canSubmitCode,
        isLoading = uiState.isSubmitting
    )

    Spacer(modifier = Modifier.height(8.dp))
    TextButton(
        onClick = onBackToEmail,
        enabled = !uiState.isSubmitting,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(text = "Zmień adres e-mail", color = TextGray)
    }
}

@Composable
private fun SuccessStep(
    uiState: ChangeEmailUiState,
    onDone: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = ButtonLight,
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "Adres e-mail został zmieniony", color = Color.White, fontSize = 22.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = uiState.normalizedNewEmail, color = TextGray, fontSize = 16.sp)
        Spacer(modifier = Modifier.height(32.dp))
        PrimaryButton(
            text = "Wróć do ustawień",
            onClick = onDone,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun EmailChangeTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    keyboardType: KeyboardType,
    isError: Boolean = false,
    supportingText: String? = null,
    enabled: Boolean = true,
    onDone: () -> Unit = {}
) {
    val supportingMessage = supportingText

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(text = placeholder, color = TextGray) },
        singleLine = true,
        enabled = enabled,
        isError = isError,
        supportingText = supportingMessage?.let { message ->
            { Text(text = message, color = ErrorRed) }
        },
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboardType,
            imeAction = ImeAction.Done
        ),
        keyboardActions = KeyboardActions(onDone = { onDone() }),
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceGray, RoundedCornerShape(8.dp)),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Color.Transparent,
            unfocusedBorderColor = Color.Transparent,
            disabledBorderColor = Color.Transparent,
            errorBorderColor = ErrorRed
        )
    )
}
