package com.yasminhosam.brandora.feature.catalogimport.domain.mapping

sealed interface MappingInvalidReason {
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

fun validateMapping(columns: List<MappedColumn>): MappingValidationResult {
    if (columns.isEmpty()) {
        return MappingValidationResult.Invalid(listOf(MappingInvalidReason.EmptySheet))
    }
    val reasons = mutableListOf<MappingInvalidReason>()
    if (columns.none { (it.choice as? MappingChoice.Standard)?.target == StandardTarget.NAME }) {
        reasons += MappingInvalidReason.NameMissing
    }
    columns.mapNotNull { (it.choice as? MappingChoice.Standard)?.target }
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
    keys.filter { it.second.isNotEmpty() }
        .groupBy(keySelector = { it.second }, valueTransform = { it.first })
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
