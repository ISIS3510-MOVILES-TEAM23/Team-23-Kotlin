package com.example.team_23_kotlin.presentation.home

import HomeViewModel
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import com.example.team_23_kotlin.data.repository.LocationRepositoryImpl
import com.example.team_23_kotlin.domain.usecase.CheckInCampusUseCase
import com.example.team_23_kotlin.core.ui.NetworkImage
import com.example.team_23_kotlin.data.posts.FirestorePostsRepository
import com.example.team_23_kotlin.data.posts.PostEntity
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

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
    onItemClick: (String) -> Unit = {}
) {
    val postsRepo = remember { FirestorePostsRepository(FirebaseFirestore.getInstance()) }
    val postsVm: HomePostsViewModel = viewModel(factory = object : ViewModelProvider.Factory {
        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return HomePostsViewModel(postsRepo) as T
        }
    })
    val postsState by postsVm.state.collectAsState()
    val context = LocalContext.current
    val viewModel: HomeViewModel = viewModel(factory = object : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val repo = LocationRepositoryImpl(context)
            val useCase = CheckInCampusUseCase(repo)
            @Suppress("UNCHECKED_CAST")
            return HomeViewModel(useCase) as T
        }
    })

    val isInCampus by viewModel.isInCampus.collectAsState()

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

    var query by rememberSaveable { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<PostEntity>>(emptyList()) }
    val scope = rememberCoroutineScope()

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        contentWindowInsets = WindowInsets(0),
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
            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = {
                        query = it
                        scope.launch {
                            searchResults = postsRepo.searchPosts(it, limit = 5)
                        }
                        onSearch(it)
                    },
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                    placeholder = { Text("Search products") },
                    singleLine = true,
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedBorderColor = MaterialTheme.colorScheme.background,
                        unfocusedBorderColor = MaterialTheme.colorScheme.background
                    )
                )
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
                        modifier = Modifier.clickable { onItemClick(post.id) }
                    )
                    Divider()
                }
            }

            item {
                Text(
                    "Highlighted Products for you!",
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.titleLarge,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }

            item {
                val recs = listOf(
                    ProductItem("p1","Calculus Book", "$50.000",
                        "https://www.wolfram-media.com/products/img/IntroToCalc-bookhomepage.png"),
                    ProductItem("p2","Lamp", "$30.000",
                        "https://m.media-amazon.com/images/I/61Ckk6bdzwL.jpg"),
                    ProductItem("p3","Headphones", "$120.000",
                        "https://picsum.photos/seed/calc/600/600")
                )
                RecsCarousel(items = recs, onClick = onItemClick)
            }

            item {
                Text("New Posts", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }

            when {
                postsState.isLoading -> {
                    item {
                        Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                }
                postsState.error != null -> {
                    item {
                        Text(
                            "Error: ${postsState.error}",
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
                else -> {
                    items(postsState.items.size) { index ->
                        val p = postsState.items[index]
                        Row(modifier = Modifier.fillMaxWidth()) {
                            PostCard(
                                id = p.id,
                                title = p.title,
                                description = p.description,
                                imageUrl = "",
                                onClick = onItemClick,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
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
