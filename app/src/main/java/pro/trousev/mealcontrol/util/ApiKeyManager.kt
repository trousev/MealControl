package pro.trousev.mealcontrol.util

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

interface SecureStorage {
    fun storeApiKey(apiKey: String)

    fun retrieveApiKey(): String
}

class ApiKeyManager(
    private val context: Context,
) : SecureStorage {
    private val masterKey: MasterKey =
        MasterKey
            .Builder(context)
            .setKeyGenParameterSpec(
                KeyGenParameterSpec
                    .Builder(
                        MasterKey.DEFAULT_MASTER_KEY_ALIAS,
                        KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
                    ).setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build(),
            ).build()

    private val sharedPreferences =
        EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )

    override fun storeApiKey(apiKey: String) {
        sharedPreferences.edit().putString(KEY_API_KEY, apiKey).apply()
    }

    override fun retrieveApiKey(): String = sharedPreferences.getString(KEY_API_KEY, "") ?: ""

    companion object {
        private const val PREFS_NAME = "mealcontrol_secure_prefs"
        private const val KEY_API_KEY = "openai_api_key"
    }
}
