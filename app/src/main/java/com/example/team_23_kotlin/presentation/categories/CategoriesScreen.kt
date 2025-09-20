package com.example.team_23_kotlin.presentation.categories

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.team_23_kotlin.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriesScreen(
    onCategoryClick: (String) -> Unit = {}
) {
    val cs = MaterialTheme.colorScheme
    val ty = MaterialTheme.typography
    val hint = cs.onSurface.copy(alpha = 0.60f)

    val categories = listOf(
        "Furniture" to R.drawable.ic_furniture,
        "Bikes" to R.drawable.ic_bikes,
        "Books" to R.drawable.ic_books,
        "Electronics" to R.drawable.ic_electronics,
        "Clothes" to R.drawable.ic_clothes,
        "Tickets" to R.drawable.ic_electronics
    )

    Scaffold(
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
            contentPadding = PaddingValues(
                start = 16.dp, end = 16.dp, top = 12.dp, bottom = 16.dp
            )
        ) {
            item {
                var query by remember { mutableStateOf("") }
                val shape = RoundedCornerShape(14.dp)

                TextField(
                    value = query,
                    onValueChange = { query = it },
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
                    textStyle = ty.bodyMedium
                )

                Spacer(Modifier.height(20.dp))

                Text(
                    "Categories",
                    color = cs.onBackground,
                    style = ty.titleLarge.copy(fontWeight = FontWeight.ExtraBold)
                )

                Spacer(Modifier.height(12.dp))
            }

            items(categories) { (title, res) ->
                CategoryCard(
                    title = title,
                    iconRes = res,
                    onClick = { onCategoryClick(title) }
                )
            }

            item { Spacer(Modifier.height(8.dp)) }
        }
    }
}

@Composable
private fun CategoryCard(
    title: String,
    @DrawableRes iconRes: Int,
    onClick: () -> Unit = {}
) {
    val cs = MaterialTheme.colorScheme
    val ty = MaterialTheme.typography
    val shape = RoundedCornerShape(12.dp)
    val minHeight = 96.dp
    val iconSize = 40.dp
    val horizPad = 22.dp

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
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(minHeight),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = horizPad)
            ) {
                Image(
                    painter = painterResource(iconRes),
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
}

@Preview(showBackground = true)
@Composable
private fun CategoriesScreenPreview() {
    MaterialTheme { CategoriesScreen() }
}
