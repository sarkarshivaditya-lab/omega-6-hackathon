package com.udc.collection.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.udc.collection.util.PbkdfHasher
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "udc_prefs")

@Singleton
class PreferencesDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.dataStore

    companion object {
        private val KEY_AGENT_NAME = stringPreferencesKey("agent_name")
        private val KEY_ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
        private val KEY_DARK_MODE = booleanPreferencesKey("dark_mode")
        private val KEY_ADMIN_PIN_HASH = stringPreferencesKey("admin_pin_hash")

        // Shipped as the PBKDF2 hash of "1234" so new installs start correctly.
        // Existing installs may have the legacy SHA-256 hash — PbkdfHasher handles migration.
        val DEFAULT_PIN_HASH: String by lazy { PbkdfHasher.hash("1234") }
    }

    val agentName: Flow<String> = dataStore.data.map { it[KEY_AGENT_NAME] ?: "" }
    val onboardingDone: Flow<Boolean> = dataStore.data.map { it[KEY_ONBOARDING_DONE] ?: false }
    val darkMode: Flow<Boolean> = dataStore.data.map { it[KEY_DARK_MODE] ?: false }
    val adminPinHash: Flow<String> = dataStore.data.map { it[KEY_ADMIN_PIN_HASH] ?: DEFAULT_PIN_HASH }

    suspend fun saveAgentName(name: String) {
        dataStore.edit {
            it[KEY_AGENT_NAME] = name
            it[KEY_ONBOARDING_DONE] = true
        }
    }

    suspend fun setDarkMode(enabled: Boolean) {
        dataStore.edit { it[KEY_DARK_MODE] = enabled }
    }

    suspend fun setAdminPin(newPin: String) {
        dataStore.edit { it[KEY_ADMIN_PIN_HASH] = PbkdfHasher.hash(newPin) }
    }

    /**
     * Verifies the PIN and transparently upgrades legacy SHA-256 hashes to PBKDF2.
     * Returns true on match, false on mismatch.
     */
    suspend fun verifyAndMigratePin(input: String): Boolean {
        val stored = adminPinHash.first()
        return when (val result = PbkdfHasher.verify(input, stored)) {
            is PbkdfHasher.VerifyResult.Match -> {
                result.upgradedHash?.let { upgraded ->
                    dataStore.edit { it[KEY_ADMIN_PIN_HASH] = upgraded }
                }
                true
            }
            PbkdfHasher.VerifyResult.Mismatch -> false
        }
    }

    suspend fun clearAll() {
        dataStore.edit { it.clear() }
    }
}
