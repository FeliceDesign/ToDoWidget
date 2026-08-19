package com.felicedesign.todowidget.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.felicedesign.todowidget.model.Appearance
import com.felicedesign.todowidget.model.BarSide
import com.felicedesign.todowidget.model.Settings
import com.felicedesign.todowidget.model.StorageConfig
import com.felicedesign.todowidget.model.StorageMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/** Appearance, behaviour and storage location, shared by the widget and both activities. */
class SettingsRepository(private val context: Context) {

    val settings: Flow<Settings> = context.settingsDataStore.data.map(::toSettings)

    suspend fun current(): Settings = settings.first()

    suspend fun updateAppearance(transform: (Appearance) -> Appearance) {
        context.settingsDataStore.edit { prefs ->
            val next = transform(toSettings(prefs).appearance)
            prefs[HIGHLIGHT] = next.highlight
            prefs[BACKGROUND] = next.background
            prefs[TEXT] = next.text
            prefs[HIGH] = next.highPriority
            prefs[MEDIUM] = next.mediumPriority
            prefs[LOW] = next.lowPriority
        }
    }

    suspend fun setAutoHideSeconds(seconds: Int) {
        context.settingsDataStore.edit {
            it[AUTO_HIDE] = seconds.coerceIn(Settings.AUTO_HIDE_RANGE)
        }
    }

    suspend fun setAddButtonSide(side: BarSide) {
        context.settingsDataStore.edit { it[ADD_BUTTON_SIDE] = side.name }
    }

    suspend fun setDefaultDurationMinutes(minutes: Long?) {
        context.settingsDataStore.edit { prefs ->
            if (minutes == null) prefs.remove(DEFAULT_DURATION) else prefs[DEFAULT_DURATION] = minutes
        }
    }

    suspend fun setStorage(config: StorageConfig) {
        context.settingsDataStore.edit { prefs ->
            prefs[STORAGE_MODE] = config.mode.name
            config.documentUri.let { if (it == null) prefs.remove(DOCUMENT_URI) else prefs[DOCUMENT_URI] = it }
            config.treeUri.let { if (it == null) prefs.remove(TREE_URI) else prefs[TREE_URI] = it }
            prefs[FILE_NAME] = config.fileName
        }
    }

    private fun toSettings(prefs: Preferences) = Settings(
        appearance = Appearance(
            highlight = prefs[HIGHLIGHT] ?: Appearance.DEFAULT_HIGHLIGHT,
            background = prefs[BACKGROUND] ?: Appearance.DEFAULT_BACKGROUND,
            text = prefs[TEXT] ?: Appearance.DEFAULT_TEXT,
            highPriority = prefs[HIGH] ?: Appearance.DEFAULT_HIGH,
            mediumPriority = prefs[MEDIUM] ?: Appearance.DEFAULT_MEDIUM,
            lowPriority = prefs[LOW] ?: Appearance.DEFAULT_LOW,
        ),
        autoHideSeconds = prefs[AUTO_HIDE] ?: Settings.DEFAULT_AUTO_HIDE_SECONDS,
        defaultDurationMinutes = prefs[DEFAULT_DURATION],
        addButtonSide = prefs[ADD_BUTTON_SIDE]?.let { name ->
            BarSide.entries.firstOrNull { it.name == name }
        } ?: BarSide.RIGHT,
        storage = StorageConfig(
            mode = prefs[STORAGE_MODE]?.let { name ->
                StorageMode.entries.firstOrNull { it.name == name }
            } ?: StorageMode.INTERNAL,
            documentUri = prefs[DOCUMENT_URI],
            treeUri = prefs[TREE_URI],
            fileName = prefs[FILE_NAME] ?: StorageConfig.DEFAULT_FILE_NAME,
        ),
    )

    private companion object {
        val HIGHLIGHT = intPreferencesKey("color_highlight")
        val BACKGROUND = intPreferencesKey("color_background")
        val TEXT = intPreferencesKey("color_text")
        val HIGH = intPreferencesKey("color_priority_high")
        val MEDIUM = intPreferencesKey("color_priority_medium")
        val LOW = intPreferencesKey("color_priority_low")
        val AUTO_HIDE = intPreferencesKey("auto_hide_seconds")
        val DEFAULT_DURATION = longPreferencesKey("default_duration_minutes")
        val ADD_BUTTON_SIDE = stringPreferencesKey("add_button_side")
        val STORAGE_MODE = stringPreferencesKey("storage_mode")
        val DOCUMENT_URI = stringPreferencesKey("storage_document_uri")
        val TREE_URI = stringPreferencesKey("storage_tree_uri")
        val FILE_NAME = stringPreferencesKey("storage_file_name")
    }
}
