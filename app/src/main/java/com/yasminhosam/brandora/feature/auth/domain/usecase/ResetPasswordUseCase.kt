package com.yasminhosam.brandora.feature.auth.domain.usecase

import com.yasminhosam.brandora.core.network.ApiResult
import com.yasminhosam.brandora.feature.auth.domain.repository.AuthRepository
import javax.inject.Inject

/**
 * Posts the email, eight-digit OTP and new password (NEVER trimmed - leading/trailing spaces are
 * real characters, same rule as register) to /auth/reset-password.
 */
class ResetPasswordUseCase @Inject constructor(private val repository: AuthRepository) {
    suspend operator fun invoke(email: String, otp: String, newPassword: String): ApiResult<Unit> =
        repository.resetPassword(email, otp, newPassword)
}
