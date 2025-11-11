package id.avium.aviumnotes.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "avium_notes_preferences")

class PreferencesManager(private val context: Context) {

    companion object {
        private val IS_ONBOARDING_COMPLETED = booleanPreferencesKey("is_onboarding_completed")
        private val THEME_MODE = stringPreferencesKey("theme_mode")
        private val SORT_BY = stringPreferencesKey("sort_by")
        private val DEFAULT_NOTE_COLOR = intPreferencesKey("default_note_color")
        private val SHOW_NOTE_PREVIEW = booleanPreferencesKey("show_note_preview")
        private val FLOATING_BUBBLE_ENABLED = booleanPreferencesKey("floating_bubble_enabled")
        private val AUTO_SAVE_ENABLED = booleanPreferencesKey("auto_save_enabled")
        private val DELETE_CONFIRMATION = booleanPreferencesKey("delete_confirmation")
    }

    val isOnboardingCompleted: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[IS_ONBOARDING_COMPLETED] ?: false
        }

    val themeMode: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[THEME_MODE] ?: "system"
        }

    val sortBy: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[SORT_BY] ?: "date_modified"
        }

    val defaultNoteColor: Flow<Int> = context.dataStore.data
        .map { preferences ->
            preferences[DEFAULT_NOTE_COLOR] ?: 0xFFFFFFFF.toInt()
        }

    val showNotePreview: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[SHOW_NOTE_PREVIEW] ?: true
        }

    val floatingBubbleEnabled: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[FLOATING_BUBBLE_ENABLED] ?: false
        }

    val autoSaveEnabled: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[AUTO_SAVE_ENABLED] ?: true
        }

    val deleteConfirmation: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[DELETE_CONFIRMATION] ?: true
        }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[IS_ONBOARDING_COMPLETED] = completed
        }
    }

    suspend fun setThemeMode(mode: String) {
        context.dataStore.edit { preferences ->
            preferences[THEME_MODE] = mode
        }
    }

    suspend fun setSortBy(sortBy: String) {
        context.dataStore.edit { preferences ->
            preferences[SORT_BY] = sortBy
        }
    }

    suspend fun setDefaultNoteColor(color: Int) {
        context.dataStore.edit { preferences ->
            preferences[DEFAULT_NOTE_COLOR] = color
        }
    }

    suspend fun setShowNotePreview(show: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[SHOW_NOTE_PREVIEW] = show
        }
    }

    suspend fun setFloatingBubbleEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[FLOATING_BUBBLE_ENABLED] = enabled
        }
    }

    suspend fun setAutoSaveEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[AUTO_SAVE_ENABLED] = enabled
        }
    }

    suspend fun setDeleteConfirmation(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[DELETE_CONFIRMATION] = enabled
        }
    }
}
