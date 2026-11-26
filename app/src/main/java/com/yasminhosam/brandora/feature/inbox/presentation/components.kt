package com.yasminhosam.brandora.feature.inbox.presentation

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.yasminhosam.brandora.R
import com.yasminhosam.brandora.feature.inbox.domain.model.ChatMessage
import com.yasminhosam.brandora.feature.inbox.domain.model.Customer
import com.yasminhosam.brandora.feature.inbox.domain.model.MessageType
import com.yasminhosam.brandora.feature.inbox.presentation.util.DayLabel

/** Circle with the customer's initial, or a person glyph when the name is unknown. */
@Composable
fun CustomerAvatar(customer: Customer, size: Dp = 48.dp, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(scheme.primaryContainer)
    ) {
        val initial = customer.initial
        if (initial != null) {
            Text(
                text = initial,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = scheme.onPrimaryContainer
            )
        } else {
            Icon(Icons.Filled.Person, contentDescription = null, tint = scheme.onPrimaryContainer)
        }
    }
}

@Composable
fun Customer.displayTitle(): String =
    title.ifBlank { stringResource(R.string.inbox_unknown_customer) }

/** Text of a message, or a localized kind label ("Photo", "Voice message") when it has none. */
@Composable
fun ChatMessage.displayText(): String {
    if (hasText) return content.orEmpty()
    return stringResource(
        when (type) {
            MessageType.IMAGE -> R.string.message_kind_image
            MessageType.AUDIO -> R.string.message_kind_audio
            MessageType.VIDEO -> R.string.message_kind_video
            MessageType.DOCUMENT -> R.string.message_kind_document
            MessageType.LOCATION -> R.string.message_kind_location
            MessageType.TEXT, MessageType.OTHER -> R.string.message_kind_other
        }
    )
}

@Composable
fun DayLabel.asText(): String = when (this) {
    is DayLabel.Time -> text
    DayLabel.Today -> stringResource(R.string.chat_today)
    DayLabel.Yesterday -> stringResource(R.string.inbox_yesterday)
    is DayLabel.Date -> text
}

/** Opens wa.me for the phone (digits only). Silently ignored when no app can handle it. */
fun openWhatsApp(context: Context, phone: String) {
    val digits = phone.filter { it.isDigit() }
    if (digits.isEmpty()) return
    val intent = android.content.Intent(
        android.content.Intent.ACTION_VIEW,
        android.net.Uri.parse("https://wa.me/$digits")
    )
    try {
        context.startActivity(intent)
    } catch (_: android.content.ActivityNotFoundException) {
        // No WhatsApp or browser installed: nothing sensible to do.
    }
}
