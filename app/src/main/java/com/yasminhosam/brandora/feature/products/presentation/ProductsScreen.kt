@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.yasminhosam.brandora.feature.products.presentation

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yasminhosam.brandora.LocalBottomBarVisible
import com.yasminhosam.brandora.R
import com.yasminhosam.brandora.feature.products.domain.*
import com.yasminhosam.brandora.feature.products.domain.model.*
import kotlinx.coroutines.launch

/**
 * Products shell: editor/detail/inventory routing, the two bottom sheets, the confirm dialog
 * and the back stack. Screen bodies live in their own files — ProductsBrowseScreen,
 * ProductDetailScreen, ProductEditorScreen, VariantEditorScreen, VariantInventoryScreen and
 * ProductsComponents.
 */
@Composable
fun ProductsRoute(
    onImport: () -> Unit,
    viewModel: ProductsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val event = viewModel::onEvent
    val snackbarHost = remember { SnackbarHostState() }
    // Resolved at composition time so the snackbar follows configuration and locale changes.
    val savedProductMessage = stringResource(R.string.pm_saved)
    val savedVariantMessage = stringResource(R.string.pm_variant_saved)
    var productEditor by rememberSaveable(state.businessId) { mutableStateOf(false) }
    var variantEditor by
        rememberSaveable(state.businessId, state.selected?.id) { mutableStateOf(false) }
    var variantDraft by
        rememberSaveable(state.businessId, state.selected?.id) { mutableStateOf<Variant?>(null) }
    var stock by
        rememberSaveable(state.businessId, state.selected?.id) { mutableStateOf<Variant?>(null) }
    var confirm by
        remember(state.businessId, state.selected?.id) { mutableStateOf<(() -> Unit)?>(null) }
    var confirmText by
        remember(state.businessId, state.selected?.id) {
            mutableIntStateOf(R.string.pm_confirm_delete)
        }
    var addSheet by rememberSaveable(state.businessId) { mutableStateOf(false) }
    var manageSheet by rememberSaveable(state.businessId) { mutableStateOf(false) }
    var categoryName by rememberSaveable(state.businessId) { mutableStateOf("") }
    var renamingId by rememberSaveable(state.businessId) { mutableStateOf<String?>(null) }
    var renameValue by rememberSaveable(state.businessId) { mutableStateOf("") }
    var browseCategoriesRequested by rememberSaveable(state.businessId) { mutableStateOf(false) }
    val picker =
        rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
            if (uris.isNotEmpty()) event(ProductsEvent.Upload(uris))
        }
    LaunchedEffect(Unit) { event(ProductsEvent.Refresh) }
    // The chip row needs categories on first browse; fetched once per business, then cached.
    LaunchedEffect(state.busy, state.businessId, state.selected == null) {
        if (
            !state.busy &&
                state.businessId != null &&
                state.selected == null &&
                !state.categoriesLoaded &&
                !browseCategoriesRequested
        ) {
            browseCategoriesRequested = true
            event(ProductsEvent.Categories)
        }
    }
    LaunchedEffect(viewModel) {
        viewModel.effects.collect {
            when (it) {
                ProductsEffect.Saved -> {
                    // Close the editor first, then confirm — the host lives at route level.
                    val message =
                        if (variantEditor) savedVariantMessage
                        else if (productEditor) savedProductMessage
                        else null
                    productEditor = false
                    variantEditor = false
                    if (message != null) launch { snackbarHost.showSnackbar(message) }
                }
            }
        }
    }
    fun closeManage() {
        manageSheet = false
        categoryName = ""
        renamingId = null
        renameValue = ""
    }
    BackHandler(
        productEditor ||
            variantEditor ||
            stock != null ||
            state.selected != null ||
            state.historyVariant != null ||
            addSheet ||
            manageSheet ||
            state.filter != CategoryFilter.All
    ) {
        if (!state.busy)
            when {
                stock != null -> stock = null
                variantEditor -> variantEditor = false
                productEditor -> productEditor = false
                manageSheet -> closeManage()
                addSheet -> addSheet = false
                // Inventory sits above detail; detail above browse-with-filter.
                state.historyVariant != null -> event(ProductsEvent.Back)
                state.selected != null -> event(ProductsEvent.Back)
                // Only reachable on browse with an active filter: clear it, don't leave.
                else -> event(ProductsEvent.Filter(null))
            }
    }
    // One scroll position per screen: a nested screen always opens at the top while browse
    // keeps the merchant's place in the list.
    val browseScroll = rememberScrollState()
    val detailScroll = rememberScrollState()
    val inventoryScroll = rememberScrollState()
    val screen =
        when {
            state.selected == null -> "browse"
            state.historyVariant != null -> "inventory"
            else -> "detail"
        }
    // Nested screens start fresh on every entry; browse's own offset is never touched.
    LaunchedEffect(screen) {
        if (screen == "browse") {
            detailScroll.scrollTo(0)
            inventoryScroll.scrollTo(0)
        }
    }
    // Detail, inventory and both editors need the full height, so the bar belongs to browse
    // only. Written here, read by BrandoraRoot; reset when this route leaves composition.
    val bottomBarVisible = LocalBottomBarVisible.current
    val showBottomBar = screen == "browse" && !productEditor && !variantEditor
    LaunchedEffect(showBottomBar) { bottomBarVisible.value = showBottomBar }
    DisposableEffect(Unit) { onDispose { bottomBarVisible.value = true } }
    if (productEditor) {
        ProductEditor(state, { productEditor = false }, event)
        return
    }
    if (variantEditor) {
        state.selected?.let {
            VariantEditor(
                it,
                variantDraft,
                state,
                { variantEditor = false },
                event,
            ) { id, body ->
                event(ProductsEvent.SaveVariant(id, body))
            }
        }
        return
    }
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            // Browse is the graph root, so its header has no up arrow; pushed screens get
            // one that fires the same Back event the old text button did.
            TopAppBar(
                title = { Text(stringResource(R.string.pm_title)) },
                navigationIcon = {
                    if (state.selected != null)
                        IconButton({ event(ProductsEvent.Back) }, enabled = !state.busy) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.pm_back),
                            )
                        }
                },
                actions = {
                    IconButton(
                        { event(ProductsEvent.Refresh) },
                        enabled = !state.busy && state.businessId != null,
                    ) {
                        Icon(
                            Icons.Filled.Refresh,
                            contentDescription = stringResource(R.string.pm_refresh),
                        )
                    }
                },
            )
            // Shared header stays above the scroll so the progress line and the retry banner
            // remain reachable while a long list scrolls underneath them.
            val content: @Composable () -> Unit = {
                Column(
                    Modifier.fillMaxSize().padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    if (state.businessId == null)
                        Text(stringResource(R.string.pm_business_required))
                    if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                    ProductError(state) { event(ProductsEvent.Retry) }
                    // One content slot per screen; Crossfade eases browse↔detail↔inventory.
                    // Each branch owns its scroll state so a nested screen opens at the top,
                    // and each is a single Column: Crossfade places every child of a state at
                    // the same position, so a multi-child branch would stack its rows.
                    Crossfade(
                        targetState = screen,
                        modifier = Modifier.weight(1f),
                        label = "products-screen",
                    ) { current ->
                        Column(
                            Modifier.fillMaxSize()
                                .verticalScroll(
                                    when (current) {
                                        "browse" -> browseScroll
                                        "inventory" -> inventoryScroll
                                        else -> detailScroll
                                    }
                                ),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            when (current) {
                                "browse" ->
                                    ProductsBrowse(state, event) {
                                        renamingId = null
                                        renameValue = ""
                                        categoryName = ""
                                        manageSheet = true
                                    }
                                "inventory" ->
                                    VariantInventory(state, event, onStock = { stock = it })
                                else ->
                                    ProductDetail(
                                        state = state,
                                        event = event,
                                        onEdit = {
                                            productEditor = true
                                            event(ProductsEvent.Categories)
                                        },
                                        onVariant = { draft ->
                                            variantDraft = draft
                                            variantEditor = true
                                        },
                                        onConfirm = { message, action ->
                                            confirmText = message
                                            confirm = action
                                        },
                                        onUpload = { picker.launch("image/*") },
                                    )
                            }
                        }
                    }
                }
            }
            // Pull-to-refresh lives on browse only, bound to the existing Refresh event.
            if (screen == "browse") {
                PullToRefreshBox(
                    isRefreshing = state.busy && state.items.isNotEmpty(),
                    onRefresh = { event(ProductsEvent.Refresh) },
                    modifier = Modifier.fillMaxSize().navigationBarsPadding(),
                ) { content() }
            } else {
                Box(Modifier.fillMaxSize().navigationBarsPadding()) { content() }
            }
        }
        if (state.selected == null && state.businessId != null)
            FloatingActionButton(
                onClick = { addSheet = true },
                modifier =
                    Modifier.align(Alignment.BottomEnd).padding(20.dp).navigationBarsPadding(),
            ) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.pm_add))
            }
        SnackbarHost(
            snackbarHost,
            Modifier.align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
                .padding(bottom = 72.dp),
        )
    }
    if (addSheet) {
        ModalBottomSheet(onDismissRequest = { if (!state.busy) addSheet = false }) {
            Column(Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
                Text(
                    stringResource(R.string.pm_add),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
                SheetRow(stringResource(R.string.pm_add_manually), !state.busy) {
                    addSheet = false
                    productEditor = true
                    event(ProductsEvent.Categories)
                }
                SheetRow(stringResource(R.string.pm_import), !state.busy) {
                    addSheet = false
                    onImport()
                }
            }
        }
    }
    if (manageSheet) {
        // A long category list would push the rename actions and the create-category form past
        // the sheet's edge with no way to reach them, so the whole body scrolls instead of
        // clipping. skipPartiallyExpanded keeps it anchored open rather than at half height.
        ModalBottomSheet(
            onDismissRequest = { if (!state.busy) closeManage() },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        ) {
            Column(
                Modifier.fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    stringResource(R.string.pm_manage_categories),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
                ProductError(state) { event(ProductsEvent.Retry) }
                state.categories.forEach { c ->
                    if (renamingId == c.id) {
                        Field(
                            renameValue,
                            { renameValue = it },
                            R.string.pm_category_name,
                            enabled = !state.busy,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(horizontal = 16.dp),
                        ) {
                            TextButton(
                                {
                                    renamingId = null
                                    event(
                                        ProductsEvent.RenameCategory(c.id, renameValue.trim())
                                    )
                                    renameValue = ""
                                },
                                enabled =
                                    !state.busy && ProductRules.validCategoryName(renameValue.trim()),
                            ) {
                                Text(stringResource(R.string.pm_save))
                            }
                            TextButton(
                                {
                                    renamingId = null
                                    renameValue = ""
                                },
                                enabled = !state.busy,
                            ) {
                                Text(stringResource(R.string.pm_cancel))
                            }
                        }
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        ) {
                            Text(
                                c.name,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.weight(1f),
                            )
                            TextButton(
                                {
                                    renamingId = c.id
                                    renameValue = c.name
                                },
                                enabled = !state.busy,
                            ) {
                                Text(stringResource(R.string.pm_rename))
                            }
                            TextButton(
                                {
                                    confirmText = R.string.pm_confirm_category_delete
                                    confirm = { event(ProductsEvent.DeleteCategory(c.id)) }
                                },
                                enabled = !state.busy,
                            ) {
                                Text(stringResource(R.string.pm_delete))
                            }
                        }
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
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
                Button(
                    {
                        event(ProductsEvent.CreateCategory(categoryName.trim()))
                        categoryName = ""
                    },
                    enabled =
                        !state.busy &&
                            state.businessId != null &&
                            ProductRules.validCategoryName(categoryName.trim()),
                    shape = RoundedCornerShape(16.dp),
                    modifier =
                        Modifier.fillMaxWidth().height(52.dp).padding(horizontal = 16.dp),
                ) {
                    Text(stringResource(R.string.pm_add_category))
                }
            }
        }
    }
    stock?.let { variant ->
        StockDialog(variant, state.busy, { stock = null }) { quantity ->
            stock = null
            event(ProductsEvent.Stock(variant.id, quantity))
        }
    }
    if (confirm != null)
        AlertDialog(
            onDismissRequest = { confirm = null },
            title = { Text(stringResource(R.string.pm_confirm_title)) },
            text = { Text(stringResource(confirmText)) },
            confirmButton = {
                TextButton(
                    {
                        val action = confirm
                        confirm = null
                        action?.invoke()
                    },
                    enabled = !state.busy,
                ) {
                    Text(stringResource(confirmLabel(confirmText)))
                }
            },
            dismissButton = {
                TextButton({ confirm = null }) { Text(stringResource(R.string.pm_cancel)) }
            },
        )
}

/** Confirm dialogs carry their own message; the button follows from it. */
@StringRes
private fun confirmLabel(@StringRes message: Int): Int =
    when (message) {
        R.string.pm_confirm_deactivate -> R.string.pm_deactivate
        R.string.pm_confirm_reactivate -> R.string.pm_activate
        else -> R.string.pm_delete
    }

@Composable
private fun SheetRow(label: String, enabled: Boolean, onClick: () -> Unit) {
    Text(
        label,
        style = MaterialTheme.typography.bodyLarge,
        modifier =
            Modifier.fillMaxWidth()
                .clickable(enabled = enabled, onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 14.dp),
    )
}
