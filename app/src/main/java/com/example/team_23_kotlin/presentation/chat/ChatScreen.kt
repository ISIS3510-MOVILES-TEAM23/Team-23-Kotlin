package com.example.team_23_kotlin.presentation.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.FileDownload
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.team_23_kotlin.utils.isNetworkAvailable
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    chatId: String,
    onBack: () -> Unit,
    onConfirmPurchase: () -> Unit = {}
) {
    val vm: ChatViewModel = hiltViewModel()
    val state by vm.state.collectAsState()
    val exportMessage by vm.exportMessage.collectAsState() // 👈 observamos el mensaje de exportación

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // 🔹 Cargar el chat al entrar
    LaunchedEffect(chatId) {
        vm.loadChat(chatId)
    }

    // 🔹 Mostrar Snackbar cuando cambie el mensaje de exportación
    LaunchedEffect(exportMessage) {
        exportMessage?.let { msg ->
            scope.launch { snackbarHostState.showSnackbar(msg) }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }, // 👈 habilitamos Snackbar
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
                            text = state.header.peerName.ifBlank { "Messages" },
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
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                state.isLoading -> {
                    CircularProgressIndicator(Modifier.align(Alignment.Center))
                }

                state.error != null -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Send,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "You're offline",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.Gray
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Connect to the internet to load your messages.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                }

                state.messages.isEmpty() -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Send,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "No messages yet",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Start chatting to see messages here.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(state.messages, key = { it.id }) { msg ->
                            MessageRow(msg = msg, peerAvatarUrl = state.header.peerAvatarUrl)
                        }
                    }
                }
            }
        }
    }
}


/* ---------- UI PIECES ---------- */

@Composable
private fun PurchaseConfirmationBar(
    onConfirmPurchase: () -> Unit,
    listingTitle: String
) {
    Surface(
        tonalElevation = 8.dp,
        color = MaterialTheme.colorScheme.primary,
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
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = listingTitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(Modifier.width(20.dp))

                Button(
                    onClick = onConfirmPurchase,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
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
                        text = "Confirma",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun MessageRow(msg: ChatMessage, peerAvatarUrl: String?) {
    val bubbleColor = if (msg.isMine) MaterialTheme.colorScheme.primary else Color(0xFFE0E0E0)
    val textColor = if (msg.isMine) MaterialTheme.colorScheme.onPrimary else Color.Black
    val alignToEnd = msg.isMine

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (alignToEnd) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        if (!msg.isMine) {
            Avatar(peerAvatarUrl)
            Spacer(Modifier.width(8.dp))
        }

        Column(horizontalAlignment = if (alignToEnd) Alignment.End else Alignment.Start) {
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
                Text(msg.text, style = MaterialTheme.typography.bodyMedium, color = textColor)
            }

            if (msg.isMine) {
                Text(
                    text = when (msg.deliveryStatus) {
                        MessageStatus.SENDING -> "🕓 Sending…"
                        MessageStatus.SENT -> "✔ Sent"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}


@Composable
private fun Avatar(avatarUrl: String?) {
    AsyncImage(
        model = avatarUrl ?: "https://picsum.photos/200",
        contentDescription = null,
        modifier = Modifier.size(28.dp).clip(CircleShape),
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
    Surface(
        tonalElevation = 12.dp,
        color = Color.Transparent,
        modifier = Modifier.imePadding().navigationBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { /* TODO attach file */ }) {
                Icon(Icons.Outlined.AttachFile, contentDescription = "Attach", tint = Color.Gray)
            }

            Spacer(modifier = Modifier.width(8.dp))

            TextField(
                value = value,
                onValueChange = onChange,
                modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                placeholder = { Text("Write a message", color = Color.Gray) },
                singleLine = true,
                shape = RoundedCornerShape(24.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFFF0F0F0),
                    unfocusedContainerColor = Color(0xFFF0F0F0),
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    cursorColor = MaterialTheme.colorScheme.primary
                )
            )

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(onClick = onSend, enabled = canSend) {
                Icon(
                    Icons.Outlined.Send,
                    contentDescription = "Send",
                    tint = if (canSend) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
