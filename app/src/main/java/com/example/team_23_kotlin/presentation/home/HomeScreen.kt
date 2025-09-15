package com.example.team_23_kotlin.presentation.home


import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier 
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.lazy.LazyColumn


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onGoToAuth: () -> Unit = {},
    onSearch: (String) -> Unit = {},
    onItemClick: (String) -> Unit = {}
) {
    var query by rememberSaveable { mutableStateOf("") }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Mercandes") }
            )
        }
    ) { inner ->
        LazyColumn(
            contentPadding = PaddingValues(
                top = inner.calculateTopPadding() + 16.dp,
                bottom = inner.calculateBottomPadding() + 16.dp,
                start = 16.dp,
                end = 16.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Search
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
                        focusedBorderColor = MaterialTheme.colorScheme.outline,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )
            }

            item { Text("Highlighted Products for you!", style = MaterialTheme.typography.titleMedium) }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    SampleCard(title = "MathBook Baldor", price = "$50.000", onClick = onItemClick, modifier = Modifier.weight(1f))
                    SampleCard(title = "Harry Potter", price = "$100.000", onClick = onItemClick, modifier = Modifier.weight(1f))
                }
            }

            item { Text("New Posts", style = MaterialTheme.typography.titleMedium) }

            items(count = 5) { i ->
                Text("• Sample item #$i", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun SampleCard(
    modifier: Modifier = Modifier,
    title: String,
    price: String,
    onClick: (String) -> Unit = {},
) {
    ElevatedCard(
        onClick = { onClick(title) },
        shape = RoundedCornerShape(20.dp),
        modifier = modifier
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Placeholder de imagen
            Box(
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.2f)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )
            Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(price, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
        }
    }
}

