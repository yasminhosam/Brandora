package com.yasminhosam.brandora.feature.products.presentation

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.yasminhosam.brandora.R
import com.yasminhosam.brandora.core.network.ApiErrorType
import com.yasminhosam.brandora.core.network.ApiResult
import com.yasminhosam.brandora.core.ui.theme.BrandoraTheme
import com.yasminhosam.brandora.core.ui.theme.StatusStyle
import com.yasminhosam.brandora.core.ui.theme.extended
import com.yasminhosam.brandora.feature.products.domain.model.Product
import com.yasminhosam.brandora.feature.products.domain.model.Variant

/** Error banner + cleanup warning shared by every products screen. */
@Composable
internal fun ProductError(state: ProductsState, onRetry: () -> Unit) {
    val error = state.error
    // Hold the last error so the banner can animate out after the state clears it.
    var shownError by remember { mutableStateOf(error) }
    if (error != null) shownError = error
    AnimatedVisibility(
        visible = error != null,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut(),
    ) {
        val displayed = shownError ?: error ?: return@AnimatedVisibility
        ErrorBanner(
            message = stringResource(ProductsErrorMapper.messageRes(displayed)),
            container = MaterialTheme.colorScheme.errorContainer,
            content = MaterialTheme.colorScheme.onErrorContainer,
            retryEnabled = !state.busy,
            onRetry = if (ProductsErrorMapper.isRetryable(displayed)) onRetry else null,
        )
    }
    AnimatedVisibility(
        visible = state.cleanupPending,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut(),
    ) {
        ErrorBanner(
            message = stringResource(R.string.pm_cleanup_pending),
            container = MaterialTheme.extended.warning.container,
            content = MaterialTheme.extended.warning.content,
            retryEnabled = false,
            onRetry = null,
        )
    }
}

/**
 * Text-only banner: the bundled icon set has no warning glyph, so the container colour carries
 * the severity and the text stays the app's normal type.
 */
@Composable
private fun ErrorBanner(
    message: String,
    container: Color,
    content: Color,
    retryEnabled: Boolean,
    onRetry: (() -> Unit)?,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            Modifier.fillMaxWidth()
                .background(container, RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = content,
            modifier = Modifier.weight(1f),
        )
        if (onRetry != null)
            TextButton(
                onClick = onRetry,
                enabled = retryEnabled,
                colors = ButtonDefaults.textButtonColors(contentColor = content),
            ) {
                Text(stringResource(R.string.action_retry))
            }
    }
}

/** Colored status pill built from the app's extended palette (light and dark aware). */
@Composable
internal fun StatusBadge(text: String, style: StatusStyle, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        color = style.content,
        maxLines = 1,
        modifier =
            modifier
                .background(style.container, RoundedCornerShape(percent = 50))
                .padding(horizontal = 10.dp, vertical = 3.dp),
    )
}

/** Product sale state as a badge; renders nothing while the product is sellable. */
@Composable
internal fun ProductStatusBadge(product: Product, modifier: Modifier = Modifier) {
    val ext = MaterialTheme.extended
    when {
        !product.isActive -> StatusBadge(stringResource(R.string.pm_inactive), ext.danger, modifier)
        !product.isSellable ->
            StatusBadge(stringResource(R.string.pm_not_sellable), ext.warning, modifier)
    }
}

/** Inactive variants read as a red pill on every screen that lists them. */
@Composable
internal fun VariantStatusBadge(variant: Variant, modifier: Modifier = Modifier) {
    if (!variant.isActive)
        StatusBadge(stringResource(R.string.pm_inactive), MaterialTheme.extended.danger, modifier)
}

/** Out-of-stock quantities read red; anything else keeps the default text color. */
@Composable
internal fun stockColor(variant: Variant): Color =
    if (variant.trackInventory && (variant.stockQuantity ?: 0) == 0)
        MaterialTheme.extended.danger.content
    else Color.Unspecified

/** Titled section with the feature's rhythm: 16dp card gap + 8dp = 24dp. */
@Composable
internal fun SectionHeader(@StringRes title: Int, modifier: Modifier = Modifier) {
    Text(
        stringResource(title),
        style = MaterialTheme.typography.titleLarge,
        modifier = modifier.padding(top = 8.dp),
    )
}

/** Centered empty placeholder that fills the space under the app bar. */
@Composable
internal fun EmptyState(
    @StringRes text: Int,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
) {
    Box(
        modifier.fillMaxWidth().heightIn(min = 360.dp).padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (icon != null)
                Icon(
                    icon,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.outline,
                )
            Text(
                stringResource(text),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.extended.textMuted,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/** Sweeping gradient shared by the browse loading skeletons (theme-aware, light and dark). */
@Composable
internal fun rememberShimmerBrush(): Brush {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val progress by
        transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(1_200, easing = LinearEasing)),
            label = "shimmer-progress",
        )
    val base = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
    val highlight = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.14f)
    val shift = 3_000f * progress
    return Brush.linearGradient(
        colors = listOf(base, highlight, base),
        start = Offset(-1_500f + shift, 0f),
        end = Offset(500f + shift, 0f),
    )
}

/** Loading placeholder shaped like a browse card (image slot, name, price and badge rows). */
@Composable
internal fun ProductCardSkeleton(modifier: Modifier = Modifier) {
    val brush = rememberShimmerBrush()
    Card(modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                Modifier.size(84.dp).background(brush, RoundedCornerShape(12.dp)),
            )
            Column(
                Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Box(
                    Modifier.fillMaxWidth(0.7f)
                        .height(16.dp)
                        .background(brush, RoundedCornerShape(4.dp))
                )
                Box(
                    Modifier.fillMaxWidth(0.4f)
                        .height(16.dp)
                        .background(brush, RoundedCornerShape(4.dp))
                )
                Box(Modifier.width(56.dp).height(16.dp).background(brush, RoundedCornerShape(8.dp)))
            }
        }
    }
}

/** Absolute-quantity dialog used by the variant inventory screen. */
@Composable
internal fun StockDialog(
    variant: Variant,
    busy: Boolean,
    onCancel: () -> Unit,
    onSave: (Int?) -> Unit,
) {
    var tracked by rememberSaveable { mutableStateOf(variant.trackInventory) }
    var quantity by rememberSaveable { mutableStateOf(variant.stockQuantity?.toString().orEmpty()) }
    var invalid by rememberSaveable { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(stringResource(R.string.pm_stock)) },
        text = {
            Column {
                Row {
                    Checkbox(tracked, { tracked = it }, enabled = !busy)
                    Text(stringResource(R.string.pm_tracking))
                }
                Field(
                    quantity,
                    { quantity = it },
                    R.string.pm_quantity,
                    enabled = !busy && tracked,
                    keyboard = KeyboardType.Number,
                )
                Text(stringResource(R.string.pm_absolute_quantity))
                if (invalid)
                    Text(
                        stringResource(R.string.pm_quantity_invalid),
                        color = MaterialTheme.colorScheme.error,
                    )
            }
        },
        confirmButton = {
            TextButton(
                {
                    val n = quantity.toIntOrNull()
                    invalid = tracked && (n == null || n < 0)
                    if (!invalid) onSave(if (tracked) n else null)
                },
                enabled = !busy,
            ) {
                Text(stringResource(R.string.pm_save))
            }
        },
        dismissButton = { TextButton(onCancel) { Text(stringResource(R.string.pm_cancel)) } },
    )
}

/** Shared single-line labeled text field; full-width unless the caller overrides [modifier]. */
@Composable
internal fun Field(
    value: String,
    change: (String) -> Unit,
    label: Int,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    keyboard: KeyboardType = KeyboardType.Text,
    @StringRes error: Int? = null,
) {
    OutlinedTextField(
        value,
        change,
        label = { Text(stringResource(label)) },
        enabled = enabled,
        isError = error != null,
        supportingText =
            if (error != null) {
                { Text(stringResource(error)) }
            } else null,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboard),
        modifier = modifier.fillMaxWidth(),
    )
}

@Preview(name = "Error / retry", showBackground = true)
@Composable
private fun ProductErrorPreview() {
    BrandoraTheme {
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            ProductError(
                state =
                    ProductsState(
                        error =
                            ApiResult.Error(
                                ApiErrorType.CONFLICT,
                                errorCode = "PRODUCT_WRITE_CONFLICT",
                            )
                    ),
                onRetry = {},
            )
        }
    }
}
