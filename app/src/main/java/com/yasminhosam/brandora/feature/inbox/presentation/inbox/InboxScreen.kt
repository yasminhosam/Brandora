package com.yasminhosam.brandora.feature.inbox.presentation.inbox

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.yasminhosam.brandora.R
import com.yasminhosam.brandora.core.ui.theme.BrandoraTheme
import com.yasminhosam.brandora.feature.inbox.domain.model.ChatMessage
import com.yasminhosam.brandora.feature.inbox.domain.model.Conversation
import com.yasminhosam.brandora.feature.inbox.domain.model.ConversationStatus
import com.yasminhosam.brandora.feature.inbox.domain.model.Customer
import com.yasminhosam.brandora.feature.inbox.domain.model.DeliveryStatus
import com.yasminhosam.brandora.feature.inbox.domain.model.MessageType
import com.yasminhosam.brandora.feature.inbox.domain.model.SenderType
import com.yasminhosam.brandora.feature.inbox.presentation.CustomerAvatar
import com.yasminhosam.brandora.feature.inbox.presentation.asText
import com.yasminhosam.brandora.feature.inbox.presentation.displayText
import com.yasminhosam.brandora.feature.inbox.presentation.displayTitle
import com.yasminhosam.brandora.feature.inbox.presentation.util.ChatTimeFormatter
import java.time.Instant
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InboxScreen(
    state: InboxUiState,
    onEvent: (InboxEvent) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(scheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .background(scheme.primary, RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
                .padding(horizontal = 8.dp, vertical = 16.dp)
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.chat_back),
                    tint = scheme.onPrimary
                )
            }
            Column(Modifier.padding(start = 4.dp)) {
                Text(
                    stringResource(R.string.inbox_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = scheme.onPrimary
                )
                Text(
                    stringResource(R.string.inbox_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onPrimary.copy(alpha = 0.85f)
                )
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            InboxFilter.entries.forEach { filter ->
                FilterChip(
                    selected = state.filter == filter,
                    onClick = { onEvent(InboxEvent.FilterSelected(filter)) },
                    label = { Text(stringResource(filter.labelRes())) }
                )
            }
        }

        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = { onEvent(InboxEvent.RefreshRequested) },
            modifier = Modifier.fillMaxSize()
        ) {
            when {
                state.isLoading -> CenteredBox { CircularProgressIndicator() }
                state.errorRes != null -> CenteredBox {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            stringResource(state.errorRes),
                            style = MaterialTheme.typography.bodyLarge,
                            color = scheme.error,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                        TextButton(onClick = { onEvent(InboxEvent.RetryClicked) }) {
                            Text(stringResource(R.string.action_retry))
                        }
                    }
                }
                state.isEmpty -> CenteredBox {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 32.dp)
                    ) {
                        Text(
                            stringResource(R.string.inbox_empty_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.size(8.dp))
                        Text(
                            stringResource(R.string.inbox_empty_body),
                            style = MaterialTheme.typography.bodyMedium,
                            color = scheme.onSurfaceVariant
                        )
                    }
                }
                else -> ConversationList(state, onEvent)
            }
        }
    }
}

@Composable
private fun ConversationList(state: InboxUiState, onEvent: (InboxEvent) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val locale = LocalConfiguration.current.locales[0]
    val now = Instant.now()
    val zone = ZoneId.systemDefault()

    LazyColumn(Modifier.fillMaxSize()) {
        items(state.conversations, key = { it.id }) { conversation ->
            ConversationRow(
                conversation = conversation,
                timeText = conversation.lastMessageAt?.let {
                    ChatTimeFormatter.inboxLabel(it, now, zone, locale).asText()
                },
                onClick = { onEvent(InboxEvent.ConversationClicked(conversation.id)) }
            )
            HorizontalDivider(
                color = scheme.outlineVariant,
                modifier = Modifier.padding(start = 80.dp)
            )
        }
        item {
            when {
                state.isLoadingMore -> CenteredRow { CircularProgressIndicator(Modifier.size(24.dp)) }
                state.loadMoreErrorRes != null -> CenteredRow {
                    TextButton(onClick = { onEvent(InboxEvent.LoadMoreClicked) }) {
                        Text(stringResource(state.loadMoreErrorRes))
                    }
                }
                state.hasMore -> CenteredRow {
                    TextButton(onClick = { onEvent(InboxEvent.LoadMoreClicked) }) {
                        Text(stringResource(R.string.inbox_load_more))
                    }
                }
            }
        }
    }
}

@Composable
private fun ConversationRow(
    conversation: Conversation,
    timeText: String?,
    onClick: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        CustomerAvatar(conversation.customer)
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    conversation.customer.displayTitle(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (timeText != null) {
                    Spacer(Modifier.width(8.dp))
                    Text(
                        timeText,
                        style = MaterialTheme.typography.labelSmall,
                        color = scheme.onSurfaceVariant
                    )
                }
            }
            Text(
                text = previewText(conversation),
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (conversation.needsOwnerReply) {
                Text(
                    stringResource(R.string.inbox_needs_reply),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = scheme.error
                )
            }
        }
    }
}

@Composable
private fun previewText(conversation: Conversation): String {
    val last = conversation.lastMessage ?: return stringResource(R.string.inbox_no_messages)
    val text = last.displayText()
    return when (last.sender) {
        SenderType.OWNER -> stringResource(R.string.inbox_preview_you, text)
        SenderType.AI -> stringResource(R.string.inbox_preview_ai, text)
        SenderType.CUSTOMER, SenderType.UNKNOWN -> text
    }
}

private fun InboxFilter.labelRes(): Int = when (this) {
    InboxFilter.OPEN -> R.string.inbox_filter_open
    InboxFilter.CLOSED -> R.string.inbox_filter_closed
    InboxFilter.ALL -> R.string.inbox_filter_all
}

@Composable
private fun CenteredBox(content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { content() }
}

@Composable
private fun CenteredRow(content: @Composable () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) { content() }
}

@Preview(name = "Inbox", showBackground = true)
@Composable
private fun InboxScreenPreview() {
    BrandoraTheme {
        InboxScreen(
            state = InboxUiState(
                conversations = listOf(
                    previewConversation(
                        id = "c1",
                        name = "Mona",
                        aiEnabled = false,
                        handoffReason = "OWNER_TOOK_OVER"
                    ),
                    previewConversation(id = "c2", name = null)
                ),
                isLoading = false
            ),
            onEvent = {},
            onBackClick = {}
        )
    }
}

/** Layout-only sample for the preview: the real rows come from the conversations endpoint. */
private fun previewConversation(
    id: String,
    name: String?,
    aiEnabled: Boolean = true,
    handoffReason: String? = null
) = Conversation(
    id = id,
    customer = Customer(id = "u-$id", phone = "20100000000${id.last()}", name = name),
    status = ConversationStatus.OPEN,
    aiEnabled = aiEnabled,
    handoffReason = handoffReason,
    lastMessageAt = Instant.parse("2026-11-21T10:05:00Z"),
    lastMessage = ChatMessage(
        id = "m-$id",
        sender = SenderType.CUSTOMER,
        content = "Is the leather bag available in black?",
        type = MessageType.TEXT,
        status = DeliveryStatus.RECEIVED,
        createdAt = Instant.parse("2026-11-21T10:05:00Z")
    )
)
