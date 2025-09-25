package com.example.team_23_kotlin.presentation.profile

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Public
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
import androidx.hilt.navigation.compose.hiltViewModel
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
                    .padding(25.dp),
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
                Spacer(modifier = Modifier.height(4.dp))

                LocationBadge(isInCampus.value == true)

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onGoToEdit,
                    modifier = Modifier
                        .fillMaxWidth(1f)
                        .height(40.dp),
                    shape = RoundedCornerShape(7.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE0E0E0))
                ) {
                    Text("Edit Profile", color = Color(0xFF333333), style = MaterialTheme.typography.titleSmall)
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onGoToEdit,
                    modifier = Modifier
                        .fillMaxWidth(1f)
                        .height(40.dp),
                    shape = RoundedCornerShape(7.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Text("Sales", color = Color.White, style = MaterialTheme.typography.titleSmall)
                }



                Spacer(modifier = Modifier.height(48.dp))

                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "My Products",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 16.dp),
                        color = Color(0xFF333333)
                    )

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 600.dp), // puedes ajustar esto si necesitas scroll interno
                        contentPadding = PaddingValues(vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(
                            listOf("Calculus Book", "Scientific Calculator", "Backpack")
                        ) { product ->
                            ProductCard(productName = product)
                        }
                    }

                }
            }
        }
    }
}

@Composable
fun ProductCard(productName: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight(),

        horizontalAlignment = Alignment.Start
    ) {
        // Imagen cuadrada
        AsyncImage(
            model = "https://picsum.photos/300/300",
            contentDescription = "Product Image",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f) // 👈 imagen cuadrada
                .clip(RoundedCornerShape(8.dp))
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Texto debajo
        Text(
            text = productName,
            style = MaterialTheme.typography.bodySmall,
            color = Color.Black,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
    }
}



@Composable
fun LocationBadge(isInCampus: Boolean) {
    val text = if (isInCampus) "On Campus" else "Outside Campus"
    val bgColor = if (isInCampus) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
    val contentColor = if (isInCampus) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error
    val icon = if (isInCampus) Icons.Filled.LocationOn else Icons.Filled.Public

    Row(
        modifier = Modifier
            .background(bgColor, shape = RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.titleSmall,
            color = contentColor
        )
    }
}


//@Preview
//@Composable
//fun ProfileScreenPreview() {
//    ProfileScreen(
//        onGoToEdit = {},
//        locationViewModel = hiltViewModel()
//    )
//}
