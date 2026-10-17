package com.yasminhosam.brandora.feature.catalogimport.domain.mapping

import com.yasminhosam.brandora.core.network.MappingTypeValues

data class ColumnSuggestion(
    val sourceColumn: String,
    val sourceColumnIndex: Int,
    val samples: List<String>,
    val suggestedType: String,
    val suggestedTarget: String?
)

data class SavedMapping(
    val sourceColumn: String,
    val sourceColumnIndex: Int,
    val type: String,
    val target: String?
)

data class MappingPayload(
    val columns: List<ColumnSuggestion>,
    val savedMapping: List<SavedMapping>?
)

fun initialColumns(
    suggestions: List<ColumnSuggestion>,
    savedMapping: List<SavedMapping>?
): List<MappedColumn> {
    val savedByIndex = savedMapping?.associateBy { it.sourceColumnIndex }.orEmpty()
    return suggestions.map { suggestion ->
        val saved = savedByIndex[suggestion.sourceColumnIndex]
        val choice = if (saved != null) savedChoice(saved) else suggestedChoice(suggestion)
        MappedColumn(
            sourceColumn = suggestion.sourceColumn,
            sourceColumnIndex = suggestion.sourceColumnIndex,
            samples = suggestion.samples,
            choice = choice
        )
    }
}

private fun savedChoice(saved: SavedMapping): MappingChoice = when (saved.type) {
    MappingTypeValues.STANDARD ->
        saved.target?.let { StandardTarget.fromValue(it) }?.let { MappingChoice.Standard(it) }
            ?: MappingChoice.Ignore
    MappingTypeValues.ATTRIBUTE -> MappingChoice.CustomAttribute(saved.target.orEmpty())
    else -> MappingChoice.Ignore
}

private fun suggestedChoice(suggestion: ColumnSuggestion): MappingChoice =
    if (suggestion.suggestedType == MappingTypeValues.STANDARD) {
        suggestion.suggestedTarget?.let { StandardTarget.fromValue(it) }
            ?.let { MappingChoice.Standard(it) }
            ?: MappingChoice.Ignore
    } else {
        MappingChoice.Ignore
    }
