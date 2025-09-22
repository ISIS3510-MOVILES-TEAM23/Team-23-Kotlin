package com.example.team_23_kotlin.presentation.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.team_23_kotlin.presentation.shared.LocationViewModel

@Composable
fun ProfileScreen(
    onGoToEdit: () -> Unit,
    locationViewModel: LocationViewModel,
) {
    val isInCampus = locationViewModel.isInCampus.collectAsState()
    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .background(MaterialTheme.colorScheme.primary)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Profile",
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge
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
                ) {
                    AsyncImage(
                        model = "https://picsum.photos/200",
                        contentDescription = "Foto de perfil",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.matchParentSize()
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text("Sofia Ramirez", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(4.dp))
                Text("@sofia_ramirez", style = MaterialTheme.typography.labelMedium, color = Color(0xFF666666))
                Text("Math Student", style = MaterialTheme.typography.labelMedium, color = Color(0xFF666666))

                Text(
                    text = if (isInCampus.value == true) "📍 En el campus" else "📍 Fuera del campus",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isInCampus.value == true) Color.Green else MaterialTheme.colorScheme.error
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onGoToEdit,
                    modifier = Modifier
                        .fillMaxWidth(0.95f)
                        .height(40.dp),
                    shape = RoundedCornerShape(7.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE0E0E0))
                ) {
                    Text("Edit Profile", color = Color(0xFF333333), style = MaterialTheme.typography.titleSmall)
                }

                Spacer(modifier = Modifier.height(16.dp))

                Spacer(modifier = Modifier.height(32.dp))

                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "My Products",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(start = 16.dp, bottom = 16.dp),
                        color = Color(0xFF333333)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        ProductCard("Calculus Book")
                        ProductCard("Scientific Calculator")
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        ProductCard("Backpack")
                    }
                }
            }
        }
    }
}

@Composable
fun ProductCard(productName: String) {
    Column(
        horizontalAlignment = Alignment.Start,
        modifier = Modifier
            .width(150.dp)
            .padding(8.dp)
    ) {
        AsyncImage(
            model = "https://picsum.photos/300/300",
            contentDescription = "Product Image",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(150.dp)
                .clip(RoundedCornerShape(16.dp))
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(productName, style = MaterialTheme.typography.bodySmall, color = Color.Black)
    }
}

//@Preview
//@Composable
//fun ProfileScreenPreview() {
//    ProfileScreen(
//        onGoToEdit = {},
//        locationViewModel = LocationViewModel(null, null)
//    )
//}
