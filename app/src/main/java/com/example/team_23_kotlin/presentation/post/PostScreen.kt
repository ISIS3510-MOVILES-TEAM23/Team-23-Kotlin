package com.example.team_23_kotlin.presentation.post

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddCircle
import androidx.compose.material.icons.outlined.AttachMoney
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.ChatBubble
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.List
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.team_23_kotlin.R

private val Navy = Color(0xFF0D1626)
private val CardBorder = Color(0xFFE6E6E9)
private val Gold = Color(0xFFF1C550)
private val HintGray = Color(0xFF8E8E93)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostScreen(
    thumbs: List<Int> = listOf(R.drawable.ic_sample_ps5, R.drawable.ic_sample_ps5, R.drawable.ic_sample_ps5),
    onBack: () -> Unit = {},
    onAddPhotos: () -> Unit = {},
    onSubmit: (String, String, String) -> Unit = { _,_,_ -> }
) {
    var title by remember { mutableStateOf("Play Station 5") }
    var desc by remember { mutableStateOf("Play station 5 usada, con 2 controles y 5 juegos") }
    var price by remember { mutableStateOf("1’800.000") }

    val selectedTab = 2

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.Close, contentDescription = "Close", tint = Color.White)
                    }
                },
                title = { Text("Post", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Navy)
            )
        },
        bottomBar = {
            NavigationBar(containerColor = Color.White) {
                NavigationBarItem(selected = selectedTab == 0, onClick = {}, icon = { Icon(Icons.Outlined.Home, null) }, label = { Text("Home") })
                NavigationBarItem(selected = selectedTab == 1, onClick = {}, icon = { Icon(Icons.Outlined.List, null) }, label = { Text("Categories") })
                NavigationBarItem(selected = selectedTab == 2, onClick = {}, icon = { Icon(Icons.Outlined.AddCircle, null) }, label = { Text("Post") })
                NavigationBarItem(selected = selectedTab == 3, onClick = {}, icon = { Icon(Icons.Outlined.ChatBubble, null) }, label = { Text("Messages") })
                NavigationBarItem(selected = selectedTab == 4, onClick = {}, icon = { Icon(Icons.Outlined.Person, null) }, label = { Text("Profile") })
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Tile "Add more photos" (usa icono de cámara Material)
            AddPhotosTile(onClick = onAddPhotos)

            Spacer(Modifier.height(12.dp))

            // Thumbnails
            LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                items(thumbs.size) { i -> Thumb(imageRes = thumbs[i]) }
            }

            Spacer(Modifier.height(20.dp))

            FieldLabel("Title")
            RoundedTextField(
                value = title,
                onValueChange = { title = it },
                placeholder = "Play Station 5",
                trailing = {
                    Icon(Icons.Outlined.ContentCopy, contentDescription = "Copy", tint = HintGray)
                }
            )

            Spacer(Modifier.height(14.dp))

            FieldLabel("Description")
            RoundedTextField(
                value = desc,
                onValueChange = { desc = it },
                placeholder = "Describe your item…",
                trailing = { Icon(Icons.Outlined.Description, contentDescription = "Notes", tint = HintGray) },
                singleLine = false,
                minLines = 3,
                maxLines = 6
            )
            Text("Do not share contact details", color = HintGray, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp, start = 4.dp))

            Spacer(Modifier.height(14.dp))

            FieldLabel("Price")
            RoundedTextField(
                value = price,
                onValueChange = { price = formatPrice(it) },
                placeholder = "0",
                trailing = { Icon(Icons.Outlined.AttachMoney, contentDescription = "Price", tint = HintGray) },
                keyboardType = KeyboardType.Number
            )

            Spacer(Modifier.height(24.dp))

            // Botón Post
            Button(
                onClick = { onSubmit(title, desc, price) },
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Color.White),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp, pressedElevation = 0.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) { Text("Post", fontSize = 18.sp, fontWeight = FontWeight.Medium) }
        }
    }
}

/* ---------- Sub-composables ---------- */

@Composable
private fun FieldLabel(text: String) {
    Text(
        text = text,
        fontSize = 16.sp,
        fontWeight = FontWeight.SemiBold,
        color = Color(0xFF3A3A3C),
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
    visualTransformation: VisualTransformation = VisualTransformation.None
) {
    val shape = RoundedCornerShape(16.dp)
    TextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder, color = HintGray) },
        singleLine = singleLine,
        minLines = minLines,
        maxLines = maxLines,
        keyboardOptions = androidx.compose.ui.text.input.KeyboardOptions(
            keyboardType = keyboardType,
            imeAction = if (singleLine) ImeAction.Done else ImeAction.Default
        ),
        visualTransformation = visualTransformation,
        leadingIcon = leading,
        trailingIcon = trailing,
        shape = shape,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .border(1.dp, CardBorder, shape),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White,
            disabledContainerColor = Color.White,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
            cursorColor = Color.Black,
            focusedTextColor = Color.Black,
            unfocusedTextColor = Color.Black
        )
    )
}

@Composable
private fun AddPhotosTile(onClick: () -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .border(1.dp, CardBorder, shape)
            .shadow(6.dp, shape = shape, clip = false)
            .background(Color.White, shape)
            .clickable { onClick() }
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Add more\nphotos",
                fontSize = 28.sp,
                lineHeight = 30.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF6C6C70),
                style = LocalTextStyle.current.copy(
                    platformStyle = PlatformTextStyle(includeFontPadding = false)
                )
            )
            Icon(
                imageVector = Icons.Outlined.CameraAlt,
                contentDescription = "Camera",
                tint = Color(0xFF1C1C1E),
                modifier = Modifier.size(56.dp)
            )
        }
    }
}

@Composable
private fun Thumb(@DrawableRes imageRes: Int) {
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier = Modifier
            .size(width = 140.dp, height = 120.dp)
            .border(1.dp, CardBorder, shape)
            .shadow(3.dp, shape = shape, clip = false)
            .background(Color.White, shape)
            .padding(10.dp)
    ) {
        Image(
            painter = painterResource(imageRes),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
    }
}

/* ---------- Utilidad precio simple ---------- */
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

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun PostScreenPreview() {
    PostScreen()
}
