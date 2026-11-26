package com.yasminhosam.brandora.feature.products.domain

import com.yasminhosam.brandora.feature.products.domain.model.ProductDraft
import com.yasminhosam.brandora.feature.products.domain.model.VariantDraft
import kotlinx.serialization.json.*

data class OptionDraft(val name: String = "", val values: String = "") : java.io.Serializable

object ProductRules {
    const val MAX_IMAGES = 5

    private val forbiddenOptionNames = setOf("__proto__", "prototype", "constructor")

    fun validMoney(value: String) =
        value.isBlank() || Regex("(0|[1-9][0-9]{0,9})(\\.[0-9]{1,2})?").matches(value)

    fun validExternalKey(value: String) = value.trim().codePointCount(0, value.trim().length) <= 200

    fun validCategoryName(name: String) =
        name.isNotBlank() && name.codePointCount(0, name.length) <= 200

    fun definitions(drafts: List<OptionDraft>): Map<String, List<String>>? {
        if (drafts.size > 10) return null
        val out = linkedMapOf<String, List<String>>()
        for (draft in drafts) {
            val name = draft.name.trim()
            val values = draft.values.lines().map { it.trim() }.filter { it.isNotEmpty() }
            if (name.isBlank() || out.containsKey(name)) return null
            out[name] = values
        }
        return out.takeIf { validDefinitions(it) }
    }

    /** Server-shaped option maps: names, values and total size within the backend's limits. */
    fun validDefinitions(options: Map<String, List<String>>): Boolean {
        if (options.size > 10) return false
        for ((name, values) in options) {
            if (
                name.isBlank() ||
                    name.length > 60 ||
                    name in forbiddenOptionNames ||
                    values.isEmpty() ||
                    values.size > 50 ||
                    values.any { it.length > 100 } ||
                    values.distinct().size != values.size
            ) return false
        }
        val json = JsonObject(options.mapValues { JsonArray(it.value.map(::JsonPrimitive)) })
        return json.toString().toByteArray(Charsets.UTF_8).size <= 16384
    }

    /** Full draft validation used by SaveProductUseCase; fields may be normalized or raw. */
    fun validProduct(draft: ProductDraft): Boolean {
        val name = draft.name.trim()
        return name.isNotBlank() &&
            name.codePointCount(0, name.length) <= 200 &&
            (draft.description?.let { it.codePointCount(0, it.length) <= 5000 } ?: true) &&
            validExternalKey(draft.externalKey ?: "") &&
            validMoney(draft.basePrice ?: "") &&
            validDefinitions(draft.options)
    }

    /** Field-level variant checks (money, sku); option selection is checked separately. */
    fun validVariantFields(draft: VariantDraft): Boolean {
        val sku = draft.sku?.trim()
        return validMoney(draft.unitPrice ?: "") && (sku == null || sku.codePointCount(0, sku.length) <= 200)
    }

    fun selectionMatches(options: Map<String, List<String>>, selection: Map<String, String>) =
        selection.keys == options.keys &&
            selection.all { (key, value) -> options[key]?.contains(value) == true }

    /**
     * Option edits must keep every existing name and its values: variants (including inactive
     * ones) already select them, and the server rejects any definition that invalidates a
     * combination (PRODUCT_OPTIONS_IN_USE). Adding names or values is always allowed.
     */
    fun validOptionSuperset(
        existing: Map<String, List<String>>,
        edited: Map<String, List<String>>,
    ): Boolean = existing.all { (name, values) -> edited[name]?.containsAll(values) == true }

    fun batchFits(existing: Int, incoming: Int) =
        existing >= 0 && incoming in 1..MAX_IMAGES && existing + incoming <= MAX_IMAGES

    fun orderedIds(ids: List<String>, index: Int, to: Int): List<String>? {
        if (index !in ids.indices || to !in ids.indices || ids.distinct().size != ids.size)
            return null
        return ids.toMutableList().apply { add(to, removeAt(index)) }
    }
}
