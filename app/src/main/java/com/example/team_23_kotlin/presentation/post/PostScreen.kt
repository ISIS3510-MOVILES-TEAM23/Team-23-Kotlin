package com.example.team_23_kotlin.presentation.post

import android.Manifest
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
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
import androidx.compose.material.icons.outlined.AttachMoney
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.team_23_kotlin.R
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import androidx.core.content.FileProvider

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostScreen(
    thumbs: List<Int> = emptyList(),
    onBack: () -> Unit = {},
    onAddPhotos: () -> Unit = {},
    onSubmit: (String, String, String) -> Unit = { _,_,_ -> }
) {
    val cs = MaterialTheme.colorScheme
    val ty = MaterialTheme.typography
    val hint = cs.onSurface.copy(alpha = 0.60f)
    val hairline = cs.onSurface.copy(alpha = 0.12f)
    val focusManager = LocalFocusManager.current
    val snackbarHost = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val ctx = LocalContext.current

    // --- Estado de los campos: VACÍOS para que se vea el placeholder ---
    var title by rememberSaveable { mutableStateOf("") }
    var desc  by rememberSaveable { mutableStateOf("") }
    var price by rememberSaveable { mutableStateOf("") }

    // Lista de fotos (tokens: "res:<id>" o "uri:<...>")
    val thumbTokens = rememberSaveable {
        mutableStateListOf<String>().apply { addAll(thumbs.map { "res:$it" }) }
    }

    // Cámara
    var pendingPhotoUri by remember { mutableStateOf<Uri?>(null) }
    val takePicture = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) pendingPhotoUri?.let { uri -> thumbTokens.add("uri:$uri") } // APPEND (1-2-3…)
        pendingPhotoUri = null
    }

    // Permiso cámara
    val requestCameraPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val uri = createTempImageUri(ctx)
            pendingPhotoUri = uri
            takePicture.launch(uri)
        } else {
            scope.launch { snackbarHost.showSnackbar("Camera permission denied") }
        }
    }

    // Galería (múltiples)
    val pickImages = rememberLauncherForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (!uris.isNullOrEmpty()) thumbTokens.addAll(uris.map { "uri:$it" })
    }

    // Bottom sheet Camera/Gallery
    var showSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    fun openCamera() {
        onAddPhotos()
        requestCameraPermission.launch(Manifest.permission.CAMERA) // si ya está concedido, pasa directo
    }

    fun openGallery() {
        onAddPhotos()
        pickImages.launch("image/*")
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.Close, contentDescription = "Close", tint = cs.onPrimary)
                    }
                },
                title = {
                    Text(
                        "Post",
                        color = cs.onPrimary,
                        style = ty.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = cs.primary)
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHost) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(cs.background)
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Abre sheet para elegir Camera/Gallery
            AddPhotosTile(
                onClick = { showSheet = true },
                hairline = hairline,
                hint = hint
            )

            Spacer(Modifier.height(12.dp))

            // --- Thumbnails con mínimo 3 slots ---
            val minSlots = 3
            val placeholders = (minSlots - thumbTokens.size).coerceAtLeast(0)

            LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                itemsIndexed(thumbTokens) { index, token ->
                    if (token.startsWith("uri:")) {
                        ThumbRemote(
                            uri = Uri.parse(token.removePrefix("uri:")),
                            onRemove = { thumbTokens.removeAt(index) }
                        )
                    } else {
                        val resId = token.removePrefix("res:").toIntOrNull() ?: R.drawable.ic_playstation
                        ThumbLocal(
                            imageRes = resId,
                            onRemove = { thumbTokens.removeAt(index) }
                        )
                    }
                }
                items(placeholders) {
                    PlaceholderThumb(
                        onClick = { showSheet = true },
                        hint = hint,
                        hairline = hairline
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            FieldLabel("Title")
            RoundedTextField(
                value = title,
                onValueChange = { title = it },
                placeholder = "Play Station 5",
                hairline = hairline,
                hint = hint,
                imeAction = ImeAction.Next
            )

            Spacer(Modifier.height(14.dp))

            FieldLabel("Description")
            RoundedTextField(
                value = desc,
                onValueChange = { desc = it },
                placeholder = "Describe your item…",
                trailing = { Icon(Icons.Outlined.Description, contentDescription = "Notes", tint = hint) },
                singleLine = false,
                minLines = 3,
                maxLines = 6,
                hairline = hairline,
                hint = hint,
                imeAction = ImeAction.Next
            )

            Text(
                "Do not share contact details",
                color = hint,
                style = ty.bodySmall,
                modifier = Modifier.padding(top = 6.dp, start = 4.dp)
            )

            Spacer(Modifier.height(14.dp))

            FieldLabel("Price")
            RoundedTextField(
                value = price,
                onValueChange = { new ->
                    // Formateo al vuelo
                    val formatted = formatPrice(new)
                    price = formatted
                },
                placeholder = "0",
                trailing = { Icon(Icons.Outlined.AttachMoney, contentDescription = "Price", tint = hint) },
                keyboardType = KeyboardType.Number,
                hairline = hairline,
                hint = hint,
                imeAction = ImeAction.Done,
                onIme = { focusManager.clearFocus() }
            )

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = {
                    focusManager.clearFocus()
                    val cleanPrice = price.filter { it.isDigit() }
                    when {
                        title.isBlank() ->
                            scope.launch { snackbarHost.showSnackbar("Please enter a title.") }
                        cleanPrice.isBlank() || cleanPrice.toLongOrNull() == null || cleanPrice.toLong() <= 0L ->
                            scope.launch { snackbarHost.showSnackbar("Please enter a valid price.") }
                        else -> {
                            onSubmit(title.trim(), desc.trim(), price)
                            scope.launch { snackbarHost.showSnackbar("Posted!") }
                        }
                    }
                },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = cs.secondary,
                    contentColor = cs.onSecondary
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp, pressedElevation = 0.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text("Post", style = ty.titleMedium.copy(fontWeight = FontWeight.Medium))
            }
        }
    }

    // Bottom sheet Camera / Gallery
    if (showSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSheet = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = {
                        showSheet = false
                        openCamera()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Filled.CameraAlt, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Use camera")
                }
                OutlinedButton(
                    onClick = {
                        showSheet = false
                        openGallery()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Choose from gallery")
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

/* ---------- Helpers ---------- */

private fun createTempImageUri(context: android.content.Context): Uri {
    val imagesDir = File(context.cacheDir, "images").apply { mkdirs() }
    val fileName = "capture_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(System.currentTimeMillis())}.jpg"
    val file = File(imagesDir, fileName)
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}

/* ---------- Sub-composables ---------- */

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
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    singleLine: Boolean = true,
    minLines: Int = 1,
    maxLines: Int = 1,
    keyboardType: KeyboardType = KeyboardType.Text,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    hairline: androidx.compose.ui.graphics.Color,
    hint: androidx.compose.ui.graphics.Color,
    imeAction: ImeAction = ImeAction.Done,
    onIme: () -> Unit = {}
) {
    val cs = MaterialTheme.colorScheme
    val ty = MaterialTheme.typography
    val shape = RoundedCornerShape(16.dp)

    TextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder, color = hint, style = ty.bodyMedium) }, // <- igual que en Categories
        singleLine = singleLine,
        minLines = minLines,
        maxLines = maxLines,
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboardType,
            imeAction = imeAction
        ),
        keyboardActions = KeyboardActions(
            onDone = { onIme() },
            onNext = { onIme() },
            onPrevious = { onIme() },
            onGo = { onIme() },
            onSearch = { onIme() },
            onSend = { onIme() }
        ),
        visualTransformation = visualTransformation,
        leadingIcon = leading,
        trailingIcon = trailing,
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

/** Tile “Add more photos” */
@Composable
private fun AddPhotosTile(
    onClick: () -> Unit,
    hairline: androidx.compose.ui.graphics.Color,
    hint: androidx.compose.ui.graphics.Color
) {
    val cs = MaterialTheme.colorScheme
    val ty = MaterialTheme.typography
    val shape = RoundedCornerShape(18.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .border(1.dp, hairline, shape)
            .background(cs.surface, shape)
            .clickable { onClick() }
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Add more\nphotos",
                color = hint,
                style = ty.headlineSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    platformStyle = PlatformTextStyle(includeFontPadding = false)
                ),
                modifier = Modifier.padding(start = 2.dp)
            )
            Spacer(Modifier.weight(1f))
            Icon(
                imageVector = Icons.Filled.CameraAlt,
                contentDescription = "Camera",
                tint = cs.onSurface,
                modifier = Modifier.size(56.dp)
            )
        }
    }
}

/** Placeholder (o para completar a 3) */
@Composable
private fun PlaceholderThumb(
    onClick: () -> Unit,
    hint: androidx.compose.ui.graphics.Color,
    hairline: androidx.compose.ui.graphics.Color
) {
    val shape = RoundedCornerShape(14.dp)
    val bg = MaterialTheme.colorScheme.surface

    Box(
        modifier = Modifier
            .size(width = 140.dp, height = 120.dp)
            .border(1.dp, hairline, shape)
            .clip(shape)
            .background(bg)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Filled.CameraAlt,
            contentDescription = "Add photo",
            tint = hint,
            modifier = Modifier.size(36.dp)
        )
    }
}

/** Thumbnail local (drawable) con botón de borrar */
@Composable
private fun ThumbLocal(
    @DrawableRes imageRes: Int,
    onRemove: () -> Unit
) {
    val shape = RoundedCornerShape(14.dp)
    val bg = MaterialTheme.colorScheme.surface
    val hairline = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
    val chipBg = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)

    Box(
        modifier = Modifier
            .size(width = 140.dp, height = 120.dp)
            .border(1.dp, hairline, shape)
            .clip(shape)
            .background(bg)
    ) {
        Image(
            painter = painterResource(imageRes),
            contentDescription = null,
            modifier = Modifier.matchParentSize(),
            contentScale = ContentScale.Crop
        )
        Box(
            modifier = Modifier
                .padding(6.dp)
                .size(26.dp)
                .align(Alignment.TopEnd)
                .clip(CircleShape)
                .background(chipBg)
                .clickable { onRemove() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Close,
                contentDescription = "Remove",
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

/** Thumbnail remoto (URI) con botón de borrar */
@Composable
private fun ThumbRemote(
    uri: Uri,
    onRemove: () -> Unit
) {
    val shape = RoundedCornerShape(14.dp)
    val bg = MaterialTheme.colorScheme.surface
    val hairline = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
    val chipBg = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .size(width = 140.dp, height = 120.dp)
            .border(1.dp, hairline, shape)
            .clip(shape)
            .background(bg)
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(uri)
                .crossfade(true)
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.matchParentSize()
        )
        Box(
            modifier = Modifier
                .padding(6.dp)
                .size(26.dp)
                .align(Alignment.TopEnd)
                .clip(CircleShape)
                .background(chipBg)
                .clickable { onRemove() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Close,
                contentDescription = "Remove",
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

/* ---------- Precio ---------- */
private fun formatPrice(input: String): String {
    val digits = input.filter { it.isDigit() }
    if (digits.isEmpty()) return ""
    val rev = digits.reversed()
    val out = StringBuilder()
    for (i in rev.indices) {
        if (i != 0 && i % 3 == 0) out.append('.')
        out.append(rev[i])
    }
    val s = out.reverse().toString()
    return if (s.length > 3) s.replaceFirst(".", "’") else s
}
