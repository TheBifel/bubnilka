package com.thebifel.bubnilka.storage

import android.content.Context
import android.content.SharedPreferences
import com.thebifel.bubnilka.core.model.StylePreset
import org.json.JSONArray
import org.json.JSONObject

interface StylePresetStore {
    fun readPresets(): List<StylePreset>?
    fun writePresets(presets: List<StylePreset>)
    fun readSelectedId(): String?
    fun writeSelectedId(id: String)
}

class StylePresetRepository(
    private val store: StylePresetStore,
) {
    fun load(): List<StylePreset> {
        val stored = store.readPresets()
        if (!stored.isNullOrEmpty()) return stored
        return StylePreset.entries.also(store::writePresets)
    }

    fun selected(presets: List<StylePreset> = load()): StylePreset {
        val selected = store.readSelectedId()?.let { id -> presets.firstOrNull { it.id == id } }
        return selected ?: presets.first().also { store.writeSelectedId(it.id) }
    }

    fun select(id: String) {
        if (load().any { it.id == id }) store.writeSelectedId(id)
    }

    fun find(id: String): StylePreset? = load().firstOrNull { it.id == id }

    fun save(preset: StylePreset) {
        val presets = load().toMutableList()
        val index = presets.indexOfFirst { it.id == preset.id }
        if (index >= 0) presets[index] = preset else presets += preset
        store.writePresets(presets)
        if (store.readSelectedId() == null) store.writeSelectedId(preset.id)
    }

    fun delete(id: String): Boolean {
        val presets = load()
        if (presets.size <= 1 || presets.none { it.id == id }) return false
        val remaining = presets.filterNot { it.id == id }
        store.writePresets(remaining)
        if (store.readSelectedId() == id) store.writeSelectedId(remaining.first().id)
        return true
    }

    companion object {
        private const val PREFERENCES_NAME = "bubnilka_style_presets"

        fun from(context: Context): StylePresetRepository = StylePresetRepository(
            SharedPreferencesStylePresetStore(
                context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE),
            ),
        )
    }
}

class SharedPreferencesStylePresetStore(
    private val preferences: SharedPreferences,
) : StylePresetStore {
    override fun readPresets(): List<StylePreset>? {
        if (!preferences.contains(KEY_PRESETS)) return null
        return runCatching {
            val array = JSONArray(preferences.getString(KEY_PRESETS, "[]"))
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.getJSONObject(index)
                    runCatching {
                        StylePreset(
                            id = item.getString("id"),
                            title = item.getString("title"),
                            instruction = item.getString("instruction"),
                            isDefault = item.optBoolean("isDefault", false),
                        )
                    }.getOrNull()?.let(::add)
                }
            }
        }.getOrNull()
    }

    override fun writePresets(presets: List<StylePreset>) {
        val array = JSONArray()
        presets.forEach { preset ->
            array.put(
                JSONObject()
                    .put("id", preset.id)
                    .put("title", preset.title)
                    .put("instruction", preset.instruction)
                    .put("isDefault", preset.isDefault),
            )
        }
        preferences.edit().putString(KEY_PRESETS, array.toString()).apply()
    }

    override fun readSelectedId(): String? = preferences.getString(KEY_SELECTED_ID, null)

    override fun writeSelectedId(id: String) {
        preferences.edit().putString(KEY_SELECTED_ID, id).apply()
    }

    private companion object {
        const val KEY_PRESETS = "presets_json"
        const val KEY_SELECTED_ID = "selected_preset_id"
    }
}
