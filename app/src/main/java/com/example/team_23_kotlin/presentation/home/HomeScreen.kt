package com.example.team_23_kotlin.presentation.home


import androidx.compose.foundation.background
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier 
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CardDefaults
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.pager.PageSize
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.PlatformTextStyle
import kotlin.math.roundToInt

data class ProductItem(val title: String, val price: String, val imageUrl: String)

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
            title = item.title,
            price = item.price,
            imageUrl = item.imageUrl,
            onClick = onClick,
            modifier = Modifier
                .fillMaxWidth()
        )
    }
}


@Composable
private fun NetworkImage(
    url: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    val context = LocalContext.current
    AsyncImage(
        model = ImageRequest.Builder(context)
            .data(url)
            .crossfade(true)
            .build(),
        contentDescription = null,
        modifier = modifier,
        contentScale = contentScale
    )
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onGoToAuth: () -> Unit = {},
    onSearch: (String) -> Unit = {},
    onItemClick: (String) -> Unit = {}
) {
    var query by rememberSaveable { mutableStateOf("") }
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()


    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Mercandes",
                        color = MaterialTheme.colorScheme.onPrimary,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            platformStyle = PlatformTextStyle(includeFontPadding = false)
                        ),
                        modifier = Modifier
                            .fillMaxHeight()
                            .wrapContentHeight(Alignment.CenterVertically)
                            .offset(y = (-14).dp)
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
                .fillMaxSize(),
            contentPadding = PaddingValues(
                top = inner.calculateTopPadding() + 16.dp,
                bottom = inner.calculateBottomPadding() + 16.dp,
                start = 16.dp, end = 16.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it; onSearch(it) },
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                    placeholder = { Text("Search products") },
                    singleLine = true,
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedPlaceholderColor = Color(204,204,204),
                        unfocusedPlaceholderColor = Color(204,204,204),
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedBorderColor = MaterialTheme.colorScheme.background,
                        unfocusedBorderColor = MaterialTheme.colorScheme.background,
                        unfocusedTextColor = Color(115,115,115),
                    )
                )
            }


            item {
                Text(
                    "Highlighted Products for you!",
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.titleLarge,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center // si los quieres centrados
                )
            }

            item {
                val recs = listOf(
                    ProductItem("MathBook Baldor", "$50.000", "https://panamericana.vtexassets.com/arquivos/ids/482890/algebra-baldor-3-9786075502090.jpg?v=638125237314670000"),
                    ProductItem("Harry Potter", "$100.000", "https://sm.ign.com/ign_nordic/lists/h/harry-pott/harry-potter-books-in-order-a-chronological-reading-guide_r5mm.jpg"),
                    ProductItem("Calculus", "$80.000", "https://picsum.photos/seed/calc/600/600")
                )
                RecsCarousel(items = recs, onClick = onItemClick)
            }


            item {
                Text("New Posts", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }

            item {
                Row(modifier = Modifier.fillMaxWidth()) {
                    PostCard(
                        title = "Biology book",
                        description = "First-Year Biology book, Looks like new!",
                        imageUrl = "https://play-lh.googleusercontent.com/BNVoUFHLmyuDoun_E-WsG7j_ossatnT3Oa0ez1k1i7kkMjVzsy-LSJfQUTxcAfJqTDc",
                        onClick = onItemClick,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row {
                    PostCard(
                        title = "Desk Lamp",
                        description = "LED Desk Lamp, Looks like new!",
                        imageUrl = "https://m.media-amazon.com/images/I/61Ckk6bdzwL.jpg",
                        onClick = onItemClick,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row {
                    PostCard(
                        title = "Desk Lamp",
                        description = "LED Desk Lamp, Looks like new!",
                        imageUrl = "https://m.media-amazon.com/images/I/61Ckk6bdzwL.jpg",
                        onClick = onItemClick,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun SampleCard(
    modifier: Modifier = Modifier,
    title: String,
    price: String,
    imageUrl: String? = null,
    onClick: (String) -> Unit = {},
) {
    ElevatedCard(
        onClick = { onClick(title) },
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {

            NetworkImage(
                url = imageUrl,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(16.dp)),
                contentScale = ContentScale.Crop
            )

            Text(
                title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontWeight = FontWeight.ExtraBold,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(price, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun PostCard(
    modifier: Modifier = Modifier,
    title: String,
    description: String,
    imageUrl: String? = null,
    onClick: (String) -> Unit = {},
) {
    val config = LocalConfiguration.current
    val screenWidth = config.screenWidthDp.dp
    val imageWidth = (screenWidth * 0.4f).coerceAtMost(200.dp)

    ElevatedCard(
        onClick = { onClick(title) },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier
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
                Text(
                    text = "New",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Normal),
                )
                Text(
                    title,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    description,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Normal,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }

            NetworkImage(
                url = imageUrl,
                modifier = Modifier
                    .width(imageWidth)
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop
            )
        }
    }
}
