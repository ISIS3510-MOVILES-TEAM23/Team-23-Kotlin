package com.example.team_23_kotlin.presentation.home

import HomeViewModel
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.team_23_kotlin.R
import com.example.team_23_kotlin.data.repository.LocationRepositoryImpl
import com.example.team_23_kotlin.domain.usecase.CheckInCampusUseCase
import com.example.team_23_kotlin.core.ui.NetworkImage
import com.example.team_23_kotlin.data.local.PostsCacheStorage
import com.example.team_23_kotlin.data.local.SharedPostsMemoryCache
import com.example.team_23_kotlin.data.posts.FirestorePostsRepository
import com.example.team_23_kotlin.core.network.hasInternetConnection
import com.example.team_23_kotlin.data.posts.PostEntity
import com.example.team_23_kotlin.data.search.FirestoreSearchEventsRepository
import com.example.team_23_kotlin.presentation.shared.rememberConnectivityStatus
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.ui.graphics.Color
import com.google.firebase.crashlytics.FirebaseCrashlytics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


// =====================
// Historial de búsqueda (SharedPreferences con CSV)
// =====================
private class SearchHistoryManager(context: Context) {
    private val prefs = context.getSharedPreferences("search_history", Context.MODE_PRIVATE)
    private val key = "queries_csv"

    fun getHistory(): List<String> {
        val csv = prefs.getString(key, "") ?: ""
        if (csv.isBlank()) return emptyList()
        return csv.split("|||").filter { it.isNotBlank() }
    }

    fun saveQuery(q: String, maxItems: Int = 5) {
        val query = q.trim()
        if (query.isBlank()) return
        val list = getHistory().toMutableList()
        list.remove(query)
        list.add(0, query)
        while (list.size > maxItems) {
            list.removeAt(list.lastIndex)
        }
        prefs.edit().putString(key, list.joinToString("|||")).apply()
    }

    fun clear() {
        prefs.edit().putString(key, "").apply()
    }
}

data class ProductItem(
    val id: String,
    val title: String,
    val price: String,
    val imageUrl: String?
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onGoToAuth: () -> Unit = {},
    onSearch: (String) -> Unit = {},
    onItemClick: (String) -> Unit = {},
    onCategoryClick: (String, String) -> Unit = { _, _ -> },
) {
    val context = LocalContext.current
    val postsRepo = remember(context.applicationContext) {
        FirestorePostsRepository(
            FirebaseFirestore.getInstance(),
            PostsCacheStorage(context.applicationContext),
            SharedPostsMemoryCache.instance,
            isOnline = { context.hasInternetConnection() }
        )
    }

    val postsVm: HomePostsViewModel = viewModel(factory = object : ViewModelProvider.Factory {
        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return HomePostsViewModel(postsRepo) as T
        }
    })
    val postsState by postsVm.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    val viewModel: HomeViewModel = viewModel(factory = object : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val repo = LocationRepositoryImpl(context)
            val useCase = CheckInCampusUseCase(repo)
            @Suppress("UNCHECKED_CAST")
            return HomeViewModel(useCase) as T
        }
    })

    val isConnected by rememberConnectivityStatus()
    var wasOffline by remember { mutableStateOf(false) }

    val isInCampus by viewModel.isInCampus.collectAsState()
    val popupState by viewModel.popupState.collectAsState()
    var showPopup by rememberSaveable { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.loadMostVisitedCategory()
    }

    LaunchedEffect(popupState.favoriteCategory) {
        if (popupState.favoriteCategory != null) {
            showPopup = true
        }
    }

    LaunchedEffect(isConnected) {
        if (isConnected) {
            if (wasOffline) {
                snackbarHostState.showSnackbar(
                    message = "Conexión restaurada. Buscando nuevas publicaciones..."
                )
                postsVm.refresh()
                wasOffline = false
            }
        } else {
            wasOffline = true
            snackbarHostState.showSnackbar(
                message = "Conexión perdida. Mostrando publicaciones guardadas."
            )
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted -> if (granted) viewModel.refreshCampusStatus() }
    )

    LaunchedEffect(Unit) {
        val hasPermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (hasPermission) viewModel.refreshCampusStatus()
        else permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    // =====================
    // Estado de búsqueda + historial local con dropdown
    // =====================
    var query by rememberSaveable { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<PostEntity>>(emptyList()) }
    var showHistory by remember { mutableStateOf(false) }
    val searchHistoryMgr = remember { SearchHistoryManager(context) }
    var recentSearches by remember { mutableStateOf(searchHistoryMgr.getHistory()) }

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        contentWindowInsets = WindowInsets(0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                scrollBehavior = scrollBehavior,
                title = {
                    Text(
                        "Mercandes",
                        color = MaterialTheme.colorScheme.onPrimary,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            platformStyle = PlatformTextStyle(includeFontPadding = false)
                        )
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    ) { inner ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .consumeWindowInsets(inner),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // =====================
            // 🔍 Search bar estable + dropdown
            // =====================
            item {
                val focusManager = LocalFocusManager.current

                ExposedDropdownMenuBox(
                    expanded = showHistory,
                    onExpandedChange = { expanded ->
                        if (expanded && (query.isNotBlank() || recentSearches.isNotEmpty())) {
                            showHistory = true
                        } else {
                            showHistory = false
                        }
                    }
                ) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { text ->
                            query = text
                            if (text.isBlank()) {
                                recentSearches = searchHistoryMgr.getHistory()
                                searchResults = emptyList()
                            } else {
                                showHistory = false
                                // 🔍 Búsqueda reactiva en tiempo real (con debounce)
                                scope.launch {
                                    kotlinx.coroutines.delay(250) // espera corta para evitar spam
                                    if (text == query) { // asegura que el texto no cambió mientras esperábamos
                                        try {
                                            withContext(Dispatchers.IO) {
                                                val results = postsRepo.searchPosts(text.trim(), limit = 10)
                                                withContext(Dispatchers.Main) {
                                                    searchResults = results
                                                }
                                            }
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                        }
                                    }
                                }
                            }
                        },
                        leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                        trailingIcon = {
                            TextButton(
                                onClick = {
                                    val q = query.trim()
                                    if (q.isNotEmpty()) {
                                        searchHistoryMgr.saveQuery(q)
                                        recentSearches = searchHistoryMgr.getHistory()
                                        showHistory = false
                                        focusManager.clearFocus()
                                        scope.launch {
                                            searchResults = postsRepo.searchPosts(q, limit = 5)
                                        }
                                        onSearch(q)
                                    }
                                }
                            ) { Text("Search") }
                        },
                        placeholder = { Text("Search products") },
                        singleLine = true,
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            focusedBorderColor = MaterialTheme.colorScheme.background,
                            unfocusedBorderColor = MaterialTheme.colorScheme.background
                        )
                    )

                    ExposedDropdownMenu(
                        expanded = showHistory,
                        onDismissRequest = { showHistory = false },
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF0F0F0))
                    ) {
                        if (recentSearches.isEmpty()) {
                            DropdownMenuItem(
                                text = { Text("No recent searches", color = Color.Gray) },
                                onClick = {}
                            )
                        } else {
                            recentSearches.forEach { item ->
                                DropdownMenuItem(
                                    leadingIcon = {
                                        Icon(Icons.Outlined.AccessTime, contentDescription = null, tint = Color.Gray)
                                    },
                                    text = { Text(item) },
                                    onClick = {
                                        query = item
                                        showHistory = false
                                        focusManager.clearFocus()
                                        scope.launch {
                                            searchResults = postsRepo.searchPosts(item, limit = 5)
                                        }
                                        onSearch(item)
                                    }
                                )
                            }
                            Divider()
                            DropdownMenuItem(
                                text = { Text("Clear history") },
                                onClick = {
                                    searchHistoryMgr.clear()
                                    recentSearches = emptyList()
                                }
                            )
                        }
                    }
                }
            }

            // 🔎 Resultados de búsqueda
            if (query.isNotBlank() && searchResults.isNotEmpty()) {
                items(searchResults.size) { index ->
                    val post = searchResults[index]
                    ListItem(
                        headlineContent = { Text(post.title) },
                        supportingContent = { Text("$${post.price}") },
                        leadingContent = {
                            if (post.images.isNotEmpty()) {
                                NetworkImage(
                                    url = post.images.first(),
                                    contentDescription = post.title,
                                    modifier = Modifier.size(40.dp)
                                )
                            }
                        },
                        modifier = Modifier.clickable {
                            // 🧠 Guardar la búsqueda al hacer clic en un resultado
                            val q = query.trim()
                            if (q.isNotEmpty()) {
                                searchHistoryMgr.saveQuery(q)
                                recentSearches = searchHistoryMgr.getHistory()
                            }

                            // Ejecutar acción del ítem
                            onItemClick(post.id)
                        }
                    )
                    Divider()
                }
            }

            // 🔹 Recomendaciones personalizadas
            item {
                Text(
                    "Highlighted Products for you!",
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.titleLarge,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Button(
                    onClick = {
                        FirebaseCrashlytics.getInstance().log("🔥 Crash test triggered from HomeScreen")
                        throw RuntimeException("Test crash from HomeScreen – verifying Crashlytics integration")
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text("Simulate Crash", color = MaterialTheme.colorScheme.onErrorContainer)
                }
            }

            val currentUser = FirebaseAuth.getInstance().currentUser
            android.util.Log.d("AUTH", "UID = ${currentUser?.uid}, Email = ${currentUser?.email}")

            item {
                val recsVm: RecommendationsViewModel = viewModel(factory = object : ViewModelProvider.Factory {
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        val searchRepo = FirestoreSearchEventsRepository(FirebaseFirestore.getInstance())
                        val userId = FirebaseAuth.getInstance().currentUser?.uid
                            ?: throw IllegalStateException("No user logged in")

                        @Suppress("UNCHECKED_CAST")
                        return RecommendationsViewModel(
                            postsRepo,
                            searchRepo,
                            userId
                        ) as T
                    }
                })
                val recs by recsVm.recs.collectAsState()

                if (recs.isNotEmpty()) {
                    RecsCarousel(
                        items = recs.map {
                            ProductItem(
                                id = it.id,
                                title = it.title,
                                price = "$${it.price}",
                                imageUrl = it.images.firstOrNull()
                            )
                        },
                        onClick = { id -> onItemClick(id) }
                    )
                } else {
                    Text(
                        "No personalized recommendations yet.",
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }
            }

            item {
                Text("New Posts", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }

            when {
                postsState.isLoading -> {
                    item {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                }
                postsState.error != null -> {
                    item {
                        Text("Error: ${postsState.error}", color = MaterialTheme.colorScheme.error)
                    }
                }
                else -> {
                    items(postsState.items.size) { index ->
                        val p = postsState.items[index]
                        PostCard(
                            id = p.id,
                            title = p.title,
                            description = p.description,
                            imageUrl = p.imageUrl,
                            onClick = onItemClick,
                            modifier = Modifier
                        )
                    }
                }
            }
        }
    }

    if (showPopup && popupState.favoriteCategory != null) {
        val favoriteCategoryName = popupState.favoriteCategory!!.replaceFirstChar { it.uppercase() }

        val categories = listOf(
            Triple("Furniture", "c2", R.drawable.furniture),
            Triple("Bikes", "c3", R.drawable.bikes),
            Triple("Books", "c1", R.drawable.books),
            Triple("Electronics", "c4", R.drawable.electronics),
            Triple("Clothes", "c5", R.drawable.clothes),
            Triple("Tickets", "c6", R.drawable.electronics),
            Triple("University Club", "c7", R.drawable.ic_uni)
        )

        val matchedCategory = categories.firstOrNull {
            it.first.equals(favoriteCategoryName, ignoreCase = true)
        }

        AlertDialog(
            onDismissRequest = { showPopup = false },
            icon = {
                Icon(
                    imageVector = Icons.Outlined.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = {
                Text(
                    text = "👋 ¡Hola de nuevo!",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Parece que te encantan los productos de la categoría ")
                    Text(favoriteCategoryName, style = MaterialTheme.typography.titleMedium)
                    Text(
                        "¡Tenemos nuevas publicaciones que podrían gustarte! ¿Quieres verlas ahora?",
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showPopup = false
                        matchedCategory?.let { (title, id, _) ->
                            onCategoryClick(id, title)
                        }
                    }
                ) { Text("Ver publicaciones") }
            },
            dismissButton = { TextButton(onClick = { showPopup = false }) { Text("Cerrar") } },
            shape = RoundedCornerShape(20.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 4.dp
        )
    }
}

@Composable
private fun RecsCarousel(
    items: List<ProductItem>,
    onClick: (String) -> Unit = {}
) {
    val config = LocalConfiguration.current
    val screenWidth = config.screenWidthDp.dp
    val peek = 24.dp
    val pageWidth = screenWidth - (peek * 2)
    val pagerState = rememberPagerState(pageCount = { items.size })

    HorizontalPager(
        state = pagerState,
        pageSize = PageSize.Fixed(pageWidth),
        pageSpacing = 12.dp,
        contentPadding = PaddingValues(horizontal = peek)
    ) { page ->
        val item = items[page]
        SampleCard(
            id = item.id,
            title = item.title,
            price = item.price,
            imageUrl = item.imageUrl,
            onClick = onClick,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun SampleCard(
    modifier: Modifier = Modifier,
    id: String,
    title: String,
    price: String,
    imageUrl: String?,
    onClick: (String) -> Unit = {}
) {
    ElevatedCard(
        onClick = { onClick(id) },
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            NetworkImage(
                url = imageUrl,
                contentDescription = title,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(16.dp)),
                contentScale = ContentScale.Crop,
            )
            Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.ExtraBold)
            Text(price, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun PostCard(
    id: String,
    title: String,
    description: String,
    imageUrl: String?,
    modifier: Modifier,
    onClick: (String) -> Unit = {}
) {
    val config = LocalConfiguration.current
    val screenWidth = config.screenWidthDp.dp
    val imageWidth = (screenWidth * 0.4f).coerceAtMost(200.dp)

    ElevatedCard(
        onClick = { onClick(id) },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text("New", style = MaterialTheme.typography.labelSmall)
                Text(title, maxLines = 2, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Bold)
                Text(description, style = MaterialTheme.typography.bodyMedium, maxLines = 3, overflow = TextOverflow.Ellipsis)
            }

            NetworkImage(
                url = imageUrl,
                contentDescription = title,
                modifier = Modifier
                    .width(imageWidth)
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop,
            )
        }
    }
}
