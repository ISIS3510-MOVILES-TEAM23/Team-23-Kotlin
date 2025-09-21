package com.example.team_23_kotlin.presentation.profile

import android.R
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.team_23_kotlin.presentation.categories.CategoriesScreen
import coil.compose.AsyncImage
import com.example.team_23_kotlin.data.repository.LocationRepositoryImpl
import com.example.team_23_kotlin.domain.usecase.CheckInCampusUseCase

@Composable
fun ProfileScreen(
    onGoToEdit: () -> Unit
) {
    val context = LocalContext.current
    // 1. Permiso de ubicación
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                Log.d("Permission", "✅ Permission granted")
            } else {
                Log.e("Permission", "❌ Permission denied")
            }
        }
    )

// Este `remember` evita que se lance más de una vez
    var askedPermission by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasPermission && !askedPermission) {
            askedPermission = true
            permissionLauncher.launch(android.Manifest.permission.ACCESS_FINE_LOCATION)
        } else {
            Log.d("Permission", "Already granted")
        }
    }

    val viewModel = viewModel(
        modelClass = ProfileViewModel::class.java,
        factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val repo = LocationRepositoryImpl(context)
                val useCase = CheckInCampusUseCase(repo)
                return ProfileViewModel(useCase) as T
            }
        }
    )

    Scaffold(
        //bottomBar = { ProfileBottomNavBar() }
    ) { paddingValues ->
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
                ){
                    AsyncImage(
                        model = "https://picsum.photos/200",
                        contentDescription = "Foto de fondo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.matchParentSize()
                    )

                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Sofia Ramirez",
                    style = MaterialTheme.typography.titleMedium,
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(text = "@sofia_ramirez", style = MaterialTheme.typography.labelMedium, color = Color(0xFF666666))
                Text(text = "Math Student", style = MaterialTheme.typography.labelMedium, color = Color(0xFF666666))
                if (viewModel.state.collectAsState().value.isInCampus) {
                    Text(
                        text = "📍 En el campus",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Green
                    )
                } else {
                    Text(
                        text = "📍 Fuera del campus",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }


                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onGoToEdit,
                    modifier = Modifier
                        .fillMaxWidth(0.95f)
                        .height(40.dp),
                    shape = RoundedCornerShape(7.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE0E0E0))
                ) {
                    Text(text = "Edit Profile", color = Color(0xFF333333), style = MaterialTheme.typography.titleSmall)
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { viewModel.onEvent(ProfileEvent.OnSalesClick) },
                    modifier = Modifier
                        .fillMaxWidth(0.95f)
                        .height(40.dp),
                    shape = RoundedCornerShape(7.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Text(text = "Sales", color = Color.White, style = MaterialTheme.typography.titleSmall)
                }

                Spacer(modifier = Modifier.height(32.dp))

                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "My Products",
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
                        ProductCard(productName = "Calculus Book")
                        ProductCard(productName = "Scientific Calculator")
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
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

        Text(
            text = productName,
            style = MaterialTheme.typography.bodySmall,
            color = Color.Black
        )
    }
}


//preview
@Preview
@Composable
fun ProfileScreenPreview() {
    ProfileScreen(onGoToEdit = {})
}
