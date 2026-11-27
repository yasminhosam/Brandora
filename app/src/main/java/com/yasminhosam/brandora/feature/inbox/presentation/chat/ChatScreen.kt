package com.yasminhosam.brandora.feature.inbox.presentation.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.yasminhosam.brandora.R
import com.yasminhosam.brandora.feature.inbox.domain.model.ChatMessage
import com.yasminhosam.brandora.feature.inbox.domain.model.Conversation
import com.yasminhosam.brandora.feature.inbox.domain.model.DeliveryStatus
import com.yasminhosam.brandora.feature.inbox.domain.model.MessageType
import com.yasminhosam.brandora.feature.inbox.domain.model.SenderType
import com.yasminhosam.brandora.feature.inbox.presentation.CustomerAvatar
import com.yasminhosam.brandora.feature.inbox.presentation.asText
import com.yasminhosam.brandora.feature.inbox.presentation.displayTitle
import com.yasminhosam.brandora.feature.inbox.presentation.util.ChatTimeFormatter
import java.time.Instant
import java.time.ZoneId

@Composable
fun ChatScreen(
    state: ChatUiState,
    onEvent: (ChatEvent) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    Column(
        // Edge-to-edge: safeDrawing keeps the header clear of the status bar and the
        // bottom bar clear of the navigation bar.
        modifier = modifier
            .fillMaxSize()
            .background(scheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        ChatHeader(state.conversation, onBackClick) { onEvent(ChatEvent.HeaderClicked) }

        if (state.conversation?.needsOwnerReply == true) {
            HandoffBanner()
        }

        Box(Modifier.weight(1f)) {
            when {
                state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                state.errorRes != null -> Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Text(
                        stringResource(state.errorRes),
                        color = scheme.error,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                    TextButton(onClick = { onEvent(ChatEvent.RetryClicked) }) {
                        Text(stringResource(R.string.action_retry))
                    }
                }
                else -> MessageList(state, onEvent)
            }
        }

        HorizontalDivider(color = scheme.outlineVariant)
        Composer(state, onEvent)
    }

    if (state.showCustomerSheet && state.conversation != null) {
        CustomerSheet(state, onEvent)
    }
}

@Composable
private fun ChatHeader(conversation: Conversation?, onBackClick: () -> Unit, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        color = scheme.primary,
        shadowElevation = 4.dp,
        shape = RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 14.dp)
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.chat_back),
                    tint = scheme.onPrimary
                )
            }
            if (conversation != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .weight(1f)
                        .clickable(onClick = onClick)
                ) {
                    CustomerAvatar(conversation.customer, size = 44.dp)
                    Column(Modifier.padding(start = 12.dp)) {
                        Text(
                            conversation.customer.displayTitle(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = scheme.onPrimary,
                            maxLines = 1
                        )
                        if (conversation.customer.name != null && conversation.customer.phone.isNotBlank()) {
                            Text(
                                conversation.customer.displayPhone,
                                style = MaterialTheme.typography.bodySmall,
                                color = scheme.onPrimary.copy(alpha = 0.85f)
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Soft warning strip shown when the AI handed the chat over to the owner. */
@Composable
private fun HandoffBanner() {
    val scheme = MaterialTheme.colorScheme
    Text(
        stringResource(R.string.chat_customer_handoff),
        style = MaterialTheme.typography.labelLarge,
        color = scheme.onErrorContainer,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .background(scheme.errorContainer, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    )
}

/**
 * The reply box. Send stays disabled while the draft is blank, over the code-point limit or a
 * send is in flight; failures are shown right above it so the text is never lost.
 */
@Composable
private fun Composer(state: ChatUiState, onEvent: (ChatEvent) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(scheme.surface)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        if (state.draftTooLong) {
            Text(
                text = stringResource(R.string.chat_reply_too_long),
                style = MaterialTheme.typography.bodySmall,
                color = scheme.error,
                modifier = Modifier.padding(bottom = 6.dp)
            )
        }
        state.sendErrorRes?.let { errorRes ->
            Text(
                text = stringResource(errorRes),
                style = MaterialTheme.typography.bodySmall,
                color = scheme.error,
                modifier = Modifier.padding(bottom = 6.dp)
            )
        }
        Row(verticalAlignment = Alignment.Bottom) {
            OutlinedTextField(
                value = state.draft,
                onValueChange = { onEvent(ChatEvent.ReplyChanged(it)) },
                modifier = Modifier.weight(1f),
                placeholder = { Text(stringResource(R.string.chat_reply_hint)) },
                maxLines = 4,
                isError = state.draftTooLong,
                shape = RoundedCornerShape(20.dp),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
            )
            Spacer(Modifier.size(8.dp))
            FilledIconButton(
                onClick = { onEvent(ChatEvent.SendClicked) },
                enabled = state.canSend,
                modifier = Modifier.size(52.dp)
            ) {
                if (state.isSending) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = scheme.onPrimary
                    )
                } else {
                    Icon(
                        Icons.AutoMirrored.Filled.Send,
                        contentDescription = stringResource(R.string.chat_send)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MessageList(state: ChatUiState, onEvent: (ChatEvent) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val locale = LocalConfiguration.current.locales[0]
    val zone = ZoneId.systemDefault()
    val now = Instant.now()

    PullToRefreshBox(
        isRefreshing = state.isRefreshing,
        onRefresh = { onEvent(ChatEvent.RefreshRequested) },
        modifier = Modifier.fillMaxSize()
    ) {
        if (state.isEmpty) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    stringResource(R.string.chat_empty),
                    color = scheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(24.dp)
                )
            }
            return@PullToRefreshBox
        }
        // Messages are newest-first, so reverseLayout puts index 0 at the bottom.
        LazyColumn(
            reverseLayout = true,
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp)
        ) {
            itemsIndexed(state.messages, key = { _, m -> m.id }) { index, message ->
                MessageBubble(
                    message = message,
                    locale = locale,
                    zone = zone,
                    onRetry = { onEvent(ChatEvent.RetrySendClicked(it)) }
                )
                // With reverseLayout the next index renders ABOVE this one, so a separator
                // after the item sits above the oldest message of a day.
                val older = state.messages.getOrNull(index + 1)
                val at = message.createdAt
                val startsDay = at != null && (
                        (older?.createdAt != null && !ChatTimeFormatter.isSameDay(at, older.createdAt, zone)) ||
                                (older == null && !state.hasOlder)
                        )
                if (startsDay && at != null) {
                    DaySeparator(ChatTimeFormatter.daySeparator(at, now, zone, locale).asText())
                }
            }
            item {
                when {
                    state.isLoadingOlder -> Box(
                        Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) { CircularProgressIndicator(Modifier.size(24.dp)) }
                    state.loadOlderErrorRes != null -> Box(Modifier.fillMaxWidth(), Alignment.Center) {
                        TextButton(onClick = { onEvent(ChatEvent.LoadOlderClicked) }) {
                            Text(stringResource(state.loadOlderErrorRes))
                        }
                    }
                    state.hasOlder -> Box(Modifier.fillMaxWidth(), Alignment.Center) {
                        TextButton(onClick = { onEvent(ChatEvent.LoadOlderClicked) }) {
                            Text(stringResource(R.string.chat_load_older))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DaySeparator(text: String) {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(50))
                .padding(horizontal = 14.dp, vertical = 5.dp)
        )
    }
}

@Composable
private fun MessageBubble(
    message: ChatMessage,
    locale: java.util.Locale,
    zone: ZoneId,
    onRetry: (String) -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val fromCustomer = message.sender == SenderType.CUSTOMER || message.sender == SenderType.UNKNOWN
    val container = when (message.sender) {
        SenderType.CUSTOMER, SenderType.UNKNOWN -> scheme.surface
        SenderType.OWNER -> scheme.primary
        SenderType.AI -> scheme.primaryContainer
    }
    val content = when (message.sender) {
        SenderType.CUSTOMER, SenderType.UNKNOWN -> scheme.onSurface
        SenderType.OWNER -> scheme.onPrimary
        SenderType.AI -> scheme.onPrimaryContainer
    }
    // The small corner sits on the sender's side, like a chat tail.
    val shape = if (fromCustomer) {
        RoundedCornerShape(topStart = 4.dp, topEnd = 18.dp, bottomEnd = 18.dp, bottomStart = 18.dp)
    } else {
        RoundedCornerShape(topStart = 18.dp, topEnd = 4.dp, bottomEnd = 18.dp, bottomStart = 18.dp)
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (fromCustomer) Arrangement.Start else Arrangement.End
    ) {
        Surface(
            color = container,
            shape = shape,
            shadowElevation = if (fromCustomer) 1.dp else 0.dp,
            modifier = Modifier.widthIn(max = 300.dp)
        ) {
            Column(Modifier.padding(horizontal = 14.dp, vertical = 9.dp)) {
                if (message.sender == SenderType.AI) {
                    Text(
                        stringResource(R.string.chat_sender_ai),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = scheme.primary
                    )
                    Spacer(Modifier.size(2.dp))
                }
                if (message.hasText) {
                    Text(
                        message.content.orEmpty(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = content
                    )
                } else {
                    Text(
                        stringResource(
                            if (message.type == MessageType.TEXT) R.string.message_kind_other
                            else R.string.chat_media_unavailable
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        fontStyle = FontStyle.Italic,
                        color = content.copy(alpha = 0.8f)
                    )
                }
                Spacer(Modifier.size(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    when {
                        message.status == DeliveryStatus.FAILED -> {
                            Text(
                                stringResource(R.string.chat_failed),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (fromCustomer) scheme.error else scheme.errorContainer
                            )
                            // A retry is a new message: the backend never resends a FAILED row.
                            if (message.sender == SenderType.OWNER && message.hasText) {
                                TextButton(
                                    onClick = { onRetry(message.content.orEmpty()) },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                                ) {
                                    Text(
                                        stringResource(R.string.chat_retry),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = scheme.error
                                    )
                                }
                            }
                            Spacer(Modifier.size(6.dp))
                        }
                        message.status == DeliveryStatus.PENDING && !fromCustomer -> {
                            Text(
                                stringResource(R.string.chat_pending),
                                style = MaterialTheme.typography.labelSmall,
                                color = content.copy(alpha = 0.7f)
                            )
                            Spacer(Modifier.size(6.dp))
                        }
                    }
                    message.createdAt?.let {
                        Text(
                            ChatTimeFormatter.clockTime(it, zone, locale),
                            style = MaterialTheme.typography.labelSmall,
                            color = content.copy(alpha = 0.65f)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomerSheet(state: ChatUiState, onEvent: (ChatEvent) -> Unit) {
    val conversation = state.conversation ?: return
    val customer = conversation.customer
    ModalBottomSheet(onDismissRequest = { onEvent(ChatEvent.SheetDismissed) }) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 8.dp)
        ) {
            CustomerAvatar(customer, size = 72.dp)
            Spacer(Modifier.size(12.dp))
            Text(
                customer.displayTitle(),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            if (customer.phone.isNotBlank()) {
                Text(
                    "${stringResource(R.string.chat_customer_phone)}: ${customer.displayPhone}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.size(16.dp))
            // The AI switch is a real PATCH: turning it off hands the chat to the owner.
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp))
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.chat_ai_toggle),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        stringResource(
                            if (conversation.aiEnabled) R.string.chat_customer_ai_on
                            else R.string.chat_customer_ai_off
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = conversation.aiEnabled,
                    onCheckedChange = { onEvent(ChatEvent.AiToggled(it)) },
                    enabled = !state.isTogglingAi
                )
            }
            state.aiErrorRes?.let { errorRes ->
                Text(
                    stringResource(errorRes),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp)
                )
            }
            if (conversation.needsOwnerReply) {
                Text(
                    stringResource(R.string.chat_customer_handoff),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                )
            }
            Spacer(Modifier.size(16.dp))
            androidx.compose.material3.Button(
                onClick = { onEvent(ChatEvent.OpenWhatsAppClicked) },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.chat_open_whatsapp))
            }
            Spacer(Modifier.size(16.dp))
        }
    }
}
