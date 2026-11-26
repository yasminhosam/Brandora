@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.yasminhosam.brandora.feature.products.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.yasminhosam.brandora.R
import com.yasminhosam.brandora.core.ui.theme.BrandoraTheme
import com.yasminhosam.brandora.feature.products.domain.*
import com.yasminhosam.brandora.feature.products.domain.model.*

/**
 * Create/edit product: grouped sections (Details / Pricing / Category / Product options), a sticky
 * Save+Cancel bar that stays above the keyboard, and inline client-rule feedback after the first
 * save attempt. The A2 options lock is unchanged: once variants exist the switch is off and removals
 * are rejected by the same gate as before.
 */
@Composable
internal fun ProductEditor(
    state: ProductsState,
    onCancel: () -> Unit,
    event: (ProductsEvent) -> Unit,
) {
    val p = state.selected
    var categoriesRequested by remember(state.businessId) { mutableStateOf(false) }
    LaunchedEffect(state.busy, state.businessId) {
        if (
            !state.busy &&
                state.businessId != null &&
                !state.categoriesLoaded &&
                !categoriesRequested
        ) {
            categoriesRequested = true
            event(ProductsEvent.Categories)
        }
    }
    var name by rememberSaveable(p?.id) { mutableStateOf(p?.name.orEmpty()) }
    var external by rememberSaveable(p?.id) { mutableStateOf(p?.externalKey.orEmpty()) }
    var description by rememberSaveable(p?.id) { mutableStateOf(p?.description.orEmpty()) }
    var price by rememberSaveable(p?.id) { mutableStateOf(p?.basePrice.orEmpty()) }
    var category by rememberSaveable(p?.id) { mutableStateOf(p?.categoryId) }
    var options: List<OptionDraft> by
        rememberSaveable(p?.id) {
            mutableStateOf<List<OptionDraft>>(
                p?.options?.map { OptionDraft(it.key, it.value.joinToString("\n")) } ?: emptyList()
            )
        }
    var hasOptions by
        rememberSaveable(p?.id) { mutableStateOf(p?.options?.isNotEmpty() ?: false) }
    var saveAttempted by rememberSaveable { mutableStateOf(false) }
    var categoryName by rememberSaveable { mutableStateOf("") }
    // A2: options lock once any variant exists — expansions are fine, removals are not.
    val locked = p != null && p.variantsPagination.totalCount > 0
    val definitions = if (hasOptions) ProductRules.definitions(options) else emptyMap<String, List<String>>()
    // Nothing is discarded without asking: the draft is compared against the saved product
    // (or an empty form in create mode), including a half-typed new category.
    val dirty =
        name != p?.name.orEmpty() ||
            external != (p?.externalKey ?: "") ||
            description != (p?.description ?: "") ||
            price != (p?.basePrice ?: "") ||
            category != p?.categoryId ||
            options !=
                (p?.options?.map { OptionDraft(it.key, it.value.joinToString("\n")) }
                    ?: emptyList<OptionDraft>()) ||
            hasOptions != (p?.options?.isNotEmpty() ?: false) ||
            categoryName.isNotBlank()
    var confirmDiscard by rememberSaveable { mutableStateOf(false) }
    // Composed after the route's own handler, so while the editor holds changes this one wins.
    BackHandler(enabled = dirty && !state.busy) { confirmDiscard = true }
    // Client rules surface inline after the first save attempt; the gate below is unchanged.
    val nameError =
        saveAttempted &&
            (name.isBlank() || name.trim().codePointCount(0, name.trim().length) > 200)
    val externalError = saveAttempted && !ProductRules.validExternalKey(external)
    val descriptionError = saveAttempted && description.codePointCount(0, description.length) > 5000
    val priceError = saveAttempted && !ProductRules.validMoney(price.trim())
    val optionsInvalid = saveAttempted && hasOptions && definitions == null
    val optionsSuperset =
        saveAttempted &&
            locked &&
            definitions != null &&
            !ProductRules.validOptionSuperset(p?.options ?: emptyMap(), definitions)

    Column(
        Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding(),
    ) {
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                stringResource(if (p == null) R.string.pm_add else R.string.pm_edit_product),
                style = MaterialTheme.typography.headlineSmall,
            )
            ProductError(state) { event(ProductsEvent.Retry) }
            if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())

            SectionHeader(R.string.pm_details)
            Field(
                name,
                { name = it },
                R.string.pm_name,
                enabled = !state.busy,
                error = if (nameError) R.string.pm_name_error else null,
            )
            Field(
                external,
                { external = it },
                R.string.pm_external,
                enabled = !state.busy,
                error = if (externalError) R.string.pm_external_error else null,
            )
            Text(
                stringResource(R.string.pm_external_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(
                description,
                { description = it },
                label = { Text(stringResource(R.string.pm_description)) },
                enabled = !state.busy,
                isError = descriptionError,
                supportingText =
                    if (descriptionError) {
                        { Text(stringResource(R.string.pm_description_error)) }
                    } else null,
                modifier = Modifier.fillMaxWidth(),
            )

            SectionHeader(R.string.pm_pricing)
            Field(
                price,
                { price = it },
                R.string.pm_base_price,
                enabled = !state.busy,
                keyboard = KeyboardType.Decimal,
                error = if (priceError) R.string.pm_price_error else null,
            )

            SectionHeader(R.string.pm_category)
            if (!state.categoriesLoaded && !state.busy)
                TextButton({ event(ProductsEvent.Categories) }) {
                    Text(stringResource(R.string.pm_refresh))
                }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    category == null,
                    { category = null },
                    label = { Text(stringResource(R.string.pm_no_category)) },
                    enabled = !state.busy,
                )
                state.categories.forEach { c ->
                    FilterChip(
                        category == c.id,
                        { category = c.id },
                        label = { Text(c.name) },
                        enabled = !state.busy,
                    )
                }
            }
            if (state.categoryPage.hasMore)
                TextButton({ event(ProductsEvent.MoreCategories) }, enabled = !state.busy) {
                    Text(stringResource(R.string.pm_more))
                }
            Field(
                categoryName,
                { categoryName = it },
                R.string.pm_new_category,
                enabled = !state.busy,
            )
            TextButton(
                {
                    event(ProductsEvent.CreateCategory(categoryName))
                    categoryName = ""
                },
                enabled =
                    !state.busy &&
                        categoryName.isNotBlank() &&
                        categoryName.trim().codePointCount(0, categoryName.trim().length) <= 200,
            ) {
                Text(stringResource(R.string.pm_add_category))
            }

            SectionHeader(R.string.pm_options)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Switch(hasOptions, { hasOptions = it }, enabled = !state.busy && !locked)
                Text(stringResource(R.string.pm_has_options_q))
            }
            if (locked)
                Text(
                    stringResource(R.string.pm_options_locked),
                    style = MaterialTheme.typography.bodySmall,
                )
            AnimatedVisibility(
                visible = hasOptions,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        stringResource(R.string.pm_options_hint),
                        style = MaterialTheme.typography.bodySmall,
                    )
                    // Presets only seed the local draft — the option names stay user-editable data.
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        listOf(
                            R.string.pm_preset_size,
                            R.string.pm_preset_color,
                            R.string.pm_preset_material,
                            R.string.pm_preset_weight,
                            R.string.pm_preset_volume,
                        ).forEach { presetRes ->
                            val preset = stringResource(presetRes)
                            FilterChip(
                                false,
                                {
                                    val emptyIndex = options.indexOfFirst { it.name.isBlank() }
                                    options =
                                        if (emptyIndex >= 0)
                                            options.mapIndexed { i, o ->
                                                if (i == emptyIndex) o.copy(name = preset) else o
                                            }
                                        else options + OptionDraft(name = preset)
                                },
                                label = { Text(preset) },
                                enabled = !state.busy,
                            )
                        }
                    }
                    options.forEachIndexed { index, option ->
                        Card(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(16.dp)) {
                                Field(
                                    option.name,
                                    { value ->
                                        options =
                                            options.mapIndexed { i, v ->
                                                if (i == index) v.copy(name = value) else v
                                            }
                                    },
                                    R.string.pm_option_name,
                                    enabled = !state.busy,
                                    error =
                                        if (optionsInvalid && option.name.isBlank())
                                            R.string.pm_options_error
                                        else null,
                                )
                                OutlinedTextField(
                                    option.values,
                                    { value ->
                                        options =
                                            options.mapIndexed { i, v ->
                                                if (i == index) v.copy(values = value) else v
                                            }
                                    },
                                    label = { Text(stringResource(R.string.pm_option_values)) },
                                    enabled = !state.busy,
                                    isError = optionsInvalid && option.values.isBlank(),
                                    modifier = Modifier.fillMaxWidth(),
                                )
                                if (!locked)
                                    TextButton(
                                        { options = options.filterIndexed { i, _ -> i != index } },
                                        enabled = !state.busy,
                                    ) {
                                        Text(stringResource(R.string.pm_remove_option))
                                    }
                            }
                        }
                    }
                    TextButton(
                        { options = options + OptionDraft() },
                        enabled = !state.busy && options.size < 10,
                    ) {
                        Text(stringResource(R.string.pm_add_option))
                    }
                }
            }
            if (optionsSuperset)
                Text(
                    stringResource(R.string.pm_options_locked),
                    color = MaterialTheme.colorScheme.error,
                )
            else if (optionsInvalid)
                Text(
                    stringResource(R.string.pm_options_error),
                    color = MaterialTheme.colorScheme.error,
                )
        }
        HorizontalDivider()
        Row(
            Modifier.fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Button(
                {
                    saveAttempted = true
                    // Identical gate to before: compute every rule, dispatch only when all pass.
                    val defs = if (hasOptions) ProductRules.definitions(options) else emptyMap<String, List<String>>()
                    val optionsBlocked =
                        locked &&
                            defs != null &&
                            !ProductRules.validOptionSuperset(p?.options ?: emptyMap(), defs)
                    val invalidNow =
                        name.isBlank() ||
                            name.trim().codePointCount(0, name.trim().length) > 200 ||
                            description.codePointCount(0, description.length) > 5000 ||
                            !ProductRules.validExternalKey(external) ||
                            !ProductRules.validMoney(price.trim()) ||
                            (hasOptions && defs == null)
                    if (!invalidNow && !optionsBlocked)
                        event(
                            ProductsEvent.Save(
                                p?.id,
                                ProductDraft(
                                    name = name.trim(),
                                    externalKey = external.trim().takeIf { it.isNotEmpty() },
                                    description = description.takeIf { it.isNotEmpty() },
                                    basePrice = price.trim().takeIf { it.isNotEmpty() },
                                    categoryId = category,
                                    options = defs ?: emptyMap(),
                                ),
                            )
                        )
                },
                enabled = !state.busy && state.businessId != null,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.weight(1f).height(52.dp),
            ) {
                if (state.busy)
                    CircularProgressIndicator(
                        Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                else Text(stringResource(R.string.pm_save))
            }
            TextButton(
                { if (dirty) confirmDiscard = true else onCancel() },
                enabled = !state.busy,
            ) {
                Text(stringResource(R.string.pm_cancel))
            }
        }
    }
    if (confirmDiscard)
        AlertDialog(
            onDismissRequest = { confirmDiscard = false },
            title = { Text(stringResource(R.string.pm_confirm_title)) },
            text = { Text(stringResource(R.string.pm_confirm_discard)) },
            confirmButton = {
                TextButton(
                    {
                        confirmDiscard = false
                        onCancel()
                    }
                ) {
                    Text(stringResource(R.string.pm_discard))
                }
            },
            dismissButton = {
                TextButton({ confirmDiscard = false }) {
                    Text(stringResource(R.string.pm_cancel))
                }
            },
        )
}

@Preview(showBackground = true, locale = "ar")
@Composable
private fun Preview() {
    BrandoraTheme {
        ProductEditor(state = ProductsState(businessId = "b1"), onCancel = {}, event = {})
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewLocked() {
    BrandoraTheme {
        ProductEditor(
            state =
                ProductsState(
                    businessId = "b1",
                    selected =
                        Product(
                            id = "p1",
                            businessId = "b1",
                            name = "Shirt",
                            description = "Cotton shirt",
                            basePrice = "12.50",
                            options = mapOf("Size" to listOf("S", "M")),
                            variantsPagination = PageInfo(totalCount = 2),
                        ),
                ),
            onCancel = {},
            event = {},
        )
    }
}
