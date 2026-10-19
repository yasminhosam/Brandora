package com.yasminhosam.brandora.feature.business.data.mapper

import com.yasminhosam.brandora.feature.business.data.remote.BusinessProfileDto
import com.yasminhosam.brandora.feature.business.data.remote.CreateBusinessRequestDto
import com.yasminhosam.brandora.feature.business.domain.model.BusinessProfile
import com.yasminhosam.brandora.feature.business.domain.model.NewBusiness

internal fun NewBusiness.toRequestDto(): CreateBusinessRequestDto = CreateBusinessRequestDto(
    name = name,
    description = description,
    category = category,
    phone = phone,
    address = address
)

internal fun BusinessProfileDto.toDomain(): BusinessProfile = BusinessProfile(
    id = id,
    name = name,
    description = description,
    category = category,
    phone = phone,
    address = address
)
