package com.example.team_23_kotlin.presentation.categories

import android.content.Intent
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.team_23_kotlin.R
import com.example.team_23_kotlin.core.network.hasInternetConnection
import com.example.team_23_kotlin.core.ui.NetworkImage
import com.example.team_23_kotlin.data.local.SharedPostsMemoryCache
import com.example.team_23_kotlin.data.local.PostsCacheStorage
import com.example.team_23_kotlin.data.posts.FirestorePostsRepository
import com.example.team_23_kotlin.data.posts.PostEntity
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriesScreen(
    onCategoryClick: (String, String) -> Unit = { _, _ -> },
    onProductClick: (String) -> Unit = {},
    viewModel: CategoriesViewModel = androidx.hilt.navigation.compose.hiltViewModel()
) {
    val cs = MaterialTheme.colorScheme
    val ty = MaterialTheme.typography
    val hint = cs.onSurface.copy(alpha = 0.60f)

    val state by viewModel.state.collectAsState()

    val repo = remember { FirestorePostsRepository(FirebaseFirestore.getInstance()) }
    var searchResults by remember { mutableStateOf<List<PostEntity>>(emptyList()) }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Mercandes",
                        color = MaterialTheme.colorScheme.onPrimary,
                        style = ty.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            platformStyle = PlatformTextStyle(includeFontPadding = false)
                        )
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = cs.primary
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(cs.background)
                .padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 16.dp)
        ) {
            item {
                val shape = RoundedCornerShape(14.dp)
                TextField(
                    value = state.query,
                    onValueChange = {
                        viewModel.onEvent(CategoriesEvent.QueryChanged(it))
                        scope.launch {
                            searchResults = if (it.isBlank()) emptyList()
                            else repo.searchPosts(it, limit = 5)
                        }
                    },
                    placeholder = { Text("Search products", color = hint, style = ty.bodyMedium) },
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null, tint = hint) },
                    singleLine = true,
                    shape = shape,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
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
                    textStyle = ty.bodyMedium,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(
                        onSearch = { viewModel.onEvent(CategoriesEvent.SubmitSearch) }
                    )
                )

                Spacer(Modifier.height(20.dp))
            }

            // 🔎 resultados de búsqueda rápida
            if (state.query.isNotBlank() && searchResults.isNotEmpty()) {
                items(searchResults) { post ->
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
                            // ✅ Guardar evento en Firestore
                            val db = FirebaseFirestore.getInstance()
                            val clickData = hashMapOf(
                                "postId" to post.id,
                                "timestamp" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
                                "source" to "categories_screen"
                            )
                            db.collection("product_clicks").add(clickData)

                            // ✅ Navegar al detalle de producto
                            onProductClick(post.id)
                        }
                    )
                    HorizontalDivider()
                }
                item { Spacer(Modifier.height(20.dp)) }
            }

            // ⏳ estado de carga / error (para el fetch de categorías)
            if (state.isLoading && state.categories.isEmpty()) {
                item { LinearProgressIndicator(modifier = Modifier.fillMaxWidth()) }
            }
            if (state.error != null && state.categories.isEmpty()) {
                item {
                    Text("Error: ${state.error}", color = cs.error, style = ty.bodyMedium)
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = { viewModel.load(force = true) }) { Text("Reintentar") }
                    Spacer(Modifier.height(20.dp))
                }
            }

            // 📂 lista de categorías (desde Firestore + caché propia)
            if (state.categories.isNotEmpty()) {
                item {
                    Text(
                        "Categories",
                        color = cs.onBackground,
                        style = ty.titleLarge.copy(fontWeight = FontWeight.ExtraBold)
                    )
                    Spacer(Modifier.height(12.dp))
                }

                items(state.categories) { cat ->
                    CategoryCardLocalIcon(
                        title = cat.name,
                        iconName = cat.icon,
                        onClick = {
                            viewModel.onEvent(CategoriesEvent.CategoryClicked(cat.name))
                            onCategoryClick(cat.id, cat.name)
                        }
                    )
                }

                item { Spacer(Modifier.height(8.dp)) }
            }
        }
    }
}

/** Card para categoría con ícono en drawable local (resuelto por nombre). */
@Composable
private fun CategoryCardLocalIcon(
    title: String,
    iconName: String,
    onClick: () -> Unit = {}
) {
    val cs = MaterialTheme.colorScheme
    val ty = MaterialTheme.typography
    val shape = RoundedCornerShape(12.dp)
    val minHeight = 96.dp
    val iconSize = 40.dp
    val horizPad = 22.dp

    val context = LocalContext.current
    val resId = remember(iconName) {
        val id = context.resources.getIdentifier(iconName, "drawable", context.packageName)
        if (id == 0) android.R.drawable.ic_menu_report_image else id
    }

    Card(
        onClick = onClick,
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = cs.background),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp)
            .heightIn(min = minHeight)
            .shadow(10.dp, shape = shape, clip = false)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .height(minHeight)
                .padding(horizontal = horizPad)
        ) {
            Image(
                painter = painterResource(resId),
                contentDescription = null,
                modifier = Modifier.size(iconSize),
                contentScale = ContentScale.Fit
            )
            Spacer(Modifier.width(18.dp))
            Text(
                text = title,
                color = cs.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = ty.titleMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    platformStyle = PlatformTextStyle(includeFontPadding = false)
                )
            )
        }
    }
}
