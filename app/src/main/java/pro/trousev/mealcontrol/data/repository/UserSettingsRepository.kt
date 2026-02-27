package pro.trousev.mealcontrol.data.repository

import pro.trousev.mealcontrol.data.local.dao.UserSettingsDao
import pro.trousev.mealcontrol.data.local.entity.UserSettingsEntity

class UserSettingsRepository(private val userSettingsDao: UserSettingsDao) {

    suspend fun getSettings(): UserSettingsEntity? {
        return userSettingsDao.getSettings()
    }

    suspend fun saveSettings(settings: UserSettingsEntity) {
        userSettingsDao.saveSettings(settings)
    }
}
