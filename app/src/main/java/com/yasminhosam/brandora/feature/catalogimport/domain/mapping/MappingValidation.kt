package com.yasminhosam.brandora.feature.catalogimport.domain.mapping

sealed interface MappingInvalidReason {
    data object ExternalKeyMissing : MappingInvalidReason

    data object NameMissing : MappingInvalidReason

    data object EmptySheet : MappingInvalidReason

    data class StandardTargetUsedTwice(val target: StandardTarget) : MappingInvalidReason

    data class EmptyCustomKey(val index: Int) : MappingInvalidReason

    data class BadCustomKey(val index: Int) : MappingInvalidReason

    data class CustomKeyUsedTwice(val index: Int) : MappingInvalidReason
}

sealed interface MappingValidationResult {
    data object Valid : MappingValidationResult
    data class Invalid(val reasons: List<MappingInvalidReason>) : MappingValidationResult
}

fun normalizeMapping(
    columns: List<MappedColumn>
): List<MappedColumn> {
    return columns.map { column ->
        when (val choice = column.choice) {
            is MappingChoice.CustomAttribute ->
                column.copy(
                    choice = choice.copy(key = choice.key.trim())
                )

            is MappingChoice.VariantAttribute ->
                column.copy(
                    choice = choice.copy(key = choice.key.trim())
                )

            else -> column
        }
    }
}

fun validateMapping(columns: List<MappedColumn>): MappingValidationResult {
    if (columns.isEmpty()) {
        return MappingValidationResult.Invalid(listOf(MappingInvalidReason.EmptySheet))
    }
    val reasons = mutableListOf<MappingInvalidReason>()
    if (columns.none { (it.choice as? MappingChoice.Standard)?.target == StandardTarget.NAME }) {
        reasons += MappingInvalidReason.NameMissing
    }
    if (
        columns.none {
            (it.choice as? MappingChoice.Standard)?.target == StandardTarget.EXTERNAL_KEY
        }
    )
        reasons += MappingInvalidReason.ExternalKeyMissing
    columns
        .mapNotNull { (it.choice as? MappingChoice.Standard)?.target }
        .groupBy { it }
        .filter { it.value.size > 1 }
        .forEach { reasons += MappingInvalidReason.StandardTargetUsedTwice(it.key) }
    val keys = columns.mapIndexedNotNull { index, column ->
        (column.choice as? MappingChoice.CustomAttribute)?.let { index to it.key }
    }
    keys.forEach { (index, key) ->
        when {
            key.isEmpty() -> reasons += MappingInvalidReason.EmptyCustomKey(index)
            !CUSTOM_KEY_REGEX.matches(key) || key in FORBIDDEN_CUSTOM_KEYS ->
                reasons += MappingInvalidReason.BadCustomKey(index)
        }
    }
    keys
        .filter { it.second.isNotEmpty() }
        .groupBy(keySelector = { it.second }, valueTransform = { it.first })
        .values
        .filter { it.size > 1 }
        .forEach { indices ->
            indices.drop(1).forEach { reasons += MappingInvalidReason.CustomKeyUsedTwice(it) }
        }
    val variantKeys = columns.mapIndexedNotNull { index, column ->
        (column.choice as? MappingChoice.VariantAttribute)?.let { index to it.key }
    }
    variantKeys.forEach { (index, key) ->
        if (key.isEmpty()) reasons += MappingInvalidReason.EmptyCustomKey(index)
        else if (key != key.trim() || key.length > 60 || key in FORBIDDEN_CUSTOM_KEYS)
            reasons += MappingInvalidReason.BadCustomKey(index)
    }
    variantKeys
        .filter { it.second.isNotEmpty() }
        .groupBy({ it.second }, { it.first })
        .values
        .filter { it.size > 1 }
        .forEach { indices ->
            indices.drop(1).forEach { reasons += MappingInvalidReason.CustomKeyUsedTwice(it) }
        }
    return if (reasons.isEmpty()) MappingValidationResult.Valid
    else MappingValidationResult.Invalid(reasons)
}

private val CUSTOM_KEY_REGEX = Regex("^[a-z][a-z0-9_]{0,63}$")
private val FORBIDDEN_CUSTOM_KEYS = setOf("constructor", "prototype", "__proto__")
