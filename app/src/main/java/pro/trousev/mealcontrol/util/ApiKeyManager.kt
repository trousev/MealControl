package pro.trousev.mealcontrol.util

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.io.File
import java.security.KeyStore

interface SecureStorage {
    fun storeApiKey(apiKey: String)

    fun retrieveApiKey(): String
}

class ApiKeyManager(
    private val context: Context,
) : SecureStorage {
    private var _sharedPreferences: SharedPreferences? = null

    private val sharedPreferences: SharedPreferences
        get() {
            if (_sharedPreferences == null) {
                _sharedPreferences = tryCreateSharedPreferences()
            }
            if (_sharedPreferences == null) {
                resetAndRecreate()
                _sharedPreferences = tryCreateSharedPreferences()
            }
            if (_sharedPreferences == null) {
                _sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            }
            return _sharedPreferences!!
        }

    private fun tryCreateSharedPreferences(): SharedPreferences? {
        return try {
            val masterKey = MasterKey
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

            EncryptedSharedPreferences.create(
                context,
                PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun resetAndRecreate() {
        try {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .clear()
                .commit()
        } catch (ignored: Exception) {
        }

        try {
            val prefsFile = File("${context.filesDir.parent}/shared_prefs/$PREFS_NAME.xml")
            if (prefsFile.exists()) {
                prefsFile.delete()
            }
        } catch (ignored: Exception) {
        }

        try {
            val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER)
            keyStore.load(null)
            keyStore.deleteEntry(MasterKey.DEFAULT_MASTER_KEY_ALIAS)
        } catch (ignored: Exception) {
        }
    }

    override fun storeApiKey(apiKey: String) {
        try {
            sharedPreferences.edit().putString(KEY_API_KEY, apiKey).apply()
        } catch (e: Exception) {
            resetAndRecreate()
            _sharedPreferences = null
            sharedPreferences.edit().putString(KEY_API_KEY, apiKey).apply()
        }
    }

    override fun retrieveApiKey(): String {
        return try {
            sharedPreferences.getString(KEY_API_KEY, "") ?: ""
        } catch (e: Exception) {
            resetAndRecreate()
            _sharedPreferences = null
            sharedPreferences.getString(KEY_API_KEY, "") ?: ""
        }
    }

    companion object {
        private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        private const val PREFS_NAME = "mealcontrol_secure_prefs"
        private const val KEY_API_KEY = "openai_api_key"
    }
}
