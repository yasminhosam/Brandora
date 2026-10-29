package com.yasminhosam.brandora

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.yasminhosam.brandora.core.network.SessionExpiredSignal
import com.yasminhosam.brandora.core.ui.theme.BrandoraTheme
import com.yasminhosam.brandora.feature.auth.domain.repository.SessionStore
import com.yasminhosam.brandora.feature.business.domain.repository.CurrentBusinessStore
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var currentBusinessStore: CurrentBusinessStore
    @Inject lateinit var sessionStore: SessionStore
    @Inject lateinit var sessionExpiredSignal: SessionExpiredSignal

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BrandoraTheme {
                BrandoraRoot(
                    currentBusinessStore = currentBusinessStore,
                    sessionStore = sessionStore,
                    sessionExpiredSignal = sessionExpiredSignal
                )
            }
        }
    }
}
