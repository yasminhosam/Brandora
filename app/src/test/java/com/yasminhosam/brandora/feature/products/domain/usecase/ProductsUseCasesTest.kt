package com.yasminhosam.brandora.feature.products.domain.usecase

import com.yasminhosam.brandora.core.network.*
import com.yasminhosam.brandora.feature.products.domain.*
import com.yasminhosam.brandora.feature.products.domain.model.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

/**
 * Covers the products use-case layer: the grouped resource use cases (products, categories,
 * variants, images) plus the two rule-bearing standalone ones (SaveProductUseCase,
 * SetVariantStockUseCase). One fake repository backs them all.
 */
class ProductsUseCasesTest {
    private class Fake : ProductsRepository {
        val calls = mutableListOf<String>()
        var lastDraft: ProductDraft? = null
        var lastVariantDraft: VariantDraft? = null
        var uploaded: List<ImageUpload> = emptyList()

        private fun product() = Product(id = "p1", businessId = "b", name = "P")

        private fun unused(): ApiResult.Error = ApiResult.Error(ApiErrorType.UNKNOWN)

        override suspend fun list(
            business: String,
            offset: Int,
            categoryId: String?,
        ): ApiResult<Page<Product>> {
            calls += "list:$business:$offset:$categoryId"
            return ApiResult.Success(Page(emptyList(), PageInfo()))
        }

        override suspend fun get(business: String, id: String): ApiResult<Product> {
            calls += "get:$business:$id"
            return ApiResult.Success(product().copy(id = id))
        }

        override suspend fun save(
            business: String,
            id: String?,
            draft: ProductDraft,
        ): ApiResult<Product> {
            calls += "save:$business:$id"
            lastDraft = draft
            return ApiResult.Success(product().copy(id = id ?: "new", name = draft.name))
        }

        override suspend fun setActive(
            business: String,
            id: String,
            active: Boolean,
        ): ApiResult<Product> {
            calls += "setActive:$business:$id:$active"
            return ApiResult.Success(product().copy(id = id, isActive = active))
        }

        override suspend fun categories(business: String, offset: Int): ApiResult<Page<Category>> {
            calls += "categories:$business:$offset"
            return ApiResult.Success(Page(listOf(Category("c1", "General")), PageInfo()))
        }

        override suspend fun createCategory(business: String, name: String): ApiResult<Category> {
            calls += "createCategory:$business:$name"
            return ApiResult.Success(Category("c1", name))
        }

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
        ): ApiResult<Page<Variant>> {
            calls += "variants:$business:$product:$offset"
            return ApiResult.Success(Page(listOf(Variant("v1", product, business)), PageInfo()))
        }

        override suspend fun saveVariant(
            business: String,
            product: String,
            id: String?,
            draft: VariantDraft,
        ): ApiResult<Variant> {
            calls += "saveVariant:$business:$product:$id"
            lastVariantDraft = draft
            return ApiResult.Success(Variant(id ?: "v1", product, business))
        }

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
        ): ApiResult<Images> {
            calls += "upload:$business:$product"
            uploaded = files
            return ApiResult.Success(Images(emptyList(), cleanupPending = true))
        }

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
        ): ApiResult<Images> {
            calls += "order:$business:$product:${ids.joinToString(",")}"
            return ApiResult.Success(Images(emptyList()))
        }

        override suspend fun deleteImage(
            business: String,
            product: String,
            image: String,
        ): ApiResult<ImageDelete> {
            calls += "deleteImage:$business:$product:$image"
            return ApiResult.Success(ImageDelete(deleted = true, cleanupPending = true))
        }

        override suspend fun adjust(
            business: String,
            product: String,
            variant: String,
            quantity: Int?,
        ): ApiResult<InventoryAdjustment> {
            calls += "adjust:$business:$product:$variant:$quantity"
            return ApiResult.Success(
                InventoryAdjustment(changed = true, quantityDelta = quantity ?: 0)
            )
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
                            type = "MANUAL_ADJUSTMENT",
                            quantityDelta = 1,
                            createdAt = "2026-01-01T00:00:00.000Z",
                        )
                    ),
                    PageInfo(),
                )
            )
        }
    }

    private class FakeReader(private val fail: Boolean = false) : ProductImageReader {
        var readCount = 0

        override suspend fun read(uris: List<String>): List<ImageUpload> {
            readCount++
            if (fail) error("cannot read")
            return uris.map { ImageUpload(byteArrayOf(1), "image/png") }
        }
    }

    private class FakePages : ProductPageSource {
        var last: Triple<String, CategoryFilter, Int>? = null

        override suspend fun page(
            business: String,
            filter: CategoryFilter,
            offset: Int,
            targetCount: Int,
        ): ApiResult<Page<Product>> {
            last = Triple(business, filter, offset)
            return ApiResult.Success(Page(listOf(Product("p1", "b", "P")), PageInfo(nextOffset = 20)))
        }
    }

    private fun errorCode(result: ApiResult<*>) = (result as ApiResult.Error).errorCode

    // ---- ProductUseCases ----

    @Test
    fun `browse routes through the page source seam with filter and offset`() = runBlocking {
        val f = Fake()
        val pages = FakePages()
        ProductUseCases(f, pages).browse("b", CategoryFilter.Category("c9"), offset = 40)
        assertEquals(Triple("b", CategoryFilter.Category("c9"), 40), pages.last)
        assertTrue(f.calls.isEmpty())
    }

    @Test
    fun `load and setActive hit the repository`() = runBlocking {
        val f = Fake()
        val productUse = ProductUseCases(f, FakePages())
        productUse.load("b", "p1")
        productUse.setActive("b", "p1", active = false)
        assertEquals(listOf("get:b:p1", "setActive:b:p1:false"), f.calls)
    }

    // ---- CategoryUseCases ----

    @Test
    fun `category create and rename trim the name before writing`() = runBlocking {
        val f = Fake()
        val categoryUse = CategoryUseCases(f)
        categoryUse.create("b", "  General Goods  ")
        categoryUse.rename("b", "c1", " Toys ")
        assertEquals(
            listOf("createCategory:b:General Goods", "renameCategory:b:c1:Toys"),
            f.calls,
        )
    }

    @Test
    fun `category names that are blank or overlong fail as VALIDATION_ERROR`() = runBlocking {
        val f = Fake()
        val categoryUse = CategoryUseCases(f)
        assertEquals("VALIDATION_ERROR", errorCode(categoryUse.create("b", "   ")))
        assertEquals("VALIDATION_ERROR", errorCode(categoryUse.create("b", "x".repeat(201))))
        assertEquals("VALIDATION_ERROR", errorCode(categoryUse.rename("b", "c1", "x".repeat(201))))
        assertTrue(f.calls.isEmpty())
    }

    @Test
    fun `category list and delete delegate to the repository`() = runBlocking {
        val f = Fake()
        val categoryUse = CategoryUseCases(f)
        categoryUse.list("b", 20)
        categoryUse.delete("b", "c1")
        assertEquals(listOf("categories:b:20", "deleteCategory:b:c1"), f.calls)
    }

    // ---- VariantUseCases ----

    private fun configurable() =
        Product(
            id = "p1",
            businessId = "b",
            name = "P",
            options = mapOf("Color" to listOf("Red", "Blue")),
        )

    @Test
    fun `variant save rejects a selection that does not cover the product options`() =
        runBlocking {
            val f = Fake()
            val variantUse = VariantUseCases(f)
            assertEquals(
                "VARIANT_OPTIONS_INVALID",
                errorCode(variantUse.save("b", configurable(), null, VariantDraft())),
            )
            assertEquals(
                "VARIANT_OPTIONS_INVALID",
                errorCode(
                    variantUse.save(
                        "b",
                        configurable(),
                        "v1",
                        VariantDraft(optionValues = mapOf("Color" to "Green")),
                    )
                ),
            )
            assertTrue(f.calls.isEmpty())
        }

    @Test
    fun `variant save rejects bad money and overlong sku as VALIDATION_ERROR`() = runBlocking {
        val f = Fake()
        val variantUse = VariantUseCases(f)
        val selection = mapOf("Color" to "Red")
        assertEquals(
            "VALIDATION_ERROR",
            errorCode(
                variantUse.save(
                    "b",
                    configurable(),
                    "v1",
                    VariantDraft(unitPrice = "1.234", optionValues = selection),
                )
            ),
        )
        assertEquals(
            "VALIDATION_ERROR",
            errorCode(
                variantUse.save(
                    "b",
                    configurable(),
                    "v1",
                    VariantDraft(sku = "x".repeat(201), optionValues = selection),
                )
            ),
        )
        assertTrue(f.calls.isEmpty())
    }

    @Test
    fun `variant save writes a matching draft and paging delegates`() = runBlocking {
        val f = Fake()
        val variantUse = VariantUseCases(f)
        val draft =
            VariantDraft(sku = "S-1", unitPrice = "9.99", optionValues = mapOf("Color" to "Red"))
        val result = variantUse.save("b", configurable(), null, draft)
        assertTrue(result is ApiResult.Success)
        assertEquals(draft, f.lastVariantDraft)

        variantUse.page("b", "p1", 40)
        variantUse.movements("b", "p1", "v1", 20)
        variantUse.setActive("b", "p1", "v1", active = false)
        assertEquals(
            listOf(
                "saveVariant:b:p1:null",
                "variants:b:p1:40",
                "movements:b:p1:v1:20",
                "setVariantActive:b:p1:v1:false",
            ),
            f.calls,
        )
    }

    // ---- ImageUseCases ----

    @Test
    fun `upload rejects an oversized batch before reading files`() = runBlocking {
        val f = Fake()
        val reader = FakeReader()
        val result = ImageUseCases(f, reader).upload("b", "p1", listOf("u1", "u2"), existing = 4)
        assertEquals("PRODUCT_IMAGE_INVALID", errorCode(result))
        assertEquals(0, reader.readCount)
        assertTrue(f.calls.isEmpty())
    }

    @Test
    fun `upload maps file read failures to PRODUCT_IMAGE_INVALID`() = runBlocking {
        val reader = FakeReader(fail = true)
        val result = ImageUseCases(Fake(), reader).upload("b", "p1", listOf("u1"), existing = 0)
        assertEquals("PRODUCT_IMAGE_INVALID", errorCode(result))
    }

    @Test
    fun `upload reads and uploads when the batch fits`() = runBlocking {
        val f = Fake()
        val reader = FakeReader()
        val result = ImageUseCases(f, reader).upload("b", "p1", listOf("u1"), existing = 3)
        assertTrue(result is ApiResult.Success)
        assertEquals(1, reader.readCount)
        assertEquals(1, f.uploaded.size)
        assertEquals(listOf("upload:b:p1"), f.calls)
    }

    @Test
    fun `reorder ignores out of range moves and sends the new order otherwise`() = runBlocking {
        val f = Fake()
        val imageUse = ImageUseCases(f, FakeReader())
        val ids = listOf("i1", "i2", "i3")
        assertNull(imageUse.reorder("b", "p1", ids, from = 0, to = 5))
        assertTrue(f.calls.isEmpty())
        val result = imageUse.reorder("b", "p1", ids, from = 0, to = 1)
        assertTrue(result is ApiResult.Success)
        assertEquals(listOf("order:b:p1:i2,i1,i3"), f.calls)
    }

    @Test
    fun `setMain and deleteImage delegate to the repository`() = runBlocking {
        val f = Fake()
        val imageUse = ImageUseCases(f, FakeReader())
        imageUse.setMain("b", "p1", "i1")
        imageUse.delete("b", "p1", "i1")
        assertEquals(listOf("main:b:p1:i1", "deleteImage:b:p1:i1"), f.calls)
    }

    // ---- SaveProductUseCase ----

    @Test
    fun `product save rejects invalid drafts as VALIDATION_ERROR without writing`() =
        runBlocking {
            val f = Fake()
            val save = SaveProductUseCase(f)
            val invalid =
                listOf(
                    ProductDraft(name = "   "),
                    ProductDraft(name = "x".repeat(201)),
                    ProductDraft(name = "M", description = "d".repeat(5001)),
                    ProductDraft(name = "M", externalKey = "e".repeat(201)),
                    ProductDraft(name = "M", basePrice = "1.234"),
                    ProductDraft(name = "M", options = mapOf("Color" to listOf("Red", "Red"))),
                    ProductDraft(name = "M", options = mapOf("__proto__" to listOf("x"))),
                    ProductDraft(
                        name = "M",
                        options = (1..11).associate { "O$it" to listOf("v") },
                    ),
                )
            for (draft in invalid) {
                assertEquals("VALIDATION_ERROR", errorCode(save("b", null, draft)))
            }
            assertTrue(f.calls.isEmpty())
        }

    @Test
    fun `product save normalizes trims and passes the id through`() = runBlocking {
        val f = Fake()
        val result =
            SaveProductUseCase(f)(
                "b",
                "p1",
                ProductDraft(name = "  Manual  ", externalKey = "  EXT  ", basePrice = "  9.99  "),
            )
        assertTrue(result is ApiResult.Success)
        assertEquals("save:b:p1", f.calls.single())
        assertEquals("Manual", f.lastDraft!!.name)
        assertEquals("EXT", f.lastDraft!!.externalKey)
        assertEquals("9.99", f.lastDraft!!.basePrice)
    }

    // ---- SetVariantStockUseCase ----

    @Test
    fun `stock rejects negative quantities as INVENTORY_QUANTITY_INVALID`() = runBlocking {
        val f = Fake()
        val stock = SetVariantStockUseCase(f)
        assertEquals("INVENTORY_QUANTITY_INVALID", errorCode(stock("b", "p1", "v1", -1)))
        assertTrue(f.calls.isEmpty())
    }

    @Test
    fun `stock writes null to disable tracking and nonnegative quantities as is`() =
        runBlocking {
            val f = Fake()
            val stock = SetVariantStockUseCase(f)
            assertTrue(stock("b", "p1", "v1", null) is ApiResult.Success)
            assertTrue(stock("b", "p1", "v1", 0) is ApiResult.Success)
            assertTrue(stock("b", "p1", "v1", 15) is ApiResult.Success)
            assertEquals(
                listOf("adjust:b:p1:v1:null", "adjust:b:p1:v1:0", "adjust:b:p1:v1:15"),
                f.calls,
            )
        }
}
