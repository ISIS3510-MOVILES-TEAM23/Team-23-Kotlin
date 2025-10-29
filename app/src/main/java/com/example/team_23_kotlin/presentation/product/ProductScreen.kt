package com.example.team_23_kotlin.presentation.product

import android.content.Intent
import com.example.team_23_kotlin.R
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import com.example.team_23_kotlin.data.local.PostsCacheStorage
import com.example.team_23_kotlin.data.posts.FirestorePostsRepository
import com.example.team_23_kotlin.data.posts.PostsRepository
import com.example.team_23_kotlin.data.repository.AnalyticsRepositoryImpl
import com.example.team_23_kotlin.data.repository.LocationRepositoryImpl
import com.example.team_23_kotlin.domain.repository.AnalyticsRepository
import com.example.team_23_kotlin.domain.repository.LocationRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import android.net.Uri
import android.util.Log
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.ui.input.pointer.pointerInput
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Polyline
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONObject
import androidx.compose.foundation.gestures.detectTransformGestures
import com.google.maps.android.compose.*
import kotlinx.coroutines.*


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductScreen(
    productId: String,
    onBack: () -> Unit,
    nav: NavController
) {
    val context = LocalContext.current
    // Repositorios
    val repo: PostsRepository = remember(context.applicationContext) {
        FirestorePostsRepository(
            FirebaseFirestore.getInstance(),
            PostsCacheStorage(context.applicationContext)
        )
    }
    val analytics: AnalyticsRepository = remember {
        AnalyticsRepositoryImpl(FirebaseAuth.getInstance(), FirebaseFirestore.getInstance())
    }

    // ViewModel
    val viewModel: ProductViewModel = viewModel(factory = object : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return ProductViewModel(repo, analytics) as T
        }
    })

    // Cargar producto
    LaunchedEffect(productId) {
        viewModel.onEvent(ProductEvent.LoadProduct(productId))
    }

    val state by viewModel.state.collectAsState()

    // UI (igual que la que tú ya tienes)
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Product details",
                        color = MaterialTheme.colorScheme.onPrimary,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    ) { padding ->
        when {
            state.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            state.error != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Error: ${state.error}")
                }
            }

            else -> {
                state.product?.let { product ->
                    Column(
                        modifier = Modifier
                            .padding(padding)
                            .padding(20.dp)
                            .verticalScroll(rememberScrollState())
                    ) {

                        // 🖼️ Galería de imágenes
                        if (product.images.isNotEmpty()) {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(240.dp)
                            ) {
                                items(product.images) { imageUrl ->
                                    AsyncImage(
                                        model = imageUrl,
                                        contentDescription = null,
                                        modifier = Modifier
                                            .width(320.dp)
                                            .fillMaxHeight()
                                            .clip(RoundedCornerShape(16.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(240.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color.LightGray),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "No images available",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(Modifier.height(24.dp))

                        // 🔹 Título
                        Text(
                            text = product.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(Modifier.height(8.dp))

                        // 🔹 Descripción
                        Text(
                            text = product.description,
                            style = MaterialTheme.typography.bodyMedium
                        )

                        Spacer(Modifier.height(24.dp))

                        // 🔹 Precio
                        Text(
                            "Price",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "$${product.price}",
                            style = MaterialTheme.typography.bodyMedium
                        )

                        Spacer(Modifier.height(24.dp))

                        // 🔹 Vendedor
                        Text(
                            "Seller",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 8.dp)
                        ) {
                            Image(
                                painter = rememberAsyncImagePainter("https://randomuser.me/api/portraits/women/5.jpg"),
                                contentDescription = "Seller Avatar",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                            )
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = product.sellerName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.clickable {
                                        nav.navigate("seller/${product.sellerId}")
                                    }
                                )
                            }
                        }
                        Spacer(Modifier.height(24.dp))
                        // 🔹 Ver punto de recogida
                        var showMap by remember { mutableStateOf(false) }

                        if (showMap && state.product != null) {
                            PickupPointMapModal(
                                pickupName = state.product!!.pickupName,
                                pickupCoords = state.product!!.pickupCoords,
                                onDismiss = { showMap = false }
                            )
                        }

                        Button(
                            onClick = { showMap = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("Ver punto de recogida", style = MaterialTheme.typography.titleSmall)
                        }


                        Spacer(Modifier.height(32.dp))

                        Button(
                            onClick = {
                                val product = state.product ?: return@Button
                                viewModel.contactSeller(product.id) { chatId ->
                                    // 🔹 Navegar al chat recién creado o existente
                                    nav.navigate("chat/$chatId")
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                        ) {
                            Text("Contact", style = MaterialTheme.typography.titleSmall)
                        }



                        Spacer(Modifier.height(20.dp))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PickupPointMapModal(
    pickupName: String,
    pickupCoords: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val locationRepo: LocationRepository = remember { LocationRepositoryImpl(context) }
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )

    var currentLatLng by remember { mutableStateOf<LatLng?>(null) }
    var destinationLatLng by remember { mutableStateOf<LatLng?>(null) }
    var routePoints by remember { mutableStateOf<List<LatLng>>(emptyList()) }

    val cameraPositionState = rememberCameraPositionState()

    // === Parsear coordenadas del destino ===
    LaunchedEffect(pickupCoords) {
        try {
            val coords = pickupCoords.split(",")
            if (coords.size == 2) {
                val lat = coords[0].toDoubleOrNull()
                val lng = coords[1].toDoubleOrNull()
                if (lat != null && lng != null)
                    destinationLatLng = LatLng(lat, lng)
            }
        } catch (e: Exception) {
            Log.e("PickupMap", "Error parseando coordenadas: $pickupCoords", e)
        }
    }

    // === Obtener ubicación actual ===
    LaunchedEffect(Unit) {
        try {
            val loc = locationRepo.getCurrentLocation()
            loc?.let { currentLatLng = LatLng(it.latitude, it.longitude) }
        } catch (e: Exception) {
            Log.e("PickupMap", "Error obteniendo ubicación actual", e)
        }
    }

    // === Obtener ruta desde Directions API ===
    LaunchedEffect(currentLatLng, destinationLatLng) {
        if (currentLatLng != null && destinationLatLng != null) {
            try {
                val url = Uri.parse(
                    "https://maps.googleapis.com/maps/api/directions/json?" +
                            "origin=${currentLatLng!!.latitude},${currentLatLng!!.longitude}" +
                            "&destination=${destinationLatLng!!.latitude},${destinationLatLng!!.longitude}" +
                            "&mode=walking" +
                            "&key=${context.getString(R.string.google_maps_key)}"
                ).toString()

                val response = withContext(Dispatchers.IO) {
                    java.net.URL(url).readText()
                }
                val json = JSONObject(response)
                val routes = json.getJSONArray("routes")
                if (routes.length() > 0) {
                    val points = routes.getJSONObject(0)
                        .getJSONObject("overview_polyline")
                        .getString("points")
                    routePoints = decodePolyline(points)

                    val bounds = LatLngBounds.builder()
                        .include(currentLatLng!!)
                        .include(destinationLatLng!!)
                        .build()

                    delay(250)
                    cameraPositionState.animate(
                        CameraUpdateFactory.newLatLngBounds(bounds, 100)
                    )
                }
            } catch (e: Exception) {
                Log.e("PickupMap", "Error obteniendo la ruta", e)
            }
        }
    }

    // === Modal con mapa ===
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState=sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 10.dp,
        dragHandle = { Box(Modifier.height(8.dp)) }
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Punto de recogida: $pickupName",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(Modifier.height(10.dp))

            // --- Mapa más pequeño (300dp)
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.LightGray)
            ) {
                if (destinationLatLng != null) {
                    GoogleMap(
                        modifier = Modifier.matchParentSize(),
                        cameraPositionState = cameraPositionState,
                        uiSettings = MapUiSettings(
                            zoomControlsEnabled = false,
                            zoomGesturesEnabled = true,
                            scrollGesturesEnabled = true
                        ),
                        properties = MapProperties(isMyLocationEnabled = false)
                    ) {
                        // Marcadores
                        currentLatLng?.let {
                            Marker(state = MarkerState(it), title = "Tu ubicación")
                        }
                        destinationLatLng?.let {
                            Marker(state = MarkerState(it), title = pickupName)
                        }
                        // Ruta
                        if (routePoints.isNotEmpty()) {
                            Polyline(
                                points = routePoints,
                                color = Color(0xFF1976D2),
                                width = 8f
                            )
                        }
                    }

                    // Botón pequeño “centrar” arriba a la derecha
                    FloatingActionButton(
                        onClick = {
                            scope.launch {
                                val bounds = LatLngBounds.builder()
                                    .include(currentLatLng!!)
                                    .include(destinationLatLng!!)
                                    .build()
                                cameraPositionState.animate(
                                    CameraUpdateFactory.newLatLngBounds(bounds, 100)
                                )
                            }
                        },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .size(40.dp),
                        containerColor = MaterialTheme.colorScheme.primary
                    ) {
                        Icon(Icons.Default.MyLocation, contentDescription = "Centrar")
                    }
                } else {
                    Text(
                        "Cargando mapa...",
                        modifier = Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // --- Botón abrir en Google Maps
            OutlinedButton(
                onClick = {
                    destinationLatLng?.let { dest ->
                        val uri = Uri.parse(
                            "https://www.google.com/maps/dir/?api=1&destination=${dest.latitude},${dest.longitude}"
                        )
                        val intent = Intent(Intent.ACTION_VIEW, uri)
                        intent.setPackage("com.google.android.apps.maps")
                        context.startActivity(intent)
                    }
                },
                shape = RoundedCornerShape(10.dp),
                border = ButtonDefaults.outlinedButtonBorder.copy(width = 1.dp)
            ) {
                Text("Abrir en Google Maps")
            }

            Spacer(Modifier.height(10.dp))

            TextButton(onClick = onDismiss) {
                Text("Cerrar")
            }
        }
    }
}


/** 🔹 Decodifica la polyline del Directions API */
fun decodePolyline(encoded: String): List<LatLng> {
    val poly = ArrayList<LatLng>()
    var index = 0
    val len = encoded.length
    var lat = 0
    var lng = 0

    while (index < len) {
        var b: Int
        var shift = 0
        var result = 0
        do {
            b = encoded[index++].code - 63
            result = result or (b and 0x1f shl shift)
            shift += 5
        } while (b >= 0x20)
        val dlat = if ((result and 1) != 0) (result shr 1).inv() else result shr 1
        lat += dlat
        shift = 0
        result = 0
        do {
            b = encoded[index++].code - 63
            result = result or (b and 0x1f shl shift)
            shift += 5
        } while (b >= 0x20)
        val dlng = if ((result and 1) != 0) (result shr 1).inv() else result shr 1
        lng += dlng
        poly.add(LatLng(lat / 1E5, lng / 1E5))
    }
    return poly
}





