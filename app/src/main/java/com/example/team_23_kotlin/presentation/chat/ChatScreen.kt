package com.example.team_23_kotlin.presentation.chat

import android.R
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.team_23_kotlin.presentation.editprofile.EditProfileScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    chatId: String,
    onBack: () -> Unit,
    onConfirmPurchase: () -> Unit = {},
    vmFactory: (String) -> ChatViewModel = { ChatViewModel(it) }
) {
    val vm = remember(chatId) { vmFactory(chatId) }
    val state by vm.state.collectAsState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Messages",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        state.header.listingTitle?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.primary
                )
            )

        },
        bottomBar = {
            Column {
                // Botón de confirmar compra (aparece condicionalmente)
                if (state.showPurchaseButton) {
                    PurchaseConfirmationBar(
                        onConfirmPurchase = onConfirmPurchase,
                        listingTitle = state.header.listingTitle ?: "Item"
                    )
                }

                MessageInputBar(
                    value = state.input,
                    canSend = state.canSend,
                    onChange = { vm.onEvent(ChatEvent.OnMessageInputChange(it)) },
                    onSend = { vm.onEvent(ChatEvent.SendMessage(state.input)) }
                )
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(state.messages, key = { it.id }) { msg ->
                    MessageRow(
                        msg = msg,
                        peerAvatarUrl = state.header.peerAvatarUrl
                    )
                }
            }
        }
    }
}

/* ---------- UI pieces ---------- */

@Composable
private fun PurchaseConfirmationBar(
    onConfirmPurchase: () -> Unit,
    listingTitle: String
) {
    Surface(
        tonalElevation = 8.dp,
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "¿Listo para comprar?",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = listingTitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Button(
                    onClick = onConfirmPurchase,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Icon(
                        Icons.Default.ShoppingCart,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "Confirmar Compra",
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }
    }
}

@Composable
private fun MessageRow(msg: ChatMessage, peerAvatarUrl: String?) {
    val bubbleColor: Color
    val textColor: Color
    val alignToEnd = msg.isMine

    if (msg.isMine) {
        bubbleColor = MaterialTheme.colorScheme.primary
        textColor = MaterialTheme.colorScheme.onPrimary
    } else {
        bubbleColor = Color(0xFFE0E0E0)
        textColor = Color.Black
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (alignToEnd) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        if (!msg.isMine) {
            Avatar(avatarUrl = peerAvatarUrl)
            Spacer(Modifier.width(8.dp))
        }

        Column(
            horizontalAlignment = if (alignToEnd) Alignment.End else Alignment.Start
        ) {
            Text(
                text = if (msg.isMine) "You" else msg.senderName,
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray
            )
            Box(
                modifier = Modifier
                    .widthIn(min = 48.dp, max = 260.dp)
                    .clip(
                        if (msg.isMine)
                            RoundedCornerShape(16.dp, 16.dp, 4.dp, 16.dp)
                        else
                            RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp)
                    )
                    .background(bubbleColor)
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Text(
                    text = msg.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = textColor
                )
            }
        }

        if (msg.isMine) {
            Spacer(Modifier.width(8.dp))
            Avatar(avatarUrl = null) // tu avatar; reemplaza si tienes URL
        }
    }
}

@Composable
private fun Avatar(avatarUrl: String?) {
    val size = 28.dp

    AsyncImage(
        model =  "https://picsum.photos/200",
        contentDescription = null,
        modifier = Modifier.size(size).clip(CircleShape),
        contentScale = ContentScale.Crop
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MessageInputBar(
    value: String,
    canSend: Boolean,
    onChange: (String) -> Unit,
    onSend: () -> Unit
) {
    Surface(tonalElevation = 12.dp, color = Color.Transparent,
        modifier = Modifier
            .imePadding()            // <- SOLO la barra se mueve con el teclado
            .navigationBarsPadding()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icono de adjuntar archivo
            IconButton(onClick = { /* TODO: Implementar lógica para adjuntar */ }) {
                Icon(
                    Icons.Outlined.AttachFile,
                    contentDescription = "Adjuntar archivo",
                    tint = Color.Gray // O el color que desees
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Caja del input
            TextField(
                value = value,
                onValueChange = onChange,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp),
                placeholder = {
                    Text(
                        text = "Write a message",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                },
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    color = Color.Black
                ),
                singleLine = true,
                shape = RoundedCornerShape(24.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFFF0F0F0),
                    unfocusedContainerColor = Color(0xFFF0F0F0),
                    disabledContainerColor = Color(0xFFF0F0F0),
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                    cursorColor = MaterialTheme.colorScheme.primary
                )
            )

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = onSend,
                enabled = canSend
            ) {
                Icon(
                    Icons.Outlined.Send,
                    contentDescription = "Send",
                    tint = if (canSend)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Preview
@Composable
fun ChatScreenPreview() {
    ChatScreen(onBack = {}, chatId = "1", onConfirmPurchase = {})
}