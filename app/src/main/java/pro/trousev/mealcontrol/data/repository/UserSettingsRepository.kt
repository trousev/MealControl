package pro.trousev.mealcontrol.data.repository

import pro.trousev.mealcontrol.data.local.dao.UserSettingsDao
import pro.trousev.mealcontrol.data.local.entity.UserSettingsEntity
import pro.trousev.mealcontrol.util.SecureStorage

class UserSettingsRepository(
    private val userSettingsDao: UserSettingsDao,
    private val secureStorage: SecureStorage,
) {
    suspend fun getSettings(): UserSettingsEntity? {
        val settings = userSettingsDao.getSettings() ?: return null
        return settings.copy(
            openAiApiKey = secureStorage.retrieveApiKey(),
        )
    }

    suspend fun saveSettings(settings: UserSettingsEntity) {
        secureStorage.storeApiKey(settings.openAiApiKey)
        val settingsWithoutApiKey = settings.copy(openAiApiKey = "")
        userSettingsDao.saveSettings(settingsWithoutApiKey)
    }
}
