package com.example.team_23_kotlin.presentation.feedback

import android.Manifest
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedbackScreen(
    navController: NavController,
    viewModel: FeedbackViewModel = hiltViewModel()
) {
    val focusManager = LocalFocusManager.current
    val state by viewModel.state.collectAsState()
    val cs = MaterialTheme.colorScheme
    val ty = MaterialTheme.typography
    val hint = cs.onSurface.copy(alpha = 0.60f)
    val hairline = cs.onSurface.copy(alpha = 0.12f)
    val snackbarHost = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val ctx = LocalContext.current

    // Cámara
    var pendingPhotoUri by remember { mutableStateOf<Uri?>(null) }
    val takePicture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) pendingPhotoUri?.let { uri -> viewModel.onEvent(FeedbackEvent.PhotoAdded(uri)) }
        pendingPhotoUri = null
    }
    val requestCameraPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val uri = createTempImageUri(ctx)
            pendingPhotoUri = uri
            takePicture.launch(uri)
        } else {
            scope.launch { snackbarHost.showSnackbar("Permiso de cámara denegado") }
        }
    }

    // Galería
    val pickImages = rememberLauncherForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris -> if (!uris.isNullOrEmpty()) uris.forEach { viewModel.onEvent(FeedbackEvent.PhotoAdded(it)) } }

    var showSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    fun openCamera() = requestCameraPermission.launch(Manifest.permission.CAMERA)
    fun openGallery() = pickImages.launch("image/*")

    // Mostrar mensaje y navegar tras éxito
    LaunchedEffect(state.errorMessage, state.submitSuccess) {
        val msg = state.errorMessage ?: return@LaunchedEffect
        snackbarHost.showSnackbar(msg)
        if (state.submitSuccess) {
            kotlinx.coroutines.delay(800)
            navController.popBackStack()
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Text(
                        if (state.isEditMode) "Editar Feedback" else "Nuevo Feedback",
                        color = cs.onPrimary,
                        style = ty.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = cs.primary),
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Outlined.Close, contentDescription = "Cerrar", tint = cs.onPrimary)
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHost) }
    ) { padding ->
        // Loading state
        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(cs.background)
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
            // Rating Section
            FieldLabel("Calificación")
            RatingSelector(
                rating = state.rating,
                onRatingChange = { viewModel.onEvent(FeedbackEvent.RatingChanged(it)) }
            )

            Spacer(Modifier.height(20.dp))

            // Comment Section
            FieldLabel("Comentarios")
            RoundedTextField(
                value = state.comment,
                onValueChange = { viewModel.onEvent(FeedbackEvent.CommentChanged(it)) },
                placeholder = "Describe tu experiencia con esta compra…",
                singleLine = false,
                minLines = 4,
                maxLines = 8,
                hairline = hairline,
                hint = hint,
                imeAction = ImeAction.Default,
                onIme = { focusManager.clearFocus() }
            )

            Spacer(Modifier.height(20.dp))

            // Photos Section
            AddPhotosTile(onClick = { showSheet = true }, hairline = hairline, hint = hint)
            Spacer(Modifier.height(12.dp))

            val minSlots = 3
            val placeholders = (minSlots - state.photoTokens.size).coerceAtLeast(0)

            LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                itemsIndexed(state.photoTokens) { index, token ->
                    if (token.startsWith("uri:")) {
                        // Nueva foto desde URI local
                        ThumbRemote(uri = Uri.parse(token.removePrefix("uri:"))) {
                            viewModel.onEvent(FeedbackEvent.PhotoRemovedAt(index))
                        }
                    } else {
                        // Foto existente desde URL de Firebase
                        ThumbUrl(url = token) {
                            viewModel.onEvent(FeedbackEvent.PhotoRemovedAt(index))
                        }
                    }
                }
                items(placeholders) {
                    PlaceholderThumb(onClick = { showSheet = true }, hint = hint, hairline = hairline)
                }
            }

            Spacer(Modifier.height(24.dp))

            // Progress indicator
            if (state.isSaving) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (state.uploadProgress in 0f..0.99f) {
                        LinearProgressIndicator(
                            progress = { state.uploadProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(4.dp))
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Enviando feedback...",
                            style = ty.bodySmall,
                            color = cs.primary
                        )
                    }
                }
            }

            // Submit Button
            Button(
                onClick = { viewModel.onEvent(FeedbackEvent.SubmitClicked) },
                shape = RoundedCornerShape(12.dp),
                enabled = !state.isSaving,
                colors = ButtonDefaults.buttonColors(
                    containerColor = cs.secondary,
                    contentColor = cs.onSecondary,
                    disabledContainerColor = cs.secondary.copy(alpha = 0.5f),
                    disabledContentColor = cs.onSecondary.copy(alpha = 0.7f)
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp, pressedElevation = 0.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                if (state.isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp, color = cs.onSecondary)
                    Spacer(Modifier.width(10.dp))
                }
                Text(
                    if (state.isEditMode) "Actualizar Feedback" else "Enviar Feedback",
                    style = ty.titleMedium.copy(fontWeight = FontWeight.Medium)
                )
            }
        }
        }
    }

    if (showSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSheet = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(onClick = { showSheet = false; openCamera() }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                    Icon(Icons.Filled.CameraAlt, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Usar cámara")
                }
                OutlinedButton(onClick = { showSheet = false; openGallery() }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                    Text("Elegir de galería")
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun RatingSelector(rating: Int, onRatingChange: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 1..5) {
            IconButton(onClick = { onRatingChange(i) }) {
                Icon(
                    imageVector = if (i <= rating) Icons.Filled.Star else Icons.Filled.StarBorder,
                    contentDescription = "Star $i",
                    tint = if (i <= rating) Color(0xFFFFC107) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                    modifier = Modifier.size(48.dp)
                )
            }
        }
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RoundedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    singleLine: Boolean = true,
    minLines: Int = 1,
    maxLines: Int = 1,
    hairline: Color,
    hint: Color,
    imeAction: ImeAction = ImeAction.Done,
    onIme: () -> Unit = {}
) {
    val cs = MaterialTheme.colorScheme
    val ty = MaterialTheme.typography
    val shape = RoundedCornerShape(16.dp)

    TextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder, color = hint, style = ty.bodyMedium) },
        singleLine = singleLine,
        minLines = minLines,
        maxLines = maxLines,
        keyboardOptions = KeyboardOptions(imeAction = imeAction),
        keyboardActions = KeyboardActions(onDone = { onIme() }),
        shape = shape,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .border(1.dp, hairline, shape),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = cs.surface,
            unfocusedContainerColor = cs.surface,
            disabledContainerColor = cs.surface,
            focusedIndicatorColor = cs.surface,
            unfocusedIndicatorColor = cs.surface,
            disabledIndicatorColor = cs.surface,
            cursorColor = cs.onSurface,
            focusedTextColor = cs.onSurface,
            unfocusedTextColor = cs.onSurface
        ),
        textStyle = ty.bodyMedium.copy(
            color = cs.onSurface,
            platformStyle = PlatformTextStyle(includeFontPadding = false)
        )
    )
}

@Composable
private fun AddPhotosTile(onClick: () -> Unit, hairline: Color, hint: Color) {
    val cs = MaterialTheme.colorScheme
    val ty = MaterialTheme.typography
    val shape = RoundedCornerShape(18.dp)
    Box(
        Modifier
            .fillMaxWidth()
            .height(140.dp)
            .border(1.dp, hairline, shape)
            .background(cs.surface, shape)
            .clickable { onClick() }
            .padding(20.dp)
    ) {
        Row(Modifier.fillMaxSize().padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Agregar\nfotos",
                color = hint,
                style = ty.headlineSmall.copy(fontWeight = FontWeight.SemiBold, platformStyle = PlatformTextStyle(includeFontPadding = false))
            )
            Spacer(Modifier.weight(1f))
            Icon(Icons.Filled.CameraAlt, contentDescription = "Camera", tint = cs.onSurface, modifier = Modifier.size(56.dp))
        }
    }
}

@Composable
private fun PlaceholderThumb(onClick: () -> Unit, hint: Color, hairline: Color) {
    val shape = RoundedCornerShape(14.dp)
    val bg = MaterialTheme.colorScheme.surface
    Box(
        Modifier
            .size(140.dp, 120.dp)
            .border(1.dp, hairline, shape)
            .clip(shape)
            .background(bg)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Filled.CameraAlt, contentDescription = "Add photo", tint = hint, modifier = Modifier.size(36.dp))
    }
}

@Composable
private fun ThumbRemote(uri: Uri, onRemove: () -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    val bg = MaterialTheme.colorScheme.surface
    val hairline = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
    val chipBg = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
    val context = LocalContext.current
    Box(
        Modifier
            .size(140.dp, 120.dp)
            .border(1.dp, hairline, shape)
            .clip(shape)
            .background(bg)
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context).data(uri).crossfade(true).build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.matchParentSize()
        )
        Box(
            Modifier
                .padding(6.dp)
                .size(26.dp)
                .align(Alignment.TopEnd)
                .clip(CircleShape)
                .background(chipBg)
                .clickable { onRemove() },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Outlined.Close, contentDescription = "Remove", tint = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
private fun ThumbUrl(url: String, onRemove: () -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    val bg = MaterialTheme.colorScheme.surface
    val hairline = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
    val chipBg = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
    val context = LocalContext.current
    Box(
        Modifier
            .size(140.dp, 120.dp)
            .border(1.dp, hairline, shape)
            .clip(shape)
            .background(bg)
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context).data(url).crossfade(true).build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.matchParentSize()
        )
        Box(
            Modifier
                .padding(6.dp)
                .size(26.dp)
                .align(Alignment.TopEnd)
                .clip(CircleShape)
                .background(chipBg)
                .clickable { onRemove() },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Outlined.Close, contentDescription = "Remove", tint = MaterialTheme.colorScheme.onSurface)
        }
    }
}

private fun createTempImageUri(context: android.content.Context): Uri {
    val imagesDir = File(context.cacheDir, "images").apply { mkdirs() }
    val fileName = "capture_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(System.currentTimeMillis())}.jpg"
    val file = File(imagesDir, fileName)
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}
