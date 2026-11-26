 package com.yasminhosam.brandora.feature.products.presentation

import android.net.Uri
import androidx.lifecycle.viewModelScope
import com.yasminhosam.brandora.core.base.*
import com.yasminhosam.brandora.core.network.*
import com.yasminhosam.brandora.feature.business.domain.repository.CurrentBusinessStore
import com.yasminhosam.brandora.feature.products.domain.CategoryFilter
import com.yasminhosam.brandora.feature.products.domain.model.*
import com.yasminhosam.brandora.feature.products.domain.usecase.*
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.distinctUntilChanged

data class ProductsState(
    val businessId: String? = null,
    val filter: CategoryFilter = CategoryFilter.All,
    val items: List<Product> = emptyList(),
    val pagination: PageInfo = PageInfo(),
    val selected: Product? = null,
    val categories: List<Category> = emptyList(),
    val categoryPage: PageInfo = PageInfo(),
    val categoriesLoaded: Boolean = false,
    val movements: List<StockMovement> = emptyList(),
    val movementPage: PageInfo = PageInfo(),
    val historyVariant: String? = null,
    val busy: Boolean = false,
    val error: ApiResult.Error? = null,
    val cleanupPending: Boolean = false,
    val pendingImageIds: Set<String> = emptySet(),
) : UiState

sealed interface ProductsEvent : UiEvent {
    data object Refresh : ProductsEvent

    data object More : ProductsEvent

    /** Browse category filter: null shows all categories. */
    data class Filter(val categoryId: String?) : ProductsEvent

    data object Back : ProductsEvent

    /** Refresh the current screen, then re-run the event that last failed. */
    data object Retry : ProductsEvent

    data class Open(val id: String) : ProductsEvent

    data class Save(val id: String?, val draft: ProductDraft) : ProductsEvent

    data class Active(val active: Boolean) : ProductsEvent

    data object Categories : ProductsEvent

    data object MoreCategories : ProductsEvent

    data class CreateCategory(val name: String) : ProductsEvent

    data class RenameCategory(val id: String, val name: String) : ProductsEvent

    data class DeleteCategory(val id: String) : ProductsEvent

    data class SaveVariant(val id: String?, val draft: VariantDraft) : ProductsEvent

    /** Variant activate/deactivate: a state-only PATCH, never a DELETE. */
    data class VariantActive(val variant: String, val active: Boolean) : ProductsEvent

    data object MoreVariants : ProductsEvent

    data class Upload(val uris: List<Uri>) : ProductsEvent

    data class Main(val id: String) : ProductsEvent

    data class Move(val index: Int, val to: Int) : ProductsEvent

    data class DeleteImage(val id: String) : ProductsEvent

    data class Stock(val variant: String, val quantity: Int?) : ProductsEvent

    data class History(val variant: String) : ProductsEvent

    data object MoreHistory : ProductsEvent
}

sealed interface ProductsEffect : UiEffect {
    data object Saved : ProductsEffect
}

@HiltViewModel
class ProductsViewModel
@Inject
constructor(
    private val products: ProductUseCases,
    private val categories: CategoryUseCases,
    private val variants: VariantUseCases,
    private val images: ImageUseCases,
    private val saveProduct: SaveProductUseCase,
    private val setStock: SetVariantStockUseCase,
    private val businessStore: CurrentBusinessStore,
) : BaseViewModel<ProductsState, ProductsEvent, ProductsEffect>(ProductsState()) {
    private var work: Job? = null

    /** Last dispatched event, kept while its run left an error so Retry can re-run it. */
    private var failedEvent: ProductsEvent? = null

    init {
        viewModelScope.launch {
            businessStore.businessId.distinctUntilChanged().collect { id ->
                work?.cancelAndJoin()
                failedEvent = null
                setState { ProductsState(businessId = id) }
                if (id != null) onEvent(ProductsEvent.Refresh)
            }
        }
    }

    private fun run(event: ProductsEvent, block: suspend (String) -> Unit) {
        val state = uiState.value
        if (state.busy) return
        val business = state.businessId ?: return
        failedEvent = event
        setState { copy(busy = true, error = null, cleanupPending = false) }
        work = viewModelScope.launch {
            try {
                block(business)
            } finally {
                if (uiState.value.businessId == business) {
                    if (uiState.value.error == null) failedEvent = null
                    setState { copy(busy = false) }
                } else {
                    failedEvent = null
                }
            }
        }
    }

    private suspend fun <T> use(result: ApiResult<T>, success: suspend (T) -> Unit) {
        when (result) {
            is ApiResult.Success -> success(result.data)
            is ApiResult.Error -> setState { copy(error = result) }
        }
    }

    private suspend fun refreshProduct(business: String, id: String) {
        use(products.load(business, id)) { p ->
            setState {
                // Keep an open inventory screen across refreshes of the same product; opening
                // a different product or switching business still clears it.
                val keepInventory = historyVariant != null && selected?.id == p.id
                copy(
                    selected = p,
                    pendingImageIds = if (selected?.id == p.id) pendingImageIds else emptySet(),
                    historyVariant = if (keepInventory) historyVariant else null,
                    movements = if (keepInventory) movements else emptyList(),
                    movementPage = if (keepInventory) movementPage else PageInfo(),
                )
            }
        }
    }

    private suspend fun refreshList(
        business: String,
        filter: CategoryFilter = uiState.value.filter,
    ) {
        use(products.browse(business, filter)) { page ->
            setState {
                copy(
                    filter = filter,
                    items = page.items,
                    pagination = page.pagination,
                )
            }
        }
    }

    private suspend fun mutateProduct(business: String, call: suspend (Product) -> ApiResult<*>) {
        val p = uiState.value.selected ?: return
        val result = call(p)
        if (result is ApiResult.Error) setState { copy(error = result) }
        refreshProduct(business, p.id)
    }

    override fun onEvent(event: ProductsEvent) {
        when (event) {
            ProductsEvent.Back -> {
                if (!uiState.value.busy) {
                    val s = uiState.value
                    when {
                        // Back walks the stack: inventory screen first, then the detail.
                        s.historyVariant != null ->
                            setState {
                                copy(
                                    historyVariant = null,
                                    movements = emptyList(),
                                    movementPage = PageInfo(),
                                )
                            }
                        s.selected != null -> {
                            setState {
                                copy(
                                    selected = null,
                                    historyVariant = null,
                                    movements = emptyList(),
                                    movementPage = PageInfo(),
                                )
                            }
                            onEvent(ProductsEvent.Refresh)
                        }
                    }
                }
                return
            }
            ProductsEvent.Retry -> {
                val failed = failedEvent ?: return
                run(failed) { b ->
                    val p = uiState.value.selected
                    if (p == null) refreshList(b) else refreshProduct(b, p.id)
                    dispatch(b, failed)
                }
                return
            }
            else -> Unit
        }
        run(event) { b -> dispatch(b, event) }
    }

    private suspend fun dispatch(b: String, event: ProductsEvent) {
        when (event) {
            ProductsEvent.Refresh -> {
                val p = uiState.value.selected
                if (p == null) refreshList(b) else refreshProduct(b, p.id)
            }
            is ProductsEvent.Filter ->
                refreshList(
                    b,
                    if (event.categoryId == null) CategoryFilter.All
                    else CategoryFilter.Category(event.categoryId),
                )
            ProductsEvent.More -> {
                val offset = uiState.value.pagination.nextOffset
                if (offset != null)
                    use(products.browse(b, uiState.value.filter, offset = offset)) { page ->
                        setState {
                            copy(
                                items = (items + page.items).distinctBy { it.id },
                                pagination = page.pagination,
                            )
                        }
                    }
            }
            is ProductsEvent.Open -> refreshProduct(b, event.id)
            is ProductsEvent.Save ->
                use(saveProduct(b, event.id, event.draft)) { p ->
                    setState { copy(selected = p) }
                    sendEffect(ProductsEffect.Saved)
                }
            is ProductsEvent.Active -> {
                val p = uiState.value.selected ?: return
                use(products.setActive(b, p.id, event.active)) { updated ->
                    setState { copy(selected = updated) }
                }
            }
            ProductsEvent.Categories ->
                use(categories.list(b)) { page ->
                    setState {
                        copy(
                            categories = page.items,
                            categoryPage = page.pagination,
                            categoriesLoaded = true,
                        )
                    }
                }
            ProductsEvent.MoreCategories -> {
                val offset = uiState.value.categoryPage.nextOffset
                if (offset != null)
                    use(categories.list(b, offset)) { page ->
                        setState {
                            copy(
                                categories = (categories + page.items).distinctBy { it.id },
                                categoryPage = page.pagination,
                            )
                        }
                    }
            }
            is ProductsEvent.CreateCategory ->
                use(categories.create(b, event.name)) { c ->
                    setState { copy(categories = (categories + c).distinctBy { it.id }) }
                }
            is ProductsEvent.RenameCategory ->
                use(categories.rename(b, event.id, event.name)) { updated ->
                    setState {
                        copy(categories = categories.map { if (it.id == updated.id) updated else it })
                    }
                }
            is ProductsEvent.DeleteCategory ->
                use(categories.delete(b, event.id)) {
                    setState { copy(categories = categories.filterNot { it.id == event.id }) }
                }
            is ProductsEvent.SaveVariant -> {
                val p = uiState.value.selected ?: return
                use(variants.save(b, p, event.id, event.draft)) {
                    refreshProduct(b, p.id)
                    sendEffect(ProductsEffect.Saved)
                }
            }
            is ProductsEvent.VariantActive ->
                mutateProduct(b) { p -> variants.setActive(b, p.id, event.variant, event.active) }
            ProductsEvent.MoreVariants -> {
                val p = uiState.value.selected ?: return
                val offset = p.variantsPagination.nextOffset
                if (offset != null)
                    use(variants.page(b, p.id, offset)) { page ->
                        setState {
                            copy(
                                selected =
                                    selected?.copy(
                                        variants =
                                            (p.variants + page.items).distinctBy { it.id },
                                        variantsPagination = page.pagination,
                                    )
                            )
                        }
                    }
            }
            is ProductsEvent.Upload -> {
                val p = uiState.value.selected ?: return
                val r = images.upload(b, p.id, event.uris.map { it.toString() }, p.images.size)
                if (r is ApiResult.Error) setState { copy(error = r) }
                if (r is ApiResult.Success) setState { copy(cleanupPending = r.data.cleanupPending) }
                refreshProduct(b, p.id)
            }
            is ProductsEvent.Main ->
                mutateProduct(b) { p -> images.setMain(b, p.id, event.id) }
            is ProductsEvent.Move -> {
                val p = uiState.value.selected ?: return
                val r = images.reorder(b, p.id, p.images.map { it.id }, event.index, event.to)
                    ?: return
                if (r is ApiResult.Error) setState { copy(error = r) }
                refreshProduct(b, p.id)
            }
            is ProductsEvent.DeleteImage -> {
                val p = uiState.value.selected ?: return
                val r = images.delete(b, p.id, event.id)
                if (r is ApiResult.Error) setState { copy(error = r) }
                if (r is ApiResult.Success)
                    setState {
                        copy(
                            cleanupPending = r.data.cleanupPending,
                            pendingImageIds =
                                if (r.data.cleanupPending) pendingImageIds + event.id
                                else pendingImageIds - event.id,
                        )
                    }
                refreshProduct(b, p.id)
            }
            is ProductsEvent.Stock -> {
                mutateProduct(b) { p -> setStock(b, p.id, event.variant, event.quantity) }
                // The quantity dialog lives on the inventory screen, which refreshProduct kept
                // open for this product — reload its movements to include the new entry.
                val p = uiState.value.selected
                if (p != null && uiState.value.historyVariant == event.variant)
                    use(variants.movements(b, p.id, event.variant, 0)) { page ->
                        setState { copy(movements = page.items, movementPage = page.pagination) }
                    }
            }
            is ProductsEvent.History -> {
                val p = uiState.value.selected ?: return
                use(variants.movements(b, p.id, event.variant, 0)) { page ->
                    setState {
                        copy(
                            historyVariant = event.variant,
                            movements = page.items,
                            movementPage = page.pagination,
                        )
                    }
                }
            }
            ProductsEvent.MoreHistory -> {
                val p = uiState.value.selected ?: return
                val v = uiState.value.historyVariant ?: return
                val offset = uiState.value.movementPage.nextOffset
                if (offset != null)
                    use(variants.movements(b, p.id, v, offset)) { page ->
                        setState {
                            copy(
                                movements = (movements + page.items).distinctBy { it.id },
                                movementPage = page.pagination,
                            )
                        }
                    }
            }
            ProductsEvent.Back,
            ProductsEvent.Retry -> Unit
        }
    }
}
