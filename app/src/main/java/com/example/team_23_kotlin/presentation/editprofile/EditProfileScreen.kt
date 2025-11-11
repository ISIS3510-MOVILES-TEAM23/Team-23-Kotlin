package com.example.team_23_kotlin.presentation.editprofile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    viewModel: EditProfileViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(end = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Edit Profile",
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.Black
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.Black
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = Color.Black
                )
            )
        }
        ,
        containerColor = Color.White
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
            ){
                AsyncImage(
                    model = "https://picsum.photos/200",
                    contentDescription = "Foto de fondo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize()
                )

            }

            Spacer(modifier = Modifier.height(12.dp))

            // Nombre & handle fijo
            Text(
                text = state.name.ifEmpty { "User" },
                style = MaterialTheme.typography.titleLarge,
                color = Color.Black
            )
            Text(
                text = "@${state.email.substringBefore("@")}",
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray
            )


            Spacer(modifier = Modifier.height(24.dp))

            // NAME FIELD
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Name",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Black,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                TextField(
                    value = state.name,
                    onValueChange = { viewModel.onEvent(EditProfileEvent.OnNameChanged(it)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFFF0F0F0),
                        unfocusedContainerColor = Color(0xFFF0F0F0),
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

// PHONE FIELD
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Phone",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Black,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                TextField(
                    value = state.phone ?: "",
                    onValueChange = { viewModel.onEvent(EditProfileEvent.OnPhoneChanged(it)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFFF0F0F0),
                        unfocusedContainerColor = Color(0xFFF0F0F0),
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Contact Preferences
            Text(
                text = "Contact Preferences",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Black,
                modifier = Modifier
                    .align(Alignment.Start)
                    .padding(bottom = 8.dp)
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                val selected = state.contactPreferences

                OutlinedButton(
                    onClick = {
                        val updated = if ("push notifications" in selected)
                            selected - "push notifications"
                        else
                            selected + "push notifications"
                        viewModel.onEvent(EditProfileEvent.OnContactPrefsChanged(updated))
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if ("push notifications" in selected)
                            MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f)
                        else Color.Transparent
                    )
                ) {
                    Text("Push notifications", color = Color.Black, fontSize = 13.sp)
                }

                OutlinedButton(
                    onClick = {
                        val updated = if ("email" in selected)
                            selected - "email"
                        else
                            selected + "email"
                        viewModel.onEvent(EditProfileEvent.OnContactPrefsChanged(updated))
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if ("email" in selected)
                            MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f)
                        else Color.Transparent
                    )
                ) {
                    Text("Email", color = Color.Black, fontSize = 13.sp)
                }
            }


            Spacer(modifier = Modifier.weight(1f))

            // Save button
            Button(
                onClick = { viewModel.onEvent(EditProfileEvent.OnSaveClicked) },
                enabled = !state.isSaving,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (state.isSaving)
                        Color.Gray.copy(alpha = 0.4f)
                    else
                        MaterialTheme.colorScheme.secondary
                )
            ) {
                Text("Save Changes", color = Color.White, style = MaterialTheme.typography.titleSmall)
            }

            when {
                state.isSaving -> {
                    CircularProgressIndicator(
                        modifier = Modifier.padding(top = 12.dp),
                        color = MaterialTheme.colorScheme.secondary
                    )
                }

                state.error != null -> {
                    Text(
                        text = state.error ?: "Something went wrong.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }

                state.success -> {
                    Text(
                        text = "Profile updated successfully!",
                        color = Color(0xFF2E7D32),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }
            }


        }

        }
    }

