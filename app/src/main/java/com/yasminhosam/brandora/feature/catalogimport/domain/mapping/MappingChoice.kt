package com.yasminhosam.brandora.feature.catalogimport.domain.mapping

import com.yasminhosam.brandora.core.network.StandardTargets

sealed interface MappingChoice {
    data class Standard(val target: StandardTarget) : MappingChoice
    data class CustomAttribute(val key: String) : MappingChoice
    data object Ignore : MappingChoice
}

enum class StandardTarget(val value: String) {
    NAME(StandardTargets.NAME),
    EXTERNAL_KEY(StandardTargets.EXTERNAL_KEY),
    DESCRIPTION(StandardTargets.DESCRIPTION),
    CATEGORY(StandardTargets.CATEGORY),
    BASE_PRICE(StandardTargets.BASE_PRICE),
    STOCK_QUANTITY(StandardTargets.STOCK_QUANTITY);

    companion object {
        fun fromValue(value: String?): StandardTarget? = entries.firstOrNull { it.value == value }
    }
}
