package com.example.team_23_kotlin.presentation.categories

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddCircle
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.List
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.team_23_kotlin.R

// ---- Paleta rápida para el look del prototipo ----
private val Navy = Color(0xFF0D1626)
private val LightBackground = Color(0xFFF3F4F6)
private val CardBorder = Color(0xFFE6E6E9)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriesScreen(
    onGoToAuth: () -> Unit = {},
    onCategoryClick: (String) -> Unit = {}
) {
    val selectedTab = 1 // "Categories"

    // Fuente de datos (cambia los drawables por los tuyos si tienen otros nombres)
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
                        color = Color.White,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Navy
                )
            )
        }//,
        //bottomBar = {
        //    NavigationBar(containerColor = Color.White) {
        //        NavigationBarItem(
        //            selected = selectedTab == 0,
        //            onClick = { /* TODO */ },
        //            icon = { Icon(Icons.Outlined.Home, null) },
        //            label = { Text("Home") }
        //        )
        //        NavigationBarItem(
        //            selected = selectedTab == 1,
        //            onClick = { /* TODO */ },
        //            icon = { Icon(Icons.Outlined.List, null) },
        //            label = { Text("Categories") }
        //        )
        //        NavigationBarItem(
        //            selected = selectedTab == 2,
        //            onClick = { /* TODO */ },
        //            icon = { Icon(Icons.Outlined.AddCircle, null) }, // estable
        //            label = { Text("Post") }
        //        )
         //       NavigationBarItem(
          //          selected = selectedTab == 3,
         //           onClick = { /* TODO */ },
         //           icon = { Icon(Icons.Outlined.AddCircle, null) }, // estable
         //           label = { Text("Messages") }
         //       )
         //       NavigationBarItem(
         //           selected = selectedTab == 4,
         //           onClick = { /* TODO */ },
         //           icon = { Icon(Icons.Outlined.Person, null) },
         //           label = { Text("Profile") }
         //       )
         //   }
        //}
    ) { padding ->

        // ---- Contenido scrollable ----
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(padding),
            contentPadding = PaddingValues(
                start = 16.dp, end = 16.dp, top = 12.dp, bottom = 16.dp
            )
        ) {
            item {
                // Search bar
                var query by remember { mutableStateOf("") }
                TextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Search products") },
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = LightBackground,
                        unfocusedContainerColor = LightBackground,
                        disabledContainerColor = LightBackground,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor = Color.Black,
                        focusedTextColor = Color.Black,
                        unfocusedTextColor = Color.Black
                    )
                )
                Spacer(Modifier.height(20.dp))
                Text(
                    "Categories",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF0B0B0C)
                )
                Spacer(Modifier.height(12.dp))
            }

            // Lista de categorías (filtra si usas query)
            items(categories) { (title, res) ->
                CategoryCard(
                    title = title,
                    iconRes = res,
                    onClick = { onCategoryClick(title) }
                )
            }

            item { Spacer(Modifier.height(8.dp)) } // respiro final
        }
    }
}

@Composable
private fun CategoryCard(
    title: String,
    @DrawableRes iconRes: Int,
    onClick: () -> Unit = {},
) {
    val shape = RoundedCornerShape(12.dp)
    val minHeight = 96.dp
    val iconSize = 44.dp
    val horizPad = 22.dp

    Card(
        onClick = onClick,
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp)
            .heightIn(min = 96.dp)
            .shadow(6.dp, shape = shape, clip = false)
            .border(1.dp, CardBorder, shape)
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
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF101012),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = LocalTextStyle.current.copy(
                        platformStyle = PlatformTextStyle(includeFontPadding = false)
                    )
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun CategoriesScreenPreview() {
    CategoriesScreen()
}
