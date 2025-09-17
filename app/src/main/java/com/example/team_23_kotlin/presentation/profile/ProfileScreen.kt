package com.example.team_23_kotlin.presentation.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel = viewModel(),
    onGoToEdit: () -> Unit
) {
    Scaffold(
        //bottomBar = { ProfileBottomNavBar() }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFFF5F5F5))
        ) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .background(Color(0xFF2C3E50))
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Profile",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Icon(
                    imageVector = Icons.Filled.Settings,
                    contentDescription = "Settings",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .background(Color.Gray)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Sofia Ramirez",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF333333)
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(text = "@sofia_ramirez", fontSize = 16.sp, color = Color(0xFF666666))
                Text(text = "Math Student", fontSize = 16.sp, color = Color(0xFF666666))

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onGoToEdit, // 👈 Aquí cambió
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .height(50.dp),
                    shape = RoundedCornerShape(25.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE0E0E0))
                ) {
                    Text(text = "Edit Profile", color = Color(0xFF333333), fontSize = 18.sp)
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { viewModel.onEvent(ProfileEvent.OnSalesClick) },
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .height(50.dp),
                    shape = RoundedCornerShape(25.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF7B500))
                ) {
                    Text(text = "Sales", color = Color.White, fontSize = 18.sp)
                }

                Spacer(modifier = Modifier.height(32.dp))

                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "My Products",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 16.dp, bottom = 16.dp),
                        color = Color(0xFF333333)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        ProductCard(productName = "Calculus Book")
                        ProductCard(productName = "Scientific Calculator")
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        ProductCard(productName = "Backpack")
                    }
                }
            }
        }
    }
}

@Composable
fun ProductCard(productName: String) {
    Card(
        modifier = Modifier
            .width(150.dp)
            .height(180.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.LightGray)
            )

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = productName,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF333333)
            )
        }
    }
}
