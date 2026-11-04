package com.yasminhosam.brandora.feature.auth.presentation

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.yasminhosam.brandora.R
import com.yasminhosam.brandora.core.ui.theme.BrandoraTheme

/**
 * Centered Brandora lockup shown at the top of every auth screen, above the title.
 *
 * The stacked artwork lives as `ic_brandora_logo_compact` in both `drawable/` and
 * `drawable-night/`, so Android swaps it with the theme (light: navy wordmark,
 * dark: white wordmark) — one resource name, no code.
 *
 * Sizing/cropping: the canvas (448x360) carries ~34 units of head room above the lockup
 * and ~110 units of empty space below the wordmark (which ends at y=250.5). At 160dp
 * wide the scale is 160/448 = 0.357, so the artwork ends at ~89.5dp; the top-aligned
 * 160x96dp crop keeps all of it plus a small margin and drops the dead space, leaving
 * the gap to the title to the Spacer at the call site. Re-check the height if the
 * artwork is ever re-exported with different padding.
 */
@Composable
fun AuthBrandHeader(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
        Image(
            // The wordmark IS the brand name; nothing else on these screens says
            // "Brandora", so announce it once when TalkBack enters the screen.
            painter = painterResource(R.drawable.ic_brandora_logo_compact),
            contentDescription = stringResource(R.string.app_name),
            // autoMirrored was removed from the vectors: the Latin wordmark must not
            // flip in Arabic. The crop only trims the canvas padding below the art.
            contentScale = ContentScale.Crop,
            alignment = Alignment.TopCenter,
            modifier = Modifier.width(160.dp).height(96.dp)
        )
    }
}

/**
 * Renders the header on the real dark surface (drawable-night picks the white wordmark
 * and the #3B5BC4 strokes via the preview's uiMode) so the contrast can be judged in the
 * Android Studio preview pane without running the app.
 */
@Preview(name = "Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AuthBrandHeaderDarkPreview() {
    BrandoraTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            AuthBrandHeader()
        }
    }
}
