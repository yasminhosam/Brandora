package com.yasminhosam.brandora.feature.auth.domain.usecase

import com.yasminhosam.brandora.core.network.ApiResult
import com.yasminhosam.brandora.feature.auth.domain.repository.AuthRepository
import javax.inject.Inject

/**
 * Posts the app-link token and the new password (NEVER trimmed - leading/trailing spaces
 * are real characters, same rule as register) to /auth/reset-password.
 */
class ResetPasswordUseCase @Inject constructor(
    private val repository: AuthRepository
) {
    suspend operator fun invoke(token: String, newPassword: String): ApiResult<Unit> =
        repository.resetPassword(token, newPassword)
}
