package com.example.team_23_kotlin.presentation.auth

import android.icu.text.SimpleDateFormat
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.team_23_kotlin.R
import com.example.team_23_kotlin.ui.theme.Montserrat
import java.util.*

val MAJORS = listOf(
    "Administración", "Antropología", "Arquitectura", "Arte", "Biología",
    "Ciencia Política", "Contaduría Internacional", "Derecho", "Diseño",
    "Economía", "Educación Infantil", "Estudios Globales", "Física",
    "Filosofía", "Geociencias", "Gobierno y Asuntos Públicos", "Historia",
    "Historia del Arte", "Ingeniería Ambiental", "Ingeniería Biomédica",
    "Ingeniería Civil", "Ingeniería de Alimentos", "Ingeniería de Sistemas y Computación",
    "Ingeniería Eléctrica", "Ingeniería Electrónica", "Ingeniería Industrial",
    "Ingeniería Mecánica", "Ingeniería Química", "Lenguas y Cultura",
    "Literatura", "Matemáticas", "Medicina", "Microbiología", "Música",
    "Narrativas Digitales", "Psicología", "Química", "Licenciatura en Artes",
    "Licenciatura en Biología", "Licenciatura en Español y Filología",
    "Licenciatura en Filosofía", "Licenciatura en Física", "Licenciatura en Historia",
    "Licenciatura en Matemáticas", "Licenciatura en Química"
)

data class SignUpForm(
    val contact_preferences: String,
    val created_at_local: String,
    val email: String,
    val is_verified: Boolean,
    val name: String,
    val password: String,
    val role: String,
    val major: String
)

@Composable
fun SignUpScreen(
    onSubmit: (SignUpForm) -> Unit,
    onGoToLogin: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var isVerified by remember { mutableStateOf(false) }
    var role by remember { mutableStateOf("student") }
    var contactPref by remember { mutableStateOf("Push Notifications") }
    var major by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    val yellow = MaterialTheme.colorScheme.secondary
    val bg = MaterialTheme.colorScheme.background
    val black = Color(0xFF121212)
    val fieldBg = Color(0xFFF0F0F0)

    val createdAtDisplay = remember {
        val sdf = SimpleDateFormat("d 'de' MMMM 'de' yyyy, h:mm:ss a 'UTC-5'", Locale.getDefault())
        sdf.format(Date())
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = bg
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()) // ✅ scroll
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(30.dp))

            Image(
                painter = painterResource(id = R.drawable.ic_logo_goat),
                contentDescription = "Logo Mercandes",
                modifier = Modifier.size(150.dp),
                contentScale = ContentScale.Fit
            )

            Spacer(Modifier.height(20.dp))

            Text(
                text = "MERCANDES",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.5.sp,
                    textAlign = TextAlign.Center
                ),
                color = yellow
            )

            Spacer(Modifier.height(28.dp))

            Text(
                text = "Create Account",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                fontSize = 35.sp,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(Modifier.height(24.dp))

            // ===== Name =====
            FieldLabel("Name")
            TextField(
                value = name,
                onValueChange = { name = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("Alice Example") },
                colors = textFieldColors(fieldBg)
            )

            Spacer(Modifier.height(14.dp))

            // ===== Email =====
            FieldLabel("Email")
            TextField(
                value = email,
                onValueChange = { email = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("you@uniandes.edu.co") },
                colors = textFieldColors(fieldBg)
            )

            Spacer(Modifier.height(14.dp))

            // ===== Password =====
            FieldLabel("Password")
            TextField(
                value = password,
                onValueChange = { password = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("••••••••") },
                visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                        Icon(
                            imageVector = if (isPasswordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                            contentDescription = "Toggle password"
                        )
                    }
                },
                colors = textFieldColors(fieldBg)
            )

            Spacer(Modifier.height(14.dp))

            // ===== Major =====
            FieldLabel("Major")
            MajorDropdown(selected = major, onSelect = { major = it }, containerColor = fieldBg)

            Spacer(Modifier.height(14.dp))

            // ===== Role =====
            FieldLabel("Role")
            RoleDropdown(selected = role, onSelect = { role = it }, containerColor = fieldBg)

            Spacer(Modifier.height(14.dp))

            // ===== Contact Preference =====
            FieldLabel("Contact Preference")
            ContactPrefDropdown(selected = contactPref, onSelect = { contactPref = it }, containerColor = fieldBg)

            Spacer(Modifier.height(32.dp))

            // ===== Sign Up Button =====
            Button(
                onClick = {
                    isLoading = true
                    onSubmit(
                        SignUpForm(
                            contact_preferences = contactPref,
                            created_at_local = createdAtDisplay,
                            email = email,
                            is_verified = isVerified,
                            name = name,
                            password = password,
                            role = role,
                            major = major
                        )
                    )
                    isLoading = false
                },
                enabled = !isLoading && major.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(65.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = black, contentColor = Color.White)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.5.dp,
                        color = Color.White
                    )
                } else {
                    Text(
                        "Sign Up",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                        fontSize = 30.sp
                    )
                }
            }

            Spacer(Modifier.height(40.dp))

            // ===== Footer =====
            Row(
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "Already have an account?  ",
                    fontSize = 15.sp,
                    fontFamily = Montserrat,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    "Sign In",
                    color = Color(0, 0, 255),
                    fontFamily = Montserrat,
                    fontWeight = FontWeight.W600,
                    fontSize = 15.sp,
                    modifier = Modifier.clickable { onGoToLogin() }
                )
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(text, style = MaterialTheme.typography.labelLarge, modifier = Modifier.fillMaxWidth())
    Spacer(Modifier.height(6.dp))
}

@Composable
private fun textFieldColors(containerColor: Color) = TextFieldDefaults.colors(
    focusedContainerColor = containerColor,
    unfocusedContainerColor = containerColor,
    disabledContainerColor = containerColor,
    focusedIndicatorColor = Color.Transparent,
    unfocusedIndicatorColor = Color.Transparent
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MajorDropdown(selected: String, onSelect: (String) -> Unit, containerColor: Color) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        TextField(
            value = selected,
            onValueChange = {},
            readOnly = true,
            modifier = Modifier.menuAnchor().fillMaxWidth(),
            placeholder = { Text("Selecciona tu carrera") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            colors = textFieldColors(containerColor),
            shape = RoundedCornerShape(10.dp)
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            MAJORS.forEach { item ->
                DropdownMenuItem(
                    text = { Text(item) },
                    onClick = {
                        onSelect(item)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoleDropdown(selected: String, onSelect: (String) -> Unit, containerColor: Color) {
    val roles = listOf("student", "seller")
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        TextField(
            value = selected,
            onValueChange = {},
            readOnly = true,
            modifier = Modifier.menuAnchor().fillMaxWidth(),
            placeholder = { Text("Select your role") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            colors = textFieldColors(containerColor),
            shape = RoundedCornerShape(10.dp)
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            roles.forEach { item ->
                DropdownMenuItem(
                    text = { Text(item.capitalize()) },
                    onClick = {
                        onSelect(item)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactPrefDropdown(selected: String, onSelect: (String) -> Unit, containerColor: Color) {
    val prefs = listOf("Push Notifications", "Email")
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        TextField(
            value = selected,
            onValueChange = {},
            readOnly = true,
            modifier = Modifier.menuAnchor().fillMaxWidth(),
            placeholder = { Text("Select your contact preference") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            colors = textFieldColors(containerColor),
            shape = RoundedCornerShape(10.dp)
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            prefs.forEach { item ->
                DropdownMenuItem(
                    text = { Text(item) },
                    onClick = {
                        onSelect(item)
                        expanded = false
                    }
                )
            }
        }
    }
}
