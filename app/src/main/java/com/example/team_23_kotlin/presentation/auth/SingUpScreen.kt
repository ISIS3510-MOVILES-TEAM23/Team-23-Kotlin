package com.example.team_23_kotlin.presentation.auth

import android.icu.text.SimpleDateFormat
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

data class SignUpForm(
    val contact_preferences: String,
    val created_at_local: String, // solo display; el server timestamp lo pones en backend
    val email: String,
    val is_verified: Boolean,
    val name: String,
    val password: String,
    val role: String
)

@Composable
fun SignUpScreen(
    onSubmit: (SignUpForm) -> Unit,
    onGoToLogin: () -> Unit
) {
    var name by remember { mutableStateOf("Alice Example") }
    var email by remember { mutableStateOf("student@uniandes.edu.co") }
    var password by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var isVerified by remember { mutableStateOf(false) } // solo UI; la verificación real es en backend
    var role by remember { mutableStateOf("student") }
    var contactPref by remember { mutableStateOf("push") }
    var isLoading by remember { mutableStateOf(false) }

    val yellow = MaterialTheme.colorScheme.secondary
    val bg = MaterialTheme.colorScheme.background
    val black = Color(0xFF121212)
    val fieldBg = Color(0xFFF0F0F0)

    val createdAtDisplay = remember {
        val sdf = SimpleDateFormat("d 'de' MMMM 'de' yyyy, h:mm:ss a 'UTC-5'", Locale.getDefault())
        sdf.format(Date())
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bg)
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
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
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.ExtraBold
                ),
                fontSize = 35.sp,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(Modifier.height(16.dp))

            // ===== Name =====
            FieldLabel("Name")
            TextField(
                value = name,
                onValueChange = { name = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("Alice Example") },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = fieldBg,
                    unfocusedContainerColor = fieldBg,
                    disabledContainerColor = fieldBg,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                shape = RoundedCornerShape(10.dp)
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
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = fieldBg,
                    unfocusedContainerColor = fieldBg,
                    disabledContainerColor = fieldBg,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                shape = RoundedCornerShape(10.dp)
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
                visualTransformation = if (isPasswordVisible) VisualTransformation.None
                else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                        Icon(
                            imageVector = if (isPasswordVisible) Icons.Filled.Visibility
                            else Icons.Filled.VisibilityOff,
                            contentDescription = "Toggle password"
                        )
                    }
                },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = fieldBg,
                    unfocusedContainerColor = fieldBg,
                    disabledContainerColor = fieldBg,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                shape = RoundedCornerShape(10.dp)
            )

            Spacer(Modifier.height(14.dp))




            // ===== Created at (solo display, no editable) =====
            //Spacer(Modifier.height(4.dp))
            /*
            Text(
                text = "Created at (local): $createdAtDisplay",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )*/

            Spacer(Modifier.height(28.dp))

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
                            role = role
                        )
                    )
                    isLoading = false
                },
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(65.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = black,
                    contentColor = Color.White
                )
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
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.ExtraBold
                        ),
                        fontSize = 30.sp
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

        }

        // Footer
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 5.dp),
        ) {
            Text(
                "Already have an account?  ",
                fontSize = 15.sp,
                textAlign = TextAlign.Center,
                fontFamily = Montserrat,
                fontWeight = FontWeight.Medium
            )
            Text(
                "Sign In",
                color = Color(0,0,255),
                fontFamily = Montserrat,
                fontWeight = FontWeight.W600,
                fontSize = 15.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.clickable { onGoToLogin() }
            )
        }
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 2.dp)
    )
    Spacer(Modifier.height(6.dp))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RoleDropdown(
    selected: String,
    onSelect: (String) -> Unit,
    containerColor: Color
) {
    var expanded by remember { mutableStateOf(false) }
    val items = listOf("student") // deja solo student por ahora

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        TextField(
            value = selected,
            onValueChange = {},
            readOnly = true,
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = containerColor,
                unfocusedContainerColor = containerColor,
                disabledContainerColor = containerColor,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            ),
            shape = RoundedCornerShape(10.dp),
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            placeholder = { Text("student") }
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            items.forEach { item ->
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
private fun ContactPrefDropdown(
    selected: String,
    onSelect: (String) -> Unit,
    containerColor: Color
) {
    var expanded by remember { mutableStateOf(false) }
    val items = listOf("push")

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        TextField(
            value = selected,
            onValueChange = {},
            readOnly = true,
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = containerColor,
                unfocusedContainerColor = containerColor,
                disabledContainerColor = containerColor,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            ),
            shape = RoundedCornerShape(10.dp),
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            placeholder = { Text("push") }
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            items.forEach { item ->
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
