package net.yslibrary.monotweety.domain.setting

import kotlinx.coroutines.flow.Flow
import net.yslibrary.monotweety.data.settings.SettingRepository
import net.yslibrary.monotweety.data.settings.Settings
import javax.inject.Inject

interface ObserveSettings {
    operator fun invoke(): Flow<Settings>
}

internal class ObserveSettingsImpl @Inject constructor(
    private val settingRepository: SettingRepository,
) : ObserveSettings {
    override fun invoke(): Flow<Settings> {
        return settingRepository.settingsFlow
    }
}