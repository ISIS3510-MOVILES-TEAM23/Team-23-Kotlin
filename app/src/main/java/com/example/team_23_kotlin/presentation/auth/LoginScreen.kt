// presentation/auth/LoginScreen.kt
package com.example.team_23_kotlin.presentation.auth

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

@Composable
fun LoginScreen(
    // ⬇️ CAMBIOS: en vez de onLoginSuccess vacío, pasamos correo/clave y un setter de loading
    onLogin: (email: String, password: String, setLoading: (Boolean) -> Unit) -> Unit,
    onGoToSignUp: () -> Unit,
    errorMessage: String? = null                         // ⬅️ opcional: para mostrar errores
) {
    // 🚫 Quita rememberNavController() aquí

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    val yellow = MaterialTheme.colorScheme.secondary
    val bg = MaterialTheme.colorScheme.background
    val black = Color(0xFF121212)
    val fieldBg = Color(0xFFF0F0F0)

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
                text = "Login",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.ExtraBold
                ),
                fontSize = 35.sp,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(Modifier.height(16.dp))

            // ===== Email =====
            Text(
                "Email",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 2.dp)
            )
            Spacer(Modifier.height(6.dp))
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
            Text(
                "Password",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 2.dp)
            )
            Spacer(Modifier.height(6.dp))
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

            // ⬇️ Mostrar error (opcional)
            if (!errorMessage.isNullOrBlank()) {
                Spacer(Modifier.height(10.dp))
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(Modifier.height(30.dp))

            Button(
                onClick = {
                    onLogin(email, password) { loading -> isLoading = loading }  // ⬅️ usa el VM
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
                        "Sign In",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.ExtraBold
                        ),
                        fontSize = 30.sp
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // ------- Or -------
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Divider(modifier = Modifier.weight(1f))
                Text(
                    "  Or  ",
                    style = MaterialTheme.typography.titleLarge,
                    fontSize = 30.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Divider(modifier = Modifier.weight(1f))
            }

            Spacer(Modifier.height(18.dp))

            Image(
                painter = painterResource(id = R.drawable.android_neutral_rd_si),
                contentDescription = "Continue with Google",
                modifier = Modifier
                    .height(55.dp)
                    .fillMaxWidth()
                    .align(Alignment.CenterHorizontally)
            )
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 5.dp),
        ) {
            Text(
                "Don’t have an account?  ",
                fontSize = 15.sp, textAlign = TextAlign.Center,
                fontFamily = Montserrat,
                fontWeight = FontWeight.Medium
            )
            Text(
                "Sign Up",
                color = Color(0,0,255),
                fontFamily = Montserrat,
                fontWeight = FontWeight.W600,
                fontSize = 15.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.clickable { onGoToSignUp() }
            )
        }
    }
}
