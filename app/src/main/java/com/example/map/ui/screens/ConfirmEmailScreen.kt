package com.example.map.ui.screens

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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.map.ui.components.PrimaryButton
import com.example.map.ui.theme.BackgroundDark
import com.example.map.ui.theme.ButtonLight
import com.example.map.ui.theme.ErrorRed
import com.example.map.ui.theme.SurfaceGray
import com.example.map.ui.theme.TextGray
import com.example.map.ui.viewmodels.ConfirmEmailUiState

@Composable
fun ConfirmEmailScreen(
    uiState: ConfirmEmailUiState,
    onCodeChange: (String) -> Unit,
    onConfirmClick: () -> Unit,
    onResendClick: () -> Unit,
    onBackClick: () -> Unit
) {
    val isBusy = uiState.isSubmitting || uiState.isResending || uiState.isVerified

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
            Text(
                text = "Potwierdzenie e-maila",
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
                    text = "Wpisz kod z wiadomości",
                    color = TextGray,
                    fontSize = 24.sp
                )

                Button(
                    onClick = onBackClick,
                    enabled = !isBusy,
                    modifier = Modifier
                        .size(44.dp)
                        .testTag("confirm_email_back_button"),
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

            Text(
                text = "Kod weryfikacyjny został wysłany na adres:",
                color = TextGray,
                fontSize = 14.sp
            )
            Text(
                text = uiState.email.ifBlank { "Brak adresu w pamięci" },
                color = Color.White,
                fontSize = 16.sp,
                modifier = Modifier.testTag("confirm_email_address")
            )

            Spacer(modifier = Modifier.height(4.dp))

            OutlinedTextField(
                value = uiState.code,
                onValueChange = onCodeChange,
                label = { Text("Kod weryfikacyjny") },
                singleLine = true,
                enabled = !isBusy,
                isError = uiState.codeError != null,
                supportingText = uiState.codeError?.let { msg -> { Text(msg) } },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = { onConfirmClick() }),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("confirm_email_code"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    disabledTextColor = Color.White.copy(alpha = 0.6f),
                    focusedContainerColor = SurfaceGray,
                    unfocusedContainerColor = SurfaceGray,
                    disabledContainerColor = SurfaceGray.copy(alpha = 0.6f),
                    focusedLabelColor = TextGray,
                    unfocusedLabelColor = TextGray,
                    cursorColor = ButtonLight,
                    errorTextColor = Color.White,
                    errorBorderColor = ErrorRed,
                    errorSupportingTextColor = ErrorRed
                )
            )

            uiState.infoMessage?.let { info ->
                Text(
                    text = info,
                    color = ButtonLight,
                    fontSize = 14.sp,
                    modifier = Modifier.testTag("confirm_email_info")
                )
            }

            uiState.errorMessage?.let { error ->
                Text(
                    text = error,
                    color = ErrorRed,
                    fontSize = 14.sp,
                    modifier = Modifier.testTag("confirm_email_error")
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            PrimaryButton(
                text = "Potwierdź e-mail",
                onClick = onConfirmClick,
                enabled = !isBusy && uiState.email.isNotBlank(),
                isLoading = uiState.isSubmitting,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("confirm_email_submit")
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onResendClick,
                    enabled = !isBusy && uiState.email.isNotBlank(),
                    modifier = Modifier.testTag("confirm_email_resend")
                ) {
                    Text(
                        text = if (uiState.isResending) {
                            "Wysyłanie nowego kodu..."
                        } else {
                            "Wyślij kod ponownie"
                        },
                        color = TextGray,
                        fontSize = 14.sp
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
            }
        }
    }
}