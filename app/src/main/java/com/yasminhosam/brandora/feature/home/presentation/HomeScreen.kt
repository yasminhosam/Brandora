package com.yasminhosam.brandora.feature.home.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.yasminhosam.brandora.R

private data class FeatureCard(
    val titleRes: Int,
    val icon: ImageVector,
    val badge: String?,
    val onClick: () -> Unit
)

/**
 * Home: a grid of square cards, one per feature that is not a bottom-bar tab. New features
 * only add an entry to the list below.
 */
@Composable
fun HomeScreen(
    state: HomeUiState,
    onEvent: (HomeEvent) -> Unit,
    onChatsClick: () -> Unit,
    onImportClick: () -> Unit,
    onWorkingHoursClick:()-> Unit,
    modifier: Modifier = Modifier
) {
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { onEvent(HomeEvent.Refresh) }

    val scheme = MaterialTheme.colorScheme
    val chatBadge = state.openChatCount?.takeIf { it > 0 }
        ?.let { stringResource(R.string.home_card_chats_badge, it) }
    val cards = listOf(
        FeatureCard(R.string.home_card_chats, Icons.Filled.Email, chatBadge, onChatsClick),
        FeatureCard(R.string.home_card_import, Icons.Filled.Add, null, onImportClick),
        FeatureCard(R.string.home_card_working_hours, Icons.Filled.DateRange, null, onWorkingHoursClick)
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(scheme.background)
            .statusBarsPadding()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(24.dp))
        Text(
            stringResource(R.string.home_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = scheme.onBackground
        )
        Spacer(Modifier.height(4.dp))
        Text(
            stringResource(R.string.home_section_title),
            style = MaterialTheme.typography.bodyLarge,
            color = scheme.onSurfaceVariant
        )
        Spacer(Modifier.height(16.dp))
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(cards, key = { it.titleRes }) { card -> FeatureTile(card) }
        }
    }
}

@Composable
private fun FeatureTile(card: FeatureCard) {
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .background(scheme.surfaceVariant, RoundedCornerShape(20.dp))
            .clickable(onClick = card.onClick)
            .padding(16.dp)
    ) {
        Icon(
            card.icon,
            contentDescription = null,
            tint = scheme.primary,
            modifier = Modifier
                .size(36.dp)
                .align(Alignment.TopStart)
        )
        if (card.badge != null) {
            Text(
                card.badge,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = scheme.onPrimary,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .background(scheme.primary, RoundedCornerShape(12.dp))
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            )
        }
        Text(
            stringResource(card.titleRes),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = scheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.BottomStart)
        )
    }
}
