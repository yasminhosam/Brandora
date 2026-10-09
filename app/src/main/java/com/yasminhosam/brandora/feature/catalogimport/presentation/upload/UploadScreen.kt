package com.yasminhosam.brandora.feature.catalogimport.presentation.upload

import android.content.res.Configuration
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.yasminhosam.brandora.R
import com.yasminhosam.brandora.core.ui.messageRes
import com.yasminhosam.brandora.core.ui.theme.BrandoraTheme
import com.yasminhosam.brandora.feature.catalogimport.domain.model.ExcelFile
import java.util.Locale

@Composable
fun UploadScreen(
    state: UploadUiState,
    onEvent: (UploadEvent) -> Unit,
    onBrowseClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    Column(
        // Edge-to-edge: keep content clear of the status and gesture bars.
        modifier = modifier
            .fillMaxSize()
            .background(scheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp)
            .padding(top = 16.dp, bottom = 12.dp)
    ) {
        Text(
            text = stringResource(R.string.import_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = scheme.onBackground
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.import_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            color = scheme.onSurfaceVariant
        )
        Spacer(Modifier.height(28.dp))

        val file = state.file
        if (file == null) {
            DropZone(hasError = state.fileError != null, onClick = onBrowseClick)
        } else {
            FileCard(
                name = file.displayName,
                sizeBytes = file.sizeBytes,
                onChangeClick = onBrowseClick
            )
        }

        if (state.isAnalyzing) {
            Spacer(Modifier.height(20.dp))
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.import_analyzing),
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.onSurfaceVariant
            )
        }

        state.visibleError?.let { error ->
            Spacer(Modifier.height(12.dp))
            Text(
                text = stringResource(error.messageRes()),
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.error
            )
        }

        Spacer(Modifier.height(20.dp))
        Button(
            onClick = { onEvent(UploadEvent.ContinueClicked) },
            enabled = state.canContinue,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Text(text = stringResource(R.string.import_continue))
        }

        Spacer(Modifier.weight(1f))
        TextButton(
            onClick = { onEvent(UploadEvent.ManualEntryClicked) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = stringResource(R.string.import_add_first_product),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun DropZone(hasError: Boolean, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(210.dp)
            .dashedBorder(if (hasError) scheme.error else scheme.outline)
            .clip(RoundedCornerShape(20.dp))
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(24.dp)
        ) {
            UploadGlyph(tint = scheme.primary, container = scheme.primaryContainer)
            Spacer(Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.import_drop_hint),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = scheme.onBackground,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.import_drop_formats),
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun FileCard(name: String, sizeBytes: Long, onChangeClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Card(
        onClick = onChangeClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = scheme.surfaceContainerLow),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(scheme.primaryContainer, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "XLSX",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = scheme.onPrimaryContainer
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleSmall,
                    color = scheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (sizeBytes >= 0) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = formatFileSize(sizeBytes),
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant
                    )
                }
            }
            TextButton(onClick = onChangeClick) {
                Text(text = stringResource(R.string.import_change_file))
            }
        }
    }
}

/** Hand-drawn: icons-core has no upload glyph and icons-extended isn't worth the APK weight. */
@Composable
private fun UploadGlyph(tint: Color, container: Color) {
    Box(
        modifier = Modifier
            .size(76.dp)
            .background(container, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.size(34.dp)) {
            val stroke = 3.dp.toPx()
            val centerX = size.width / 2f
            val arrowTip = size.height * 0.16f
            val arrowBase = size.height * 0.52f
            val head = size.width * 0.20f
            drawLine(tint, Offset(centerX, arrowBase), Offset(centerX, arrowTip), stroke, StrokeCap.Round)
            drawLine(tint, Offset(centerX, arrowTip), Offset(centerX - head, arrowTip + head), stroke, StrokeCap.Round)
            drawLine(tint, Offset(centerX, arrowTip), Offset(centerX + head, arrowTip + head), stroke, StrokeCap.Round)
            val trayTop = size.height * 0.54f
            val trayBottom = size.height * 0.84f
            val trayLeft = size.width * 0.14f
            val trayRight = size.width * 0.86f
            drawLine(tint, Offset(trayLeft, trayTop), Offset(trayLeft, trayBottom), stroke, StrokeCap.Round)
            drawLine(tint, Offset(trayLeft, trayBottom), Offset(trayRight, trayBottom), stroke, StrokeCap.Round)
            drawLine(tint, Offset(trayRight, trayTop), Offset(trayRight, trayBottom), stroke, StrokeCap.Round)
        }
    }
}

private fun Modifier.dashedBorder(
    color: Color,
    cornerRadius: Dp = 20.dp,
    strokeWidth: Dp = 1.5.dp,
    dashLength: Dp = 10.dp,
    gapLength: Dp = 8.dp
): Modifier = composed {
    val density = LocalDensity.current
    val dashPx = with(density) { dashLength.toPx() }
    val gapPx = with(density) { gapLength.toPx() }
    drawBehind {
        drawRoundRect(
            color = color,
            style = Stroke(
                width = strokeWidth.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(dashPx, gapPx), 0f)
            ),
            cornerRadius = CornerRadius(cornerRadius.toPx())
        )
    }
}

@Composable
private fun formatFileSize(bytes: Long): String {
    val kb = bytes / 1024.0
    return if (bytes < 1024L * 1024L) {
        stringResource(R.string.import_size_kb, String.format(Locale.getDefault(), "%.1f", kb))
    } else {
        stringResource(R.string.import_size_mb, String.format(Locale.getDefault(), "%.1f", kb / 1024.0))
    }
}

private fun previewFile() = ExcelFile("products-catalog.xlsx", 2_457_600L) {
    ByteArray(0).inputStream()
}

@Preview(name = "Empty", showBackground = true)
@Composable
private fun UploadEmptyPreview() {
    BrandoraTheme {
        UploadScreen(state = UploadUiState(), onEvent = {}, onBrowseClick = {})
    }
}

@Preview(name = "Empty Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun UploadEmptyDarkPreview() {
    BrandoraTheme {
        UploadScreen(state = UploadUiState(), onEvent = {}, onBrowseClick = {})
    }
}

@Preview(name = "File selected", showBackground = true)
@Composable
private fun UploadFilePreview() {
    BrandoraTheme {
        UploadScreen(state = UploadUiState(file = previewFile()), onEvent = {}, onBrowseClick = {})
    }
}

@Preview(name = "Analyzing", showBackground = true)
@Composable
private fun UploadAnalyzingPreview() {
    BrandoraTheme {
        UploadScreen(
            state = UploadUiState(file = previewFile(), isAnalyzing = true),
            onEvent = {},
            onBrowseClick = {}
        )
    }
}
