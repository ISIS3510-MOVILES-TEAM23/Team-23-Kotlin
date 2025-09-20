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
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.outlined.AttachMoney
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.team_23_kotlin.R
import androidx.compose.ui.draw.clip

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostScreen(
    thumbs: List<Int> = listOf(R.drawable.ic_playstation, R.drawable.ic_playstation, R.drawable.ic_playstation),
    onBack: () -> Unit = {},
    onAddPhotos: () -> Unit = {},
    onSubmit: (String, String, String) -> Unit = { _,_,_ -> }
) {
    val cs = MaterialTheme.colorScheme
    val ty = MaterialTheme.typography
    val hint = cs.onSurface.copy(alpha = 0.60f)
    val hairline = cs.onSurface.copy(alpha = 0.12f)

    var title by remember { mutableStateOf("Play Station 5") }
    var desc by remember { mutableStateOf("Play station 5 usada, con 2 controles y 5 juegos") }
    var price by remember { mutableStateOf("1’800.000") }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.Close, contentDescription = "Close", tint = cs.onPrimary)
                    }
                },
                title = {
                    Text(
                        "Post",
                        color = cs.onPrimary,
                        style = ty.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = cs.primary)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(cs.background)
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            AddPhotosTile(onClick = onAddPhotos, hairline = hairline, hint = hint)

            Spacer(Modifier.height(12.dp))

            LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                items(thumbs.size) { i -> Thumb(imageRes = thumbs[i]) }
            }

            Spacer(Modifier.height(20.dp))

            FieldLabel("Title")
            RoundedTextField(
                value = title,
                onValueChange = { title = it },
                placeholder = "Play Station 5",
                hairline = hairline,
                hint = hint
            )

            Spacer(Modifier.height(14.dp))

            FieldLabel("Description")
            RoundedTextField(
                value = desc,
                onValueChange = { desc = it },
                placeholder = "Describe your item…",
                trailing = { Icon(Icons.Outlined.Description, contentDescription = "Notes", tint = hint) },
                singleLine = false,
                minLines = 3,
                maxLines = 6,
                hairline = hairline,
                hint = hint
            )
            Text(
                "Do not share contact details",
                color = hint,
                style = ty.bodySmall,
                modifier = Modifier.padding(top = 6.dp, start = 4.dp)
            )

            Spacer(Modifier.height(14.dp))

            FieldLabel("Price")
            RoundedTextField(
                value = price,
                onValueChange = { price = formatPrice(it) },
                placeholder = "0",
                trailing = { Icon(Icons.Outlined.AttachMoney, contentDescription = "Price", tint = hint) },
                keyboardType = KeyboardType.Number,
                hairline = hairline,
                hint = hint
            )

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = { onSubmit(title, desc, price) },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = cs.secondary,
                    contentColor = cs.onSecondary
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp, pressedElevation = 0.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text("Post", style = ty.titleMedium.copy(fontWeight = FontWeight.Medium))
            }
        }
    }
}

/* ---------- Sub-composables ---------- */

@Composable
private fun FieldLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
        color = MaterialTheme.colorScheme.onSurface,
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
    visualTransformation: VisualTransformation = VisualTransformation.None,
    hairline: androidx.compose.ui.graphics.Color,
    hint: androidx.compose.ui.graphics.Color
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
        keyboardOptions = KeyboardOptions(
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
private fun AddPhotosTile(
    onClick: () -> Unit,
    hairline: androidx.compose.ui.graphics.Color,
    hint: androidx.compose.ui.graphics.Color
) {
    val cs = MaterialTheme.colorScheme
    val ty = MaterialTheme.typography
    val shape = RoundedCornerShape(18.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .border(1.dp, hairline, shape)
            .background(cs.surface, shape)
            .clickable { onClick() }
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Add more\nphotos",
                color = hint,
                style = ty.headlineSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    platformStyle = PlatformTextStyle(includeFontPadding = false)
                ),
                modifier = Modifier.padding(start = 2.dp)
            )
            Spacer(Modifier.weight(1f))
            Icon(
                imageVector = Icons.Filled.CameraAlt,
                contentDescription = "Camera",
                tint = cs.onSurface,
                modifier = Modifier.size(56.dp)
            )
        }
    }
}

@Composable
private fun Thumb(@DrawableRes imageRes: Int) {
    val shape = RoundedCornerShape(14.dp)
    val bg = MaterialTheme.colorScheme.surface
    val hairline = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)

    Box(
        modifier = Modifier
            .size(width = 140.dp, height = 120.dp)
            .border(1.dp, hairline, shape)
            .clip(shape)
            .background(bg)
    ) {
        Image(
            painter = painterResource(imageRes),
            contentDescription = null,
            modifier = Modifier.matchParentSize(),
            contentScale = ContentScale.Crop
        )
    }
}

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

@Preview(showBackground = true)
@Composable
private fun PostScreenPreview() {
    MaterialTheme { PostScreen() }
}
