package com.yasminhosam.brandora.feature.inbox.presentation.util

/**
 * Appends a next offset-page without duplicates. New activity shifts offsets between requests,
 * so an item can arrive on two pages; the first copy wins.
 */
fun <T, K> List<T>.appendDistinct(incoming: List<T>, keyOf: (T) -> K): List<T> {
    val seen = mapTo(HashSet()) { keyOf(it) }
    return this + incoming.filter { seen.add(keyOf(it)) }
}

/**
 * Prepends newer items (a just-sent message, or a refreshed first page) to a newest-first list,
 * dropping anything already present. The incoming order is kept, so the newest stays first.
 */
fun <T, K> List<T>.prependDistinct(incoming: List<T>, keyOf: (T) -> K): List<T> {
    val seen = mapTo(HashSet()) { keyOf(it) }
    return incoming.filter { seen.add(keyOf(it)) } + this
}
