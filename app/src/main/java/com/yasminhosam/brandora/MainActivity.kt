package com.yasminhosam.brandora

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.yasminhosam.brandora.core.network.SessionExpiredSignal
import com.yasminhosam.brandora.core.ui.theme.BrandoraTheme
import com.yasminhosam.brandora.feature.auth.domain.repository.SessionStore
import com.yasminhosam.brandora.feature.auth.domain.reset.extractResetToken
import com.yasminhosam.brandora.feature.auth.presentation.reset.ResetTokenHolder
import com.yasminhosam.brandora.feature.business.domain.repository.CurrentBusinessStore
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var currentBusinessStore: CurrentBusinessStore
    @Inject lateinit var sessionStore: SessionStore
    @Inject lateinit var sessionExpiredSignal: SessionExpiredSignal
    @Inject lateinit var resetTokenHolder: ResetTokenHolder

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // The launch intent may be a reset link (cold start straight from the email).
        handleResetLink(intent)
        setContent {
            BrandoraTheme {
                BrandoraRoot(
                    currentBusinessStore = currentBusinessStore,
                    sessionStore = sessionStore,
                    sessionExpiredSignal = sessionExpiredSignal,
                    resetTokenHolder = resetTokenHolder
                )
            }
        }
    }

    // singleTop: a link tapped while the app is already open reuses this activity instead
    // of stacking a second one. setIntent keeps getIntent() pointing at the newest link
    // (the framework does not refresh it for us), which matters after process death.
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleResetLink(intent)
    }

    /**
     * Hands a validated token to the in-memory holder, or ignores the intent entirely.
     * The link and the token are never logged - extractResetToken already enforced the
     * exact https origin and path before it looks at the fragment.
     */
    private fun handleResetLink(intent: Intent?) {
        if (intent?.action != Intent.ACTION_VIEW) return
        val token = intent.dataString?.let { extractResetToken(it) } ?: return
        resetTokenHolder.submit(token)
    }
}
