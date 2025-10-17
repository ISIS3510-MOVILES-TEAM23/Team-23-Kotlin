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
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.team_23_kotlin.R
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostScreen(
    thumbs: List<Int> = emptyList(),
) {
    val focusManager = LocalFocusManager.current
    val vm: PostViewModel = viewModel()
    val s by vm.state.collectAsState()

    val cs = MaterialTheme.colorScheme
    val ty = MaterialTheme.typography
    val hint = cs.onSurface.copy(alpha = 0.60f)
    val hairline = cs.onSurface.copy(alpha = 0.12f)
    val snackbarHost = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val ctx = LocalContext.current

    // Cámara
    var pendingPhotoUri by remember { mutableStateOf<Uri?>(null) }
    val takePicture = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) pendingPhotoUri?.let { uri -> vm.onEvent(PostEvent.PhotoAdded(uri)) }
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
            scope.launch { snackbarHost.showSnackbar("Camera permission denied") }
        }
    }

    // Galería
    val pickImages = rememberLauncherForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (!uris.isNullOrEmpty()) uris.forEach { vm.onEvent(PostEvent.PhotoAdded(it)) }
    }

    // Sheet
    var showSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    fun openCamera() = requestCameraPermission.launch(Manifest.permission.CAMERA)
    fun openGallery() = pickImages.launch("image/*")

    // Toasts VM
    LaunchedEffect(s.errorMessage) { s.errorMessage?.let { scope.launch { snackbarHost.showSnackbar(it) } } }
    LaunchedEffect(s.postedOk) {
        if (s.postedOk) scope.launch { snackbarHost.showSnackbar("Post created!") }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text("Post", color = cs.onPrimary, style = ty.titleLarge.copy(fontWeight = FontWeight.Bold))
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
            AddPhotosTile(onClick = { showSheet = true }, hairline = hairline, hint = hint)

            Spacer(Modifier.height(12.dp))

            val minSlots = 3
            val placeholders = (minSlots - s.photoTokens.size).coerceAtLeast(0)

            LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                itemsIndexed(s.photoTokens) { index, token ->
                    if (token.startsWith("uri:")) {
                        ThumbRemote(
                            uri = Uri.parse(token.removePrefix("uri:")),
                            onRemove = { vm.onEvent(PostEvent.PhotoRemovedAt(index)) }
                        )
                    } else {
                        ThumbLocal(
                            imageRes = R.drawable.ic_playstation,
                            onRemove = { vm.onEvent(PostEvent.PhotoRemovedAt(index)) }
                        )
                    }
                }
                items(placeholders) {
                    PlaceholderThumb(onClick = { showSheet = true }, hint = hint, hairline = hairline)
                }
            }

            Spacer(Modifier.height(20.dp))

            FieldLabel("Title")
            RoundedTextField(
                value = s.title,
                onValueChange = { vm.onEvent(PostEvent.TitleChanged(it)) },
                placeholder = "Play Station 5",
                hairline = hairline,
                hint = hint,
                imeAction = ImeAction.Next
            )

            Spacer(Modifier.height(14.dp))

            FieldLabel("Category")
            CategoryDropdown(
                categories = s.categories,
                loading = s.categoriesLoading,
                error = s.categoriesError,
                selectedName = s.categoryName,
                onRetry = { vm.onEvent(PostEvent.ReloadCategories) },
                onSelect = { cat -> vm.onEvent(PostEvent.CategorySelected(cat.id, cat.name)) },
                hairline = hairline,
                hint = hint
            )

            Spacer(Modifier.height(14.dp))

            FieldLabel("Description")
            RoundedTextField(
                value = s.description,
                onValueChange = { vm.onEvent(PostEvent.DescriptionChanged(it)) },
                placeholder = "Describe your item…",
                trailing = { Icon(Icons.Outlined.Description, contentDescription = "Notes", tint = hint) },
                singleLine = false,
                minLines = 3,
                maxLines = 6,
                hairline = hairline,
                hint = hint,
                imeAction = ImeAction.Next
            )

            Text("Do not share contact details", color = hint, style = ty.bodySmall, modifier = Modifier.padding(top = 6.dp, start = 4.dp))

            Spacer(Modifier.height(14.dp))

            FieldLabel("Price")
            RoundedTextField(
                value = s.price,
                onValueChange = { vm.onEvent(PostEvent.PriceChanged(formatPrice(it))) },
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
                onClick = { vm.onEvent(PostEvent.SubmitClicked) },
                shape = RoundedCornerShape(12.dp),
                enabled = !s.isSaving,
                colors = ButtonDefaults.buttonColors(
                    containerColor = cs.secondary,
                    contentColor = cs.onSecondary,
                    disabledContainerColor = cs.secondary.copy(alpha = 0.5f),
                    disabledContentColor = cs.onSecondary.copy(alpha = 0.7f)
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp, pressedElevation = 0.dp),
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                if (s.isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp, color = cs.onSecondary)
                    Spacer(Modifier.width(10.dp))
                }
                Text("Post", style = ty.titleMedium.copy(fontWeight = FontWeight.Medium))
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
                    Icon(Icons.Filled.CameraAlt, contentDescription = null); Spacer(Modifier.width(8.dp)); Text("Use camera")
                }
                OutlinedButton(onClick = { showSheet = false; openGallery() }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                    Text("Choose from gallery")
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

/* ---------- Dropdown de categorías (lee del VM) ---------- */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryDropdown(
    categories: List<Category>,
    loading: Boolean,
    error: String?,
    selectedName: String?,
    onRetry: () -> Unit,
    onSelect: (Category) -> Unit,
    hairline: androidx.compose.ui.graphics.Color,
    hint: androidx.compose.ui.graphics.Color
) {
    val cs = MaterialTheme.colorScheme
    val ty = MaterialTheme.typography
    val shape = RoundedCornerShape(16.dp)

    var expanded by remember { mutableStateOf(false) }
    val labelText = selectedName ?: if (loading) "Loading…" else error ?: "Select a category"

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = {
        if (!loading) expanded = !expanded
        else expanded = false
    }) {
        TextField(
            value = labelText,
            onValueChange = {},
            readOnly = true,
            trailingIcon = {
                when {
                    loading -> CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    else -> ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                }
            },
            singleLine = true,
            shape = shape,
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .border(1.dp, hairline, shape),
            placeholder = { Text("Select a category", color = hint, style = ty.bodyMedium) },
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
                color = when {
                    selectedName == null && error == null && !loading -> hint
                    error != null -> cs.error
                    else -> cs.onSurface
                },
                platformStyle = PlatformTextStyle(includeFontPadding = false)
            )
        )

        ExposedDropdownMenu(expanded = expanded && error == null, onDismissRequest = { expanded = false }) {
            if (categories.isEmpty() && !loading) {
                DropdownMenuItem(
                    text = { Text("No categories") },
                    onClick = { expanded = false }
                )
                DropdownMenuItem(
                    text = { Text("Retry") },
                    onClick = { expanded = false; onRetry() }
                )
            } else {
                categories.forEach { cat ->
                    DropdownMenuItem(
                        text = { Text(cat.name) },
                        onClick = {
                            onSelect(cat)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

/* ---------- Helpers y Sub-composables (sin cambios visuales) ---------- */

private fun createTempImageUri(context: android.content.Context): Uri {
    val imagesDir = File(context.cacheDir, "images").apply { mkdirs() }
    val fileName = "capture_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(System.currentTimeMillis())}.jpg"
    val file = File(imagesDir, fileName)
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}

@Composable private fun FieldLabel(text: String) {
    Text(text = text, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
        color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(start = 4.dp, bottom = 8.dp))
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
    onIme: () -> Unit = {}                 // <-- quitar @Composable aquí
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
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
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


@Composable
private fun AddPhotosTile(onClick: () -> Unit, hairline: androidx.compose.ui.graphics.Color, hint: androidx.compose.ui.graphics.Color) {
    val cs = MaterialTheme.colorScheme; val ty = MaterialTheme.typography
    val shape = RoundedCornerShape(18.dp)
    Box(Modifier.fillMaxWidth().height(160.dp).border(1.dp, hairline, shape).background(cs.surface, shape).clickable { onClick() }.padding(20.dp)) {
        Row(Modifier.fillMaxSize().padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Add more\nphotos", color = hint, style = ty.headlineSmall.copy(
                fontWeight = FontWeight.SemiBold, platformStyle = PlatformTextStyle(includeFontPadding = false)))
            Spacer(Modifier.weight(1f))
            Icon(Icons.Filled.CameraAlt, contentDescription = "Camera", tint = cs.onSurface, modifier = Modifier.size(56.dp))
        }
    }
}

@Composable
private fun PlaceholderThumb(onClick: () -> Unit, hint: androidx.compose.ui.graphics.Color, hairline: androidx.compose.ui.graphics.Color) {
    val shape = RoundedCornerShape(14.dp); val bg = MaterialTheme.colorScheme.surface
    Box(Modifier.size(140.dp, 120.dp).border(1.dp, hairline, shape).clip(shape).background(bg).clickable { onClick() },
        contentAlignment = Alignment.Center) {
        Icon(Icons.Filled.CameraAlt, contentDescription = "Add photo", tint = hint, modifier = Modifier.size(36.dp))
    }
}

@Composable
private fun ThumbLocal(@DrawableRes imageRes: Int, onRemove: () -> Unit) {
    val shape = RoundedCornerShape(14.dp); val bg = MaterialTheme.colorScheme.surface
    val hairline = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
    val chipBg = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
    Box(Modifier.size(140.dp, 120.dp).border(1.dp, hairline, shape).clip(shape).background(bg)) {
        Image(painter = painterResource(imageRes), contentDescription = null, modifier = Modifier.matchParentSize(), contentScale = ContentScale.Crop)
        Box(Modifier.padding(6.dp).size(26.dp).align(Alignment.TopEnd).clip(CircleShape).background(chipBg).clickable { onRemove() },
            contentAlignment = Alignment.Center) { Icon(Icons.Outlined.Close, contentDescription = "Remove", tint = MaterialTheme.colorScheme.onSurface) }
    }
}

@Composable
private fun ThumbRemote(uri: Uri, onRemove: () -> Unit) {
    val shape = RoundedCornerShape(14.dp); val bg = MaterialTheme.colorScheme.surface
    val hairline = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
    val chipBg = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
    val context = LocalContext.current
    Box(Modifier.size(140.dp, 120.dp).border(1.dp, hairline, shape).clip(shape).background(bg)) {
        AsyncImage(model = ImageRequest.Builder(context).data(uri).crossfade(true).build(),
            contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.matchParentSize())
        Box(Modifier.padding(6.dp).size(26.dp).align(Alignment.TopEnd).clip(CircleShape).background(chipBg).clickable { onRemove() },
            contentAlignment = Alignment.Center) { Icon(Icons.Outlined.Close, contentDescription = "Remove", tint = MaterialTheme.colorScheme.onSurface) }
    }
}

/* ---------- Precio ---------- */
private fun formatPrice(input: String): String {
    val digits = input.filter { it.isDigit() }; if (digits.isEmpty()) return ""
    val rev = digits.reversed(); val out = StringBuilder()
    for (i in rev.indices) { if (i != 0 && i % 3 == 0) out.append('.'); out.append(rev[i]) }
    val s = out.reverse().toString(); return if (s.length > 3) s.replaceFirst(".", "’") else s
}
