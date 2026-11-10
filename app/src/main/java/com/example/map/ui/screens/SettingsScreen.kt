package com.example.map.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.map.ui.components.Header
import com.example.map.ui.theme.BackgroundDark
import com.example.map.ui.theme.ButtonLight
import com.example.map.ui.theme.DangerRed
import com.example.map.ui.theme.ErrorRed
import com.example.map.ui.theme.SurfaceGray
import com.example.map.ui.theme.TextGray
import com.example.map.ui.viewmodels.SettingsUiState

@Composable
fun SettingsScreen(
    uiState: SettingsUiState,
    onBackClick: () -> Unit,
    onChangeEmailClick: () -> Unit,
    onDeleteAccountClick: () -> Unit,
    onDismissDeleteDialog: () -> Unit,
    onConfirmDeleteAccount: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
    ) {
        Header(title = "Ustawienia", onBackClick = onBackClick)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text(text = "Konto", color = TextGray, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(SurfaceGray)
                    .testTag("settings_change_email_row")
                    .clickable(onClick = onChangeEmailClick)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Zmień adres e-mail", color = Color.White, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = uiState.email.ifBlank { "Nie ustawiono" },
                        color = TextGray,
                        fontSize = 14.sp
                    )
                }

                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = ButtonLight
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(text = "Strefa niebezpieczna", color = DangerRed, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(SurfaceGray)
                    .border(
                        width = 1.dp,
                        color = DangerRed.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .testTag("settings_delete_row")
                    .clickable(
                        enabled = !uiState.isDeletingAccount,
                        onClick = onDeleteAccountClick
                    )
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteForever,
                        contentDescription = null,
                        tint = DangerRed
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(text = "Usuń konto", color = DangerRed, fontSize = 16.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Trwale usuń konto i wszystkie dane",
                            color = TextGray,
                            fontSize = 14.sp
                        )
                    }
                }

                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = DangerRed
                )
            }
        }
    }

    if (uiState.isDeleteDialogVisible) {
        DeleteAccountDialog(
            isDeleting = uiState.isDeletingAccount,
            errorMessage = uiState.deleteErrorMessage,
            onDismiss = onDismissDeleteDialog,
            onConfirm = onConfirmDeleteAccount
        )
    }
}

@Composable
private fun DeleteAccountDialog(
    isDeleting: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = {
            if (!isDeleting) {
                onDismiss()
            }
        },
        title = {
            Text(text = "Usunąć konto?", color = Color.White, fontSize = 20.sp)
        },
        text = {
            Column {
                Text(
                    text = "Ta operacja jest trwała i nie można jej cofnąć. " +
                        "Usunięcie konta spowoduje utratę dostępu do Twoich podróży, " +
                        "miejsc i zapisanych danych.",
                    color = TextGray,
                    fontSize = 14.sp
                )

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(text = errorMessage, color = ErrorRed, fontSize = 14.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                modifier = Modifier.testTag("settings_delete_confirm"),
                enabled = !isDeleting,
                colors = ButtonDefaults.buttonColors(
                    containerColor = DangerRed,
                    contentColor = BackgroundDark,
                    disabledContainerColor = DangerRed.copy(alpha = 0.4f),
                    disabledContentColor = BackgroundDark.copy(alpha = 0.7f)
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                if (isDeleting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = BackgroundDark,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(text = "Usuń konto", fontSize = 14.sp)
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("settings_delete_cancel"),
                enabled = !isDeleting,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = TextGray,
                    disabledContentColor = TextGray.copy(alpha = 0.4f)
                )
            ) {
                Text(text = "Anuluj", fontSize = 14.sp)
            }
        },
        containerColor = SurfaceGray,
        titleContentColor = Color.White,
        textContentColor = TextGray
    )
}
