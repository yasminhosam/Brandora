package com.yasminhosam.brandora.core.navigation

import java.net.URLEncoder

object Routes {
    // Auth
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val FORGOT_PASSWORD = "forgot_password"
    // Static route on purpose: recovery is driven by an emailed OTP, so no reset
    // token ever appears in navigation.
    const val RESET_PASSWORD = "reset_password?email={email}"

    // Encode the value so addresses containing '+', '&' or '#' survive navigation intact.
    fun resetPassword(email: String = "") =
        "reset_password?email=${URLEncoder.encode(email, Charsets.UTF_8.name())}"

    // Onboarding
    const val BUSINESS_CREATE = "business/create"

    // Bottom-bar tabs
    const val HOME = "home"
    const val DASHBOARD = "dashboard"
    const val ORDERS = "orders"
    // Products doubles as the nested graph route, so the tab keeps a single route.
    const val PRODUCTS = "products"

    // Products graph destinations. Detail, editors, the variant screens and the inventory
    // screen run inside ProductsRoute as one compose state machine, so only the graph start
    // needs a route.
    const val PRODUCTS_BROWSE = "products/browse"

    // Import flow: one nested graph, opened from Home or Products, or automatically on first
    // run after business creation (onboarding = true, which is what offers the Skip action).
    const val IMPORT_GRAPH = "import?onboarding={onboarding}"
    const val UPLOAD = "import/upload"
    const val MAPPING = "import/mapping/{importId}"
    const val VALIDATE = "import/validate/{importId}"
    const val PREVIEW = "import/preview/{importId}"
    const val RESULT = "import/result/{importId}"

    // Working hours: one screen, optional onboarding flag (true right after the import)
    const val WORKING_HOURS = "working_hours?onboarding={onboarding}"

    fun workingHours(onboarding: Boolean = false) = "working_hours?onboarding=$onboarding"

    // Inbox: one nested graph opened from a Home card (list -> chat)
    const val INBOX_GRAPH = "inbox"
    const val INBOX = "inbox/list"
    const val CHAT = "inbox/chat/{conversationId}"

    fun chat(conversationId: String) = "inbox/chat/$conversationId"

    fun importGraph(onboarding: Boolean = false) = "import?onboarding=$onboarding"

    fun mapping(importId: String) = "import/mapping/$importId"
    fun validate(importId: String) = "import/validate/$importId"
    fun preview(importId: String) = "import/preview/$importId"
    fun result(importId: String) = "import/result/$importId"
}