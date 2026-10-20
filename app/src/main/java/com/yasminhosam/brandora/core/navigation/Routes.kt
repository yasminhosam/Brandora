package com.yasminhosam.brandora.core.navigation

object Routes {
    // Onboarding
    const val BUSINESS_CREATE = "business/create"

    // Bottom-bar tabs
    const val HOME = "home"
    const val DASHBOARD = "dashboard"
    const val ORDERS = "orders"
    const val PRODUCTS = "products"

    // Import flow: one nested graph, opened from Home or Products
    const val IMPORT_GRAPH = "import"
    const val UPLOAD = "import/upload"
    const val ADD_PRODUCT = "import/add_product"
    const val MAPPING = "import/mapping/{importId}"
    const val VALIDATE = "import/validate/{importId}"
    const val PREVIEW = "import/preview/{importId}"
    const val RESULT = "import/result/{importId}"

    fun mapping(importId: String) = "import/mapping/$importId"
    fun validate(importId: String) = "import/validate/$importId"
    fun preview(importId: String) = "import/preview/$importId"
    fun result(importId: String) = "import/result/$importId"
}