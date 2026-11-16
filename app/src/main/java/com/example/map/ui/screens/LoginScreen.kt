package com.example.map.ui.screens

import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import com.example.map.R
import com.example.map.ui.theme.BackgroundDark
import com.example.map.ui.theme.ButtonLight
import com.example.map.ui.theme.SurfaceGray
import com.example.map.ui.theme.TextGray
import com.example.map.ui.viewmodels.AuthUiState

@Composable
fun LoginScreen(
    state: AuthUiState,
    onLoginClick: (email: String, password: String) -> Unit,
    onInputsChanged: () -> Unit
) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 26.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "My adventures\n& places",
            color = Color.White,
            fontSize = 28.sp,
            lineHeight = 32.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Image(
            painter = painterResource(id = R.drawable.map_logo_white),
            contentDescription = "Logo Map",
            modifier = Modifier.size(82.dp)
        )

        Spacer(modifier = Modifier.height(40.dp))

        LoginInputField(
            value = email,
            placeholder = "E-mail",
            enabled = !state.isLoading,
            errorMessage = state.emailError,
            keyboardType = KeyboardType.Email,
            imeAction = ImeAction.Next,
            onValueChange = {
                email = it
                onInputsChanged()
            }
        )

        Spacer(modifier = Modifier.height(22.dp))

        LoginInputField(
            value = password,
            placeholder = "Hasło",
            enabled = !state.isLoading,
            errorMessage = state.passwordError,
            keyboardType = KeyboardType.Password,
            imeAction = ImeAction.Done,
            visualTransformation = PasswordVisualTransformation(),
            onValueChange = {
                password = it
                onInputsChanged()
            }
        )

        state.errorMessage?.let { error ->
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = error,
                color = Color(0xFFFF7777),
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(42.dp))

        Button(
            onClick = { onLoginClick(email, password) },
            enabled = !state.isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp),
            contentPadding = PaddingValues(4.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = ButtonLight,
                contentColor = BackgroundDark
            ),
            shape = RoundedCornerShape(6.dp)
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    color = BackgroundDark,
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    text = "Zaloguj się",
                    fontSize = 18.sp,
                    color = BackgroundDark
                )
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            PlaceholderAction(
                label = "Nie masz\njeszcze konta?",
                buttonText = "Zarejestruj się",
                modifier = Modifier.weight(1f)
            )

            PlaceholderAction(
                label = "Zapomniałeś\nhasła?",
                buttonText = "Przypomnij hasło",
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun LoginInputField(
    value: String,
    placeholder: String,
    enabled: Boolean,
    errorMessage: String?,
    keyboardType: KeyboardType,
    imeAction: ImeAction,
    onValueChange: (String) -> Unit,
    visualTransformation: VisualTransformation = VisualTransformation.None
) {
    val shape = RoundedCornerShape(6.dp)

    Column(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(42.dp)
                .clip(shape)
                .background(SurfaceGray)
                .border(
                    width = 1.dp,
                    color = if (errorMessage == null) {
                        Color.Transparent
                    } else {
                        Color(0xFFFF7777)
                    },
                    shape = shape
                )
                .padding(horizontal = 8.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                enabled = enabled,
                singleLine = true,
                textStyle = TextStyle(
                    color = Color.White,
                    fontSize = 22.sp
                ),
                cursorBrush = SolidColor(ButtonLight),
                keyboardOptions = KeyboardOptions(
                    keyboardType = keyboardType,
                    imeAction = imeAction
                ),
                visualTransformation = visualTransformation,
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { innerTextField ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (value.isEmpty()) {
                            Text(
                                text = placeholder,
                                color = TextGray,
                                fontSize = 22.sp
                            )
                        }
                        innerTextField()
                    }
                }
            )
        }

        errorMessage?.let { error ->
            Text(
                text = error,
                color = Color(0xFFFF7777),
                fontSize = 12.sp,
                modifier = Modifier.padding(start = 4.dp, top = 4.dp)
            )
        }
    }
}

@Composable
private fun PlaceholderAction(
    label: String,
    buttonText: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            color = TextGray,
            fontSize = 14.sp,
            lineHeight = 18.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(6.dp))

        Button(
            onClick = {},
            modifier = Modifier
                .fillMaxWidth()
                .height(38.dp),
            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = ButtonLight,
                contentColor = TextGray
            ),
            shape = RoundedCornerShape(6.dp)
        ) {
            Text(
                text = buttonText,
                fontSize = 13.sp,
                maxLines = 1,
                textAlign = TextAlign.Center
            )
        }
    }
}