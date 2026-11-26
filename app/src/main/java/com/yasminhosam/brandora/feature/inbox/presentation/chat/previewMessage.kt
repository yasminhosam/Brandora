package com.yasminhosam.brandora.feature.inbox.presentation.chat

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.yasminhosam.brandora.R
import com.yasminhosam.brandora.core.ui.theme.BrandoraTheme
import com.yasminhosam.brandora.feature.inbox.domain.model.ChatMessage
import com.yasminhosam.brandora.feature.inbox.domain.model.Conversation
import com.yasminhosam.brandora.feature.inbox.domain.model.ConversationStatus
import com.yasminhosam.brandora.feature.inbox.domain.model.Customer
import com.yasminhosam.brandora.feature.inbox.domain.model.DeliveryStatus
import com.yasminhosam.brandora.feature.inbox.domain.model.MessageType
import com.yasminhosam.brandora.feature.inbox.domain.model.SenderType
import java.time.Instant

private fun previewMessage(
    id: String,
    sender: SenderType,
    content: String?,
    at: String,
    type: MessageType = MessageType.TEXT,
    status: DeliveryStatus = DeliveryStatus.DELIVERED
) = ChatMessage(id, sender, content, type, status, Instant.parse(at))

private fun previewConversation(
    name: String? = "Mona",
    aiEnabled: Boolean = true,
    handoffReason: String? = null
) = Conversation(
    id = "c1",
    customer = Customer(id = "u1", phone = "201000000001", name = name),
    status = ConversationStatus.OPEN,
    aiEnabled = aiEnabled,
    handoffReason = handoffReason,
    lastMessageAt = Instant.parse("2026-11-21T10:05:00Z"),
    lastMessage = null
)

// Newest first, exactly like the backend sends them.
private val previewMessages = listOf(
    previewMessage("6", SenderType.CUSTOMER, null, "2026-11-21T10:05:00Z", MessageType.IMAGE),
    previewMessage("5", SenderType.OWNER, "Yes, it is available in black and white.", "2026-11-21T10:03:00Z"),
    previewMessage("4", SenderType.AI, "Hello! The price is 450 EGP.", "2026-11-21T10:01:00Z"),
    previewMessage("3", SenderType.CUSTOMER, "How much is the leather bag?", "2026-11-21T10:00:00Z"),
    previewMessage(
        "2", SenderType.OWNER, "Welcome to our shop", "2026-11-20T18:30:00Z",
        status = DeliveryStatus.FAILED
    ),
    previewMessage("1", SenderType.CUSTOMER, "Hi", "2026-11-20T18:29:00Z")
)

@Preview(showBackground = true, name = "Conversation")
@Composable
private fun ChatScreenPreview() {
    BrandoraTheme(darkTheme = false) {
        ChatScreen(
            state = ChatUiState(
                conversation = previewConversation(),
                messages = previewMessages,
                isLoading = false
            ),
            onEvent = {},
            onBackClick = {}
        )
    }
}

@Preview(showBackground = true, name = "Dark")
@Composable
private fun ChatScreenDarkPreview() {
    BrandoraTheme(darkTheme = true) {
        ChatScreen(
            state = ChatUiState(
                conversation = previewConversation(),
                messages = previewMessages,
                isLoading = false
            ),
            onEvent = {},
            onBackClick = {}
        )
    }
}

@Preview(showBackground = true, name = "Unknown name + handoff + older")
@Composable
private fun ChatScreenHandoffPreview() {
    BrandoraTheme {
        ChatScreen(
            state = ChatUiState(
                conversation = previewConversation(
                    name = null,
                    aiEnabled = false,
                    handoffReason = "CUSTOMER_REQUESTED_HUMAN"
                ),
                messages = previewMessages,
                nextOffset = 20,
                isLoading = false,
                showCustomerSheet = true
            ),
            onEvent = {},
            onBackClick = {}
        )
    }
}

@Preview(showBackground = true, name = "Loading")
@Composable
private fun ChatScreenLoadingPreview() {
    BrandoraTheme {
        ChatScreen(state = ChatUiState(isLoading = true), onEvent = {}, onBackClick = {})
    }
}

@Preview(showBackground = true, name = "Error")
@Composable
private fun ChatScreenErrorPreview() {
    BrandoraTheme {
        ChatScreen(
            state = ChatUiState(isLoading = false, errorRes = R.string.error_network),
            onEvent = {},
            onBackClick = {}
        )
    }
}

@Preview(showBackground = true, name = "Empty")
@Composable
private fun ChatScreenEmptyPreview() {
    BrandoraTheme {
        ChatScreen(
            state = ChatUiState(
                conversation = previewConversation(),
                messages = emptyList(),
                isLoading = false
            ),
            onEvent = {},
            onBackClick = {}
        )
    }
}

@Preview(showBackground = true, locale = "ar", name = "Arabic RTL")
@Composable
private fun ChatScreenArabicPreview() {
    BrandoraTheme {
        ChatScreen(
            state = ChatUiState(
                conversation = previewConversation(name = "منى"),
                messages = previewMessages,
                isLoading = false
            ),
            onEvent = {},
            onBackClick = {}
        )
    }
}