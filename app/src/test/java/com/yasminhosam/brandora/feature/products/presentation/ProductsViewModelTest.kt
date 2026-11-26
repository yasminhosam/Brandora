package com.yasminhosam.brandora.feature.products.presentation

import com.yasminhosam.brandora.core.network.*
import com.yasminhosam.brandora.feature.business.domain.repository.CurrentBusinessStore
import com.yasminhosam.brandora.feature.products.data.page.FetchAllFilterPageSource
import com.yasminhosam.brandora.feature.products.domain.*
import com.yasminhosam.brandora.feature.products.domain.model.*
import com.yasminhosam.brandora.feature.products.domain.usecase.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class ProductsViewModelTest {
    private val scheduler = TestCoroutineScheduler()
    private val dispatcher = UnconfinedTestDispatcher(scheduler)

    private class Business(initial: String?) : CurrentBusinessStore {
        override val businessId = MutableStateFlow(initial)

        override suspend fun setBusinessId(id: String, ownerId: String?) {
            businessId.value = id
        }

        override suspend fun clearUnlessOwnedBy(userId: String) = error("not under test")
    }

    private class Reader : ProductImageReader {
        override suspend fun read(uris: List<String>) = uris.map {
            ImageUpload(byteArrayOf(1), "image/png")
        }
    }

    private class Fake : ProductsRepository {
        val calls = mutableListOf<String>()
        var gate: CompletableDeferred<Page<Product>>? = null
        var getGate: CompletableDeferred<Product>? = null
        var saveGate: CompletableDeferred<Product>? = null
        var saveError: ApiResult.Error? = null
        var getError: ApiResult.Error? = null
        var pages = mapOf<Int, Page<Product>>()
        var categoryItems: List<Category> = emptyList()

        private fun product(b: String) = Product(id = "p-" + b, businessId = b, name = "Product")

        private fun unused(): ApiResult.Error = ApiResult.Error(ApiErrorType.UNKNOWN)

        override suspend fun list(
            business: String,
            offset: Int,
            categoryId: String?,
        ): ApiResult<Page<Product>> {
            calls += "list:" + business + ":" + offset
            val wait = gate
            if (business == "a" && wait != null) return ApiResult.Success(wait.await())
            return ApiResult.Success(pages[offset] ?: Page(listOf(product(business)), PageInfo()))
        }

        override suspend fun get(business: String, id: String): ApiResult<Product> {
            calls += "get:" + business + ":" + id
            getError?.let {
                getError = null
                return it
            }
            val wait = getGate
            if (wait != null) return ApiResult.Success(wait.await())
            return ApiResult.Success(product(business).copy(id = id))
        }

        override suspend fun save(
            business: String,
            id: String?,
            draft: ProductDraft,
        ): ApiResult<Product> {
            calls += "save:" + business
            saveError?.let {
                saveError = null
                return it
            }
            return ApiResult.Success(saveGate?.await() ?: product(business))
        }

        override suspend fun setActive(
            business: String,
            id: String,
            active: Boolean,
        ): ApiResult<Product> {
            calls += "setActive:$business:$id:$active"
            return ApiResult.Success(product(business).copy(id = id, isActive = active))
        }

        override suspend fun categories(business: String, offset: Int): ApiResult<Page<Category>> =
            ApiResult.Success(Page(categoryItems, PageInfo()))

        override suspend fun createCategory(business: String, name: String): ApiResult<Category> =
            unused()

        override suspend fun renameCategory(
            business: String,
            id: String,
            name: String,
        ): ApiResult<Category> {
            calls += "renameCategory:$business:$id:$name"
            return ApiResult.Success(Category(id, name))
        }

        override suspend fun deleteCategory(business: String, id: String): ApiResult<Unit> {
            calls += "deleteCategory:$business:$id"
            return ApiResult.Success(Unit)
        }

        override suspend fun variants(
            business: String,
            product: String,
            offset: Int,
        ): ApiResult<Page<Variant>> = unused()

        override suspend fun saveVariant(
            business: String,
            product: String,
            id: String?,
            draft: VariantDraft,
        ): ApiResult<Variant> = unused()

        override suspend fun setVariantActive(
            business: String,
            product: String,
            variant: String,
            active: Boolean,
        ): ApiResult<Variant> {
            calls += "setVariantActive:$business:$product:$variant:$active"
            return ApiResult.Success(Variant(variant, product, business, isActive = active))
        }

        override suspend fun upload(
            business: String,
            product: String,
            files: List<ImageUpload>,
        ): ApiResult<Images> = unused()

        override suspend fun main(
            business: String,
            product: String,
            image: String,
        ): ApiResult<Images> {
            calls += "main:$business:$product:$image"
            return ApiResult.Success(Images(emptyList()))
        }

        override suspend fun order(
            business: String,
            product: String,
            ids: List<String>,
        ): ApiResult<Images> = unused()

        override suspend fun deleteImage(
            business: String,
            product: String,
            image: String,
        ): ApiResult<ImageDelete> = unused()

        override suspend fun adjust(
            business: String,
            product: String,
            variant: String,
            quantity: Int?,
        ): ApiResult<InventoryAdjustment> {
            calls += "adjust:$business:$product:$variant:$quantity"
            return ApiResult.Success(InventoryAdjustment(changed = true, quantityDelta = 0))
        }

        override suspend fun movements(
            business: String,
            product: String,
            variant: String,
            offset: Int,
        ): ApiResult<Page<StockMovement>> {
            calls += "movements:$business:$product:$variant:$offset"
            return ApiResult.Success(
                Page(
                    listOf(
                        StockMovement(
                            id = "m1",
                            type = "ADJUSTMENT",
                            quantityBefore = 8,
                            quantityAfter = 12,
                            quantityDelta = 4,
                            createdAt = "2026-11-08T10:00:00Z",
                        )
                    ),
                    PageInfo(),
                )
            )
        }
    }

    private fun vm(f: Fake, store: CurrentBusinessStore) =
        ProductsViewModel(
            ProductUseCases(f, FetchAllFilterPageSource(f)),
            CategoryUseCases(f),
            VariantUseCases(f),
            ImageUseCases(f, Reader()),
            SaveProductUseCase(f),
            SetVariantStockUseCase(f),
            store,
        )

    @Before
    fun before() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun after() {
        Dispatchers.resetMain()
    }

    @Test
    fun `no business never generates a placeholder or API call`() =
        runTest(scheduler) {
            val f = Fake()
            val v = vm(f, Business(null))
            v.onEvent(ProductsEvent.Refresh)
            v.onEvent(ProductsEvent.Save(null, ProductDraft(name = "Manual")))
            assertTrue(f.calls.isEmpty())
            assertNull(v.uiState.value.businessId)
        }

    @Test
    fun `business switch cancels in flight reads and clears old detail`() =
        runTest(scheduler) {
            val f = Fake()
            val gate = CompletableDeferred<Page<Product>>()
            f.gate = gate
            val store = Business("a")
            val v = vm(f, store)
            store.setBusinessId("b", "u1")
            runCurrent()
            assertEquals("b", v.uiState.value.businessId)
            assertEquals(listOf("p-b"), v.uiState.value.items.map { it.id })
            gate.complete(Page(listOf(Product("old", "a", "Old")), PageInfo()))
            runCurrent()
            assertEquals(listOf("p-b"), v.uiState.value.items.map { it.id })
        }

    @Test
    fun `manual save uses persisted business and duplicate taps do not repeat writes`() =
        runTest(scheduler) {
            val f = Fake()
            val v = vm(f, Business("b"))
            f.saveGate = CompletableDeferred()
            val event =
                ProductsEvent.Save(
                    null,
                    ProductDraft(name = "Manual", externalKey = "IMPORT-1"),
                )
            v.onEvent(event)
            v.onEvent(event)
            assertEquals(1, f.calls.count { it == "save:b" })
            f.saveGate!!.complete(Product("new", "b", "Manual"))
            assertEquals("new", v.uiState.value.selected?.id)
        }

    @Test
    fun `pagination uses nextOffset and deduplicates stable product IDs`() =
        runTest(scheduler) {
            val f = Fake()
            val first = (1..20).map { Product("p$it", "b", "P$it") }
            val two = Product("two", "b", "Two")
            f.pages =
                mapOf(
                    0 to Page(first, PageInfo(hasMore = true, nextOffset = 20)),
                    20 to Page(listOf(first[19], two), PageInfo(offset = 20)),
                )
            val v = vm(f, Business("b"))
            v.onEvent(ProductsEvent.More)
            assertEquals(first.map { it.id } + "two", v.uiState.value.items.map { it.id })
            assertTrue(f.calls.contains("list:b:20"))
        }

    @Test
    fun `retry refreshes then re-dispatches the failed event exactly once`() =
        runTest(scheduler) {
            val f = Fake()
            val v = vm(f, Business("b"))
            f.saveError =
                ApiResult.Error(ApiErrorType.CONFLICT, errorCode = "PRODUCT_WRITE_CONFLICT")
            v.onEvent(ProductsEvent.Save(null, ProductDraft(name = "Manual")))
            assertEquals("PRODUCT_WRITE_CONFLICT", v.uiState.value.error?.errorCode)

            f.saveError = null
            v.onEvent(ProductsEvent.Retry)
            // One extra save (the re-dispatch) and one extra list read (the refresh first).
            assertEquals(2, f.calls.count { it == "save:b" })
            assertEquals(2, f.calls.count { it == "list:b:0" })
            assertNull(v.uiState.value.error)

            // The latch is cleared by the successful retry: Retry alone is a no-op now.
            v.onEvent(ProductsEvent.Retry)
            assertEquals(2, f.calls.count { it == "save:b" })
        }

    @Test
    fun `retry after a committed mutation only refreshes when the follow-up read failed`() =
        runTest(scheduler) {
            val f = Fake()
            val v = vm(f, Business("b"))
            v.onEvent(ProductsEvent.Open("p-b"))
            assertEquals("p-b", v.uiState.value.selected?.id)

            // The image is already set; only the product reload that follows it fails.
            f.getError = ApiResult.Error(ApiErrorType.TIMEOUT)
            v.onEvent(ProductsEvent.Main("img1"))
            assertEquals(1, f.calls.count { it == "main:b:p-b:img1" })
            assertNotNull(v.uiState.value.error)

            // Retry must read the product again without re-sending the committed PATCH.
            v.onEvent(ProductsEvent.Retry)
            assertEquals(1, f.calls.count { it == "main:b:p-b:img1" })
            assertEquals(3, f.calls.count { it == "get:b:p-b" })
            assertNull(v.uiState.value.error)
        }

    @Test
    fun `category filter narrows browse results and clears back to all`() =
        runTest(scheduler) {
            val f = Fake()
            val dates = Product("p1", "b", "Dates", categoryId = "c1")
            val nuts = Product("p2", "b", "Nuts", categoryId = "c2")
            f.pages = mapOf(0 to Page(listOf(dates, nuts), PageInfo()))
            val v = vm(f, Business("b"))
            assertEquals(listOf("p1", "p2"), v.uiState.value.items.map { it.id })

            v.onEvent(ProductsEvent.Filter("c1"))
            assertEquals(CategoryFilter.Category("c1"), v.uiState.value.filter)
            assertEquals(listOf("p1"), v.uiState.value.items.map { it.id })

            v.onEvent(ProductsEvent.Filter(null))
            assertEquals(CategoryFilter.All, v.uiState.value.filter)
            assertEquals(listOf("p1", "p2"), v.uiState.value.items.map { it.id })
        }

    @Test
    fun `load more keeps the active category filter`() =
        runTest(scheduler) {
            val f = Fake()
            val first = (1..20).map { Product("a$it", "b", "A$it", categoryId = "c1") }
            val other = Product("x", "b", "Other", categoryId = "c2")
            val next = Product("a21", "b", "A21", categoryId = "c1")
            f.pages =
                mapOf(
                    0 to Page(first + other, PageInfo(hasMore = true, nextOffset = 20)),
                    20 to
                        Page(
                            listOf(other.copy(id = "x2"), next),
                            PageInfo(offset = 20, hasMore = true, nextOffset = 40),
                        ),
                )
            val v = vm(f, Business("b"))
            v.onEvent(ProductsEvent.Filter("c1"))
            assertEquals(first.map { it.id }, v.uiState.value.items.map { it.id })
            assertTrue(v.uiState.value.pagination.hasMore)

            v.onEvent(ProductsEvent.More)
            assertEquals(first.map { it.id } + "a21", v.uiState.value.items.map { it.id })
            assertEquals(CategoryFilter.Category("c1"), v.uiState.value.filter)
            // The filtered fill resumes at the backend offset instead of restarting at zero.
            assertEquals(2, f.calls.count { it == "list:b:0" })
            assertEquals(1, f.calls.count { it == "list:b:20" })
            assertEquals(1, f.calls.count { it == "list:b:40" })
        }

    @Test
    fun `stale detail response never lands after a business switch`() =
        runTest(scheduler) {
            val f = Fake()
            val gate = CompletableDeferred<Product>()
            f.getGate = gate
            val store = Business("a")
            val v = vm(f, store)
            v.onEvent(ProductsEvent.Open("p1"))
            // The detail read for business "a" is in flight when the user switches away.
            store.setBusinessId("b", "u1")
            runCurrent()
            gate.complete(Product("p-stale", "a", "Stale"))
            runCurrent()
            assertNull(v.uiState.value.selected)
            assertEquals(listOf("p-b"), v.uiState.value.items.map { it.id })
            assertFalse(v.uiState.value.busy)
        }

    @Test
    fun `product deactivate flows through the active flag instead of a delete`() =
        runTest(scheduler) {
            val f = Fake()
            val v = vm(f, Business("b"))
            v.onEvent(ProductsEvent.Open("p1"))
            v.onEvent(ProductsEvent.Active(false))
            assertEquals(
                listOf("list:b:0", "get:b:p1", "setActive:b:p1:false"),
                f.calls,
            )
            assertEquals(false, v.uiState.value.selected?.isActive)
        }

    @Test
    fun `variant toggle sends a state only patch and refreshes the product`() =
        runTest(scheduler) {
            val f = Fake()
            val v = vm(f, Business("b"))
            v.onEvent(ProductsEvent.Open("p1"))
            v.onEvent(ProductsEvent.VariantActive("v1", false))
            assertEquals(
                listOf(
                    "list:b:0",
                    "get:b:p1",
                    "setVariantActive:b:p1:v1:false",
                    "get:b:p1",
                ),
                f.calls,
            )
        }

    @Test
    fun `category rename rewrites the row and delete removes it`() =
        runTest(scheduler) {
            val f = Fake()
            f.categoryItems = listOf(Category("c1", "Old"), Category("c2", "Other"))
            val v = vm(f, Business("b"))
            v.onEvent(ProductsEvent.Categories)
            assertEquals(listOf("Old", "Other"), v.uiState.value.categories.map { it.name })

            v.onEvent(ProductsEvent.RenameCategory("c1", "Renamed"))
            assertEquals(listOf("Renamed", "Other"), v.uiState.value.categories.map { it.name })

            v.onEvent(ProductsEvent.DeleteCategory("c1"))
            assertEquals(listOf("Other"), v.uiState.value.categories.map { it.name })
            assertEquals(
                listOf("renameCategory:b:c1:Renamed", "deleteCategory:b:c1"),
                f.calls.filter { it.startsWith("renameCategory") || it.startsWith("deleteCategory") },
            )
            assertNull(v.uiState.value.error)
        }

    @Test
    fun `back closes the inventory screen before leaving the product`() =
        runTest(scheduler) {
            val f = Fake()
            val v = vm(f, Business("b"))
            v.onEvent(ProductsEvent.Open("p1"))
            v.onEvent(ProductsEvent.History("v1"))
            assertEquals("v1", v.uiState.value.historyVariant)
            assertEquals(1, v.uiState.value.movements.size)

            v.onEvent(ProductsEvent.Back)
            assertNotNull(v.uiState.value.selected)
            assertNull(v.uiState.value.historyVariant)
            assertTrue(v.uiState.value.movements.isEmpty())

            v.onEvent(ProductsEvent.Back)
            assertNull(v.uiState.value.selected)
        }

    @Test
    fun `quantity adjustment keeps inventory open and reloads its movements`() =
        runTest(scheduler) {
            val f = Fake()
            val v = vm(f, Business("b"))
            v.onEvent(ProductsEvent.Open("p1"))
            v.onEvent(ProductsEvent.History("v1"))
            v.onEvent(ProductsEvent.Stock("v1", 42))
            assertEquals("v1", v.uiState.value.historyVariant)
            assertNull(v.uiState.value.error)
            assertEquals(2, f.calls.count { it.startsWith("movements") })
            assertEquals(
                listOf("adjust:b:p1:v1:42"),
                f.calls.filter { it.startsWith("adjust") },
            )
        }
}
