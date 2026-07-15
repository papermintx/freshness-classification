package com.skripsi.core.domain.usecase

import com.skripsi.core.domain.model.RoiConfiguration

class GetRoiConfigurationUseCase {
    operator fun invoke(): RoiConfiguration {
        return RoiConfiguration.Default
    }
}
