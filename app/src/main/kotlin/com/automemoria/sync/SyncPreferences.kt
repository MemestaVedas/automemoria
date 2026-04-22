package com.automemoria.sync

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "sync_prefs")

@Singleton
class SyncPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val lastSyncKey = stringPreferencesKey("last_sync_timestamp")
    private val supabaseUrlKey = stringPreferencesKey("supabase_url")
    private val supabaseKeyKey = stringPreferencesKey("supabase_anon_key")
    private val userEmailKey = stringPreferencesKey("user_email")
    private val defaultBoardIdKey = stringPreferencesKey("default_board_id")

    // Returns epoch-zero ISO string if never synced — this pulls ALL records on first sync
    suspend fun getLastSyncTimestamp(): String =
        context.dataStore.data.map { prefs ->
            prefs[lastSyncKey] ?: "1970-01-01T00:00:00"
        }.first()

    suspend fun setLastSyncTimestamp(timestamp: String) {
        context.dataStore.edit { prefs -> prefs[lastSyncKey] = timestamp }
    }

    suspend fun getSupabaseUrl(): String =
        context.dataStore.data.map { it[supabaseUrlKey] ?: "" }.first()

    suspend fun setSupabaseUrl(url: String) {
        context.dataStore.edit { prefs -> prefs[supabaseUrlKey] = url }
    }

    suspend fun getSupabaseAnonKey(): String =
        context.dataStore.data.map { it[supabaseKeyKey] ?: "" }.first()

    suspend fun setSupabaseAnonKey(key: String) {
        context.dataStore.edit { prefs -> prefs[supabaseKeyKey] = key }
    }

    suspend fun getUserEmail(): String =
        context.dataStore.data.map { it[userEmailKey] ?: "" }.first()

    suspend fun setUserEmail(email: String) {
        context.dataStore.edit { prefs -> prefs[userEmailKey] = email }
    }

    suspend fun getDefaultBoardId(): String? =
        context.dataStore.data.map { it[defaultBoardIdKey] }.first()

    suspend fun setDefaultBoardId(id: String) {
        context.dataStore.edit { prefs -> prefs[defaultBoardIdKey] = id }
    }

    suspend fun isSetupComplete(): Boolean =
        getSupabaseUrl().isNotBlank() && getSupabaseAnonKey().isNotBlank()
}
