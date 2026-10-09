package com.yasminhosam.brandora.feature.catalogimport.presentation.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.yasminhosam.brandora.R

/** TEMP: navigation target for the manual-entry link until the real screen lands. */
@Composable
fun AddProductPlaceholderScreen(modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .fillMaxSize()
            .background(scheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp)
    ) {
        Text(
            text = stringResource(R.string.import_add_product_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = scheme.onBackground,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.import_add_product_placeholder),
            style = MaterialTheme.typography.bodyLarge,
            color = scheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}
