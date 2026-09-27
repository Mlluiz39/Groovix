package com.seuapp.music.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.seuapp.music.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: com.seuapp.music.ui.viewmodel.MusicViewModel, onBack: () -> Unit) {
    val userName by viewModel.userName.collectAsState()
    val userEmail by viewModel.userEmail.collectAsState()
    val backendUrl by viewModel.backendUrl.collectAsState()
    val backendStatus by viewModel.backendStatus.collectAsState()
    val configStatus by viewModel.configStatus.collectAsState()

    var name by remember { mutableStateOf(userName) }
    var email by remember { mutableStateOf(userEmail) }
    var saved by remember { mutableStateOf(false) }
    var backendInput by remember { mutableStateOf(backendUrl) }
    var backendInvalid by remember { mutableStateOf(false) }

    LaunchedEffect(userName, userEmail) {
        name = userName
        email = userEmail
    }
    LaunchedEffect(backendUrl) {
        backendInput = backendUrl
        backendInvalid = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepNavy)
            .padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(Modifier.height(20.dp))

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(onClick = onBack, shape = CircleShape, color = NavyGlass.copy(alpha = 0.6f)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar", modifier = Modifier.padding(8.dp).size(24.dp), tint = OffWhite)
            }
            Text("Configurações", style = MaterialTheme.typography.titleLarge, color = OffWhite)
            Spacer(modifier = Modifier.size(40.dp))
        }

        Spacer(Modifier.height(32.dp))

        Text("Seus Dados", style = MaterialTheme.typography.titleMedium, color = ElectricCyan)
        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = name,
            onValueChange = { name = it; saved = false },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Seu nome", color = MutedSteel) },
            leadingIcon = { Icon(Icons.Default.Person, null, tint = ElectricCyan) },
            singleLine = true,
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
            onValueChange = { email = it; saved = false },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Seu email", color = MutedSteel) },
            leadingIcon = { Icon(Icons.Default.Email, null, tint = ElectricCyan) },
            singleLine = true,
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

        Spacer(Modifier.height(24.dp))

        if (saved) {
            Text("Salvo com sucesso!", color = ElectricCyan, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(8.dp))
        }

        Surface(
            onClick = {
                viewModel.updateUser(name, email)
                saved = true
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp),
            color = ElectricCyan
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text("Salvar", color = OnCyanDark, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        }

        // ---------- Servidor (Backend) ----------

        Spacer(Modifier.height(36.dp))

        Text("Servidor (Backend)", style = MaterialTheme.typography.titleMedium, color = ElectricCyan)
        Spacer(Modifier.height(8.dp))
        Text(
            "URL da API que resolve busca e stream. " +
                "O servidor mudou? Troque aqui na hora, sem reinstalar o app.",
            style = MaterialTheme.typography.labelSmall,
            color = MutedSteel
        )
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = backendInput,
            onValueChange = {
                backendInput = it
                backendInvalid = false
                viewModel.clearBackendStatus()
            },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("https://meu-servidor.com/", color = MutedSteel) },
            leadingIcon = { Icon(Icons.Default.Link, null, tint = ElectricCyan) },
            isError = backendInvalid,
            supportingText = if (backendInvalid) {
                { Text("URL inválida — use algo como https://meu-servidor.com", color = SoftCoral) }
            } else null,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
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

        Spacer(Modifier.height(12.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Surface(
                onClick = {
                    backendInvalid = !viewModel.saveAndTestBackend(backendInput)
                },
                modifier = Modifier.weight(1f).height(52.dp),
                shape = RoundedCornerShape(16.dp),
                color = ElectricCyan
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("Salvar e testar", color = OnCyanDark, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
            Surface(
                onClick = { backendInvalid = false; viewModel.resetBackendUrl() },
                modifier = Modifier.height(52.dp),
                shape = RoundedCornerShape(16.dp),
                color = NavyGlass,
                border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.4f))
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 18.dp)) {
                    Text("Padrão", color = ElectricCyan, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }

        backendStatus?.let { status ->
            Spacer(Modifier.height(10.dp))
            val color = when {
                status.startsWith("✅") -> ElectricCyan
                status.startsWith("❌") -> SoftCoral
                else -> MutedSteel
            }
            Text(status, color = color, style = MaterialTheme.typography.bodySmall)
        }

        // Flags de estratégia que o servidor manda (config remota /config)
        configStatus?.let { cfg ->
            Spacer(Modifier.height(6.dp))
            Text(cfg, color = MutedSteel, style = MaterialTheme.typography.labelSmall)
        }

        Spacer(Modifier.height(32.dp))
    }
}
