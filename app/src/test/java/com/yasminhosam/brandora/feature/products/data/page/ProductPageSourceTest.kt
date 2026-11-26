package com.yasminhosam.brandora.feature.products.data.page

import com.yasminhosam.brandora.core.network.*
import com.yasminhosam.brandora.feature.products.domain.*
import com.yasminhosam.brandora.feature.products.domain.model.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

private fun product(id: String, category: String? = null) =
    Product(id = id, businessId = "b", name = id, categoryId = category)

private fun page(
    items: List<Product>,
    offset: Int = 0,
    hasMore: Boolean = false,
    nextOffset: Int? = null,
    total: Int = items.size,
): ApiResult<Page<Product>> =
    ApiResult.Success(
        Page(
            items,
            PageInfo(
                limit = 20,
                offset = offset,
                totalCount = total,
                hasMore = hasMore,
                nextOffset = nextOffset,
            ),
        )
    )

class ProductPageSourceTest {
    private class FakeRepo : ProductsRepository {
        val listCalls = mutableListOf<Pair<Int, String?>>()
        val responses = mutableMapOf<Int, ApiResult<Page<Product>>>()

        private fun unused(): ApiResult.Error = ApiResult.Error(ApiErrorType.UNKNOWN)

        override suspend fun list(
            business: String,
            offset: Int,
            categoryId: String?,
        ): ApiResult<Page<Product>> {
            listCalls += offset to categoryId
            return responses[offset]
                ?: ApiResult.Success(Page(listOf(product("auto-$offset")), PageInfo(offset = offset)))
        }

        override suspend fun get(business: String, id: String): ApiResult<Product> = unused()

        override suspend fun save(
            business: String,
            id: String?,
            draft: ProductDraft,
        ): ApiResult<Product> = unused()

        override suspend fun setActive(
            business: String,
            id: String,
            active: Boolean,
        ): ApiResult<Product> = unused()

        override suspend fun categories(business: String, offset: Int): ApiResult<Page<Category>> =
            unused()

        override suspend fun createCategory(business: String, name: String): ApiResult<Category> =
            unused()

        override suspend fun renameCategory(
            business: String,
            id: String,
            name: String,
        ): ApiResult<Category> = unused()

        override suspend fun deleteCategory(business: String, id: String): ApiResult<Unit> =
            unused()

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
        ): ApiResult<Variant> = unused()

        override suspend fun upload(
            business: String,
            product: String,
            files: List<ImageUpload>,
        ): ApiResult<Images> = unused()

        override suspend fun main(
            business: String,
            product: String,
            image: String,
        ): ApiResult<Images> = unused()

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
        ): ApiResult<InventoryAdjustment> = unused()

        override suspend fun movements(
            business: String,
            product: String,
            variant: String,
            offset: Int,
        ): ApiResult<Page<StockMovement>> = unused()
    }

    @Test
    fun `fills from later backend pages until the target count is met`() = runBlocking {
        val fake = FakeRepo()
        fake.responses[0] =
            page(
                listOf(product("m1", "c1"), product("x", "c2")),
                offset = 0,
                hasMore = true,
                nextOffset = 20,
                total = 100,
            )
        fake.responses[20] =
            page(
                listOf(product("m2", "c1"), product("m3", "c1"), product("m4", "c1")),
                offset = 20,
                hasMore = true,
                nextOffset = 40,
                total = 100,
            )

        val result =
            FetchAllFilterPageSource(fake).page(
                "b",
                CategoryFilter.Category("c1"),
                offset = 0,
                targetCount = 4,
            )

        assertTrue(result is ApiResult.Success)
        val data = (result as ApiResult.Success).data
        assertEquals(listOf("m1", "m2", "m3", "m4"), data.items.map { it.id })
        assertEquals(listOf(0 to null, 20 to null), fake.listCalls)
        assertEquals(40, data.pagination.nextOffset)
    }

    @Test
    fun `keeps fetching while nothing matches and stops at the last page`() = runBlocking {
        val fake = FakeRepo()
        fake.responses[0] =
            page(listOf(product("x", "c2")), offset = 0, hasMore = true, nextOffset = 20)
        fake.responses[20] = page(listOf(product("y", "c2")), offset = 20, hasMore = false)

        val result =
            FetchAllFilterPageSource(fake).page("b", CategoryFilter.Category("c1"), targetCount = 5)

        assertTrue(result is ApiResult.Success)
        val data = (result as ApiResult.Success).data
        assertTrue(data.items.isEmpty())
        assertFalse(data.pagination.hasMore)
        assertEquals(listOf(0 to null, 20 to null), fake.listCalls)
    }

    @Test
    fun `all filter returns a full first page in a single call`() = runBlocking {
        val fake = FakeRepo()
        fake.responses[0] =
            page((1..20).map { product("p$it") }, offset = 0, hasMore = true, nextOffset = 20)

        val result =
            FetchAllFilterPageSource(fake).page("b", CategoryFilter.All, targetCount = 20)

        val data = (result as ApiResult.Success).data
        assertEquals(20, data.items.size)
        assertEquals(1, fake.listCalls.size)
        assertEquals(20, data.pagination.nextOffset)
    }

    @Test
    fun `an error from a later page is returned to the caller`() = runBlocking {
        val fake = FakeRepo()
        fake.responses[0] =
            page(listOf(product("m1", "c1")), offset = 0, hasMore = true, nextOffset = 20)
        fake.responses[20] = ApiResult.Error(ApiErrorType.BAD_REQUEST)

        val result =
            FetchAllFilterPageSource(fake).page("b", CategoryFilter.Category("c1"), targetCount = 5)

        assertTrue(result is ApiResult.Error)
        assertEquals(2, fake.listCalls.size)
    }

    @Test
    fun `load more resumes at the given backend offset`() = runBlocking {
        val fake = FakeRepo()
        fake.responses[40] = page(listOf(product("p40")), offset = 40)

        FetchAllFilterPageSource(fake).page("b", CategoryFilter.All, offset = 40)

        assertEquals(listOf(40 to null), fake.listCalls)
    }

    @Test
    fun `non advancing pagination stops the loop after one fetch`() = runBlocking {
        val fake = FakeRepo()
        fake.responses[0] =
            page(listOf(product("x", "c2")), offset = 0, hasMore = true, nextOffset = 0)

        FetchAllFilterPageSource(fake).page("b", CategoryFilter.Category("c1"))

        assertEquals(1, fake.listCalls.size)
    }

    @Test
    fun `duplicate ids across backend pages are dropped`() = runBlocking {
        val fake = FakeRepo()
        fake.responses[0] =
            page(
                listOf(product("dup", "c1"), product("a", "c1")),
                offset = 0,
                hasMore = true,
                nextOffset = 20,
            )
        fake.responses[20] =
            page(
                listOf(product("dup", "c1"), product("b", "c1")),
                offset = 20,
                hasMore = false,
            )

        val result =
            FetchAllFilterPageSource(fake).page("b", CategoryFilter.Category("c1"), targetCount = 3)

        val data = (result as ApiResult.Success).data
        assertEquals(listOf("dup", "a", "b"), data.items.map { it.id })
    }

    @Test
    fun `server side source forwards the category id and drops it for all`() = runBlocking {
        val fake = FakeRepo()
        val source = ServerCategoryParamPageSource(fake)

        source.page("b", CategoryFilter.Category("c9"), offset = 5)
        assertEquals(listOf(5 to "c9"), fake.listCalls)

        fake.listCalls.clear()
        source.page("b", CategoryFilter.All, offset = 0)
        assertEquals(listOf(0 to null), fake.listCalls)
    }
}
