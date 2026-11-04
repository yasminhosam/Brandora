package com.yasminhosam.brandora.core.navigation

import androidx.navigation.NavController

/**
 * Pops every entry, then lands on [route] as the ONLY back-stack entry.
 *
 * Used wherever a flow must end on a clean root: session expiry (refresh rejected) and a
 * successful password reset (all sessions revoked server-side). popUpTo(start) silently
 * no-ops when the start destination isn't on the back stack - a real case, since the
 * process may have started at Login and logged in since - and popBackStack() only removes
 * the top entry, so walk down to the bottom entry and let the navigate()'s popUpTo remove
 * that one too. System back then exits the app instead of revealing a session-less screen.
 */
fun NavController.clearTo(route: String) {
    while (previousBackStackEntry != null) {
        if (!popBackStack()) break
    }
    val bottomId = currentBackStackEntry?.destination?.id
    navigate(route) {
        if (bottomId != null) popUpTo(bottomId) { inclusive = true }
        launchSingleTop = true
    }
}
