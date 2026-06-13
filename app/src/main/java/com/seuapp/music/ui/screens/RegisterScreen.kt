package com.seuapp.music.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.seuapp.music.ui.theme.*

@Composable
fun RegisterScreen(onRegister: (name: String, email: String, password: String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var showError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    val nameFocusRequester = remember { FocusRequester() }
    val emailFocusRequester = remember { FocusRequester() }
    val passwordFocusRequester = remember { FocusRequester() }
    val confirmPasswordFocusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    LaunchedEffect(Unit) {
        nameFocusRequester.requestFocus()
    }

    Box(
        modifier = Modifier.fillMaxSize().background(
            Brush.radialGradient(
                colors = listOf(NeonPurple.copy(alpha = 0.3f), ElectricCyanDim.copy(alpha = 0.1f), DeepNavy),
                center = Offset(500f, 300f), radius = 1200f
            )
        )
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                "Groovix",
                fontSize = 42.sp,
                fontWeight = FontWeight.Bold,
                color = ElectricCyan
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Crie sua conta",
                style = MaterialTheme.typography.bodyLarge,
                color = SteelText
            )
            Spacer(Modifier.height(40.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                modifier = Modifier.fillMaxWidth().focusRequester(nameFocusRequester),
                placeholder = { Text("Seu nome", color = MutedSteel) },
                leadingIcon = { Icon(Icons.Default.Person, null, tint = ElectricCyan) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { emailFocusRequester.requestFocus() }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ElectricCyan,
                    unfocusedBorderColor = SteelNavyLight,
                    focusedTextColor = OffWhite,
                    unfocusedTextColor = OffWhite,
                    cursorColor = ElectricCyan,
                    focusedContainerColor = SurfaceHigh.copy(alpha = 0.5f),
                    unfocusedContainerColor = SurfaceHigh.copy(alpha = 0.3f)
                ),
                shape = RoundedCornerShape(16.dp)
            )
            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                modifier = Modifier.fillMaxWidth().focusRequester(emailFocusRequester),
                placeholder = { Text("Seu email", color = MutedSteel) },
                leadingIcon = { Icon(Icons.Default.Email, null, tint = ElectricCyan) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { passwordFocusRequester.requestFocus() }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ElectricCyan,
                    unfocusedBorderColor = SteelNavyLight,
                    focusedTextColor = OffWhite,
                    unfocusedTextColor = OffWhite,
                    cursorColor = ElectricCyan,
                    focusedContainerColor = SurfaceHigh.copy(alpha = 0.5f),
                    unfocusedContainerColor = SurfaceHigh.copy(alpha = 0.3f)
                ),
                shape = RoundedCornerShape(16.dp)
            )
            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                modifier = Modifier.fillMaxWidth().focusRequester(passwordFocusRequester),
                placeholder = { Text("Senha", color = MutedSteel) },
                leadingIcon = { Icon(Icons.Default.Lock, null, tint = ElectricCyan) },
                trailingIcon = {
                    IconButton(onClick = { showPassword = !showPassword }) {
                        Icon(
                            if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            null, tint = MutedSteel
                        )
                    }
                },
                singleLine = true,
                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { confirmPasswordFocusRequester.requestFocus() }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ElectricCyan,
                    unfocusedBorderColor = SteelNavyLight,
                    focusedTextColor = OffWhite,
                    unfocusedTextColor = OffWhite,
                    cursorColor = ElectricCyan,
                    focusedContainerColor = SurfaceHigh.copy(alpha = 0.5f),
                    unfocusedContainerColor = SurfaceHigh.copy(alpha = 0.3f)
                ),
                shape = RoundedCornerShape(16.dp)
            )
            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                modifier = Modifier.fillMaxWidth().focusRequester(confirmPasswordFocusRequester),
                placeholder = { Text("Confirmar senha", color = MutedSteel) },
                leadingIcon = { Icon(Icons.Default.Lock, null, tint = ElectricCyan) },
                singleLine = true,
                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    focusManager.clearFocus()
                    when {
                        name.isBlank() -> { showError = true; errorMessage = "Preencha seu nome" }
                        email.isBlank() -> { showError = true; errorMessage = "Preencha seu email" }
                        !email.contains("@") -> { showError = true; errorMessage = "Email inválido" }
                        password.length < 4 -> { showError = true; errorMessage = "Senha deve ter no mínimo 4 caracteres" }
                        password != confirmPassword -> { showError = true; errorMessage = "As senhas não conferem" }
                        else -> { onRegister(name, email, password) }
                    }
                }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ElectricCyan,
                    unfocusedBorderColor = SteelNavyLight,
                    focusedTextColor = OffWhite,
                    unfocusedTextColor = OffWhite,
                    cursorColor = ElectricCyan,
                    focusedContainerColor = SurfaceHigh.copy(alpha = 0.5f),
                    unfocusedContainerColor = SurfaceHigh.copy(alpha = 0.3f)
                ),
                shape = RoundedCornerShape(16.dp)
            )

            if (showError) {
                Spacer(Modifier.height(8.dp))
                Text(errorMessage, color = ErrorRed, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
            }

            Spacer(Modifier.height(32.dp))

            Surface(
                onClick = {
                    when {
                        name.isBlank() -> { showError = true; errorMessage = "Preencha seu nome" }
                        email.isBlank() -> { showError = true; errorMessage = "Preencha seu email" }
                        !email.contains("@") -> { showError = true; errorMessage = "Email inválido" }
                        password.length < 4 -> { showError = true; errorMessage = "Senha deve ter no mínimo 4 caracteres" }
                        password != confirmPassword -> { showError = true; errorMessage = "As senhas não conferem" }
                        else -> { onRegister(name, email, password) }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                color = ElectricCyan
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("Cadastrar", color = OnCyanDark, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            }
        }
    }
}
