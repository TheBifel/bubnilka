package com.thebifel.bubnilka.storage

import com.thebifel.bubnilka.core.model.StylePreset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StylePresetRepositoryTest {
    @Test
    fun seedsEditableFactoryPresetsOnlyOnce() {
        val store = FakeStore()
        val repository = StylePresetRepository(store)

        assertEquals(StylePreset.entries, repository.load())
        assertEquals(1, store.writeCalls)
        assertEquals(StylePreset.entries, repository.load())
        assertEquals(1, store.writeCalls)
    }

    @Test
    fun editsFactoryPresetAndAddsCustomPresetInOrder() {
        val repository = StylePresetRepository(FakeStore())
        repository.load()
        repository.save(StylePreset.STRUCTURED.copy(title = "Мій структурний"))
        repository.save(StylePreset("custom-1", "Новий", "Зроби текст новим."))

        val presets = repository.load()
        assertEquals("Мій структурний", presets.first().title)
        assertEquals("custom-1", presets.last().id)
    }

    @Test
    fun deletingSelectedPresetSelectsFirstRemainingAndLastPresetIsProtected() {
        val store = FakeStore()
        val repository = StylePresetRepository(store)
        repository.load()
        repository.select(StylePreset.VIKING.id)

        assertTrue(repository.delete(StylePreset.VIKING.id))
        assertEquals(StylePreset.STRUCTURED.id, repository.selected().id)
        repository.load().drop(1).forEach { assertTrue(repository.delete(it.id)) }
        assertFalse(repository.delete(repository.load().single().id))
    }

    private class FakeStore : StylePresetStore {
        var presets: List<StylePreset>? = null
        var selectedId: String? = null
        var writeCalls = 0

        override fun readPresets(): List<StylePreset>? = presets
        override fun writePresets(presets: List<StylePreset>) {
            this.presets = presets
            writeCalls += 1
        }
        override fun readSelectedId(): String? = selectedId
        override fun writeSelectedId(id: String) {
            selectedId = id
        }
    }
}
