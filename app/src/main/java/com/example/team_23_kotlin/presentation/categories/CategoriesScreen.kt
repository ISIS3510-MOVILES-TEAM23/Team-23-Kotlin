package com.example.team_23_kotlin.presentation.categories

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.team_23_kotlin.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriesScreen(
    onCategoryClick: (String) -> Unit = {},
    viewModel: CategoriesViewModel = androidx.hilt.navigation.compose.hiltViewModel()
) {
    val cs = MaterialTheme.colorScheme
    val ty = MaterialTheme.typography
    val hint = cs.onSurface.copy(alpha = 0.60f)

    val state by viewModel.state.collectAsState()

    val categories = listOf(
        Triple("Furniture", "c2", R.drawable.ic_furniture),
        Triple("Bikes", "c3", R.drawable.ic_bikes),
        Triple("Books", "c1", R.drawable.ic_books),
        Triple("Electronics", "c4", R.drawable.ic_electronics),
        Triple("Clothes", "c5", R.drawable.ic_clothes),
        Triple("Tickets", "c6", R.drawable.ic_electronics),
        Triple("University Club", "c7", R.drawable.ic_uni)
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
                        )
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary
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
                    onValueChange = { viewModel.onEvent(CategoriesEvent.QueryChanged(it)) },
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
                Text("Categories", color = cs.onBackground, style = ty.titleLarge.copy(fontWeight = FontWeight.ExtraBold))
                Spacer(Modifier.height(12.dp))
            }

            items(categories) { (title, id, res) ->
                CategoryCard(
                    title = title,
                    iconRes = res,
                    onClick = {
                        viewModel.onEvent(CategoriesEvent.CategoryClicked(title))
                        onCategoryClick(id)
                    }
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
