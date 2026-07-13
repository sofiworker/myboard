package xyz.xiao6.myboard.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import xyz.xiao6.myboard.data.dao.SettingsDao
import xyz.xiao6.myboard.data.entity.SettingsEntity
import xyz.xiao6.myboard.data.entity.ToolbarItemEntity
import xyz.xiao6.myboard.theme.foundation.AppearanceMode
import xyz.xiao6.myboard.theme.foundation.FoundationPaletteId
import xyz.xiao6.myboard.theme.foundation.FoundationThemeSelection
import xyz.xiao6.myboard.theme.foundation.KeyTreatment

class SettingsRepositoryAppearanceTest {
    @Test
    fun `missing appearance setting returns default`() = runBlocking {
        val repo = SettingsRepository(FakeSettingsDao())

        val settings = repo.getAppearanceSettings()

        assertEquals(FoundationPaletteId.GBOARD_BLUE, settings.foundation.paletteId)
        assertEquals(AppearanceMode.FOLLOW_SYSTEM, settings.foundation.appearanceMode)
    }

    @Test
    fun `update appearance settings persists one json value`() = runBlocking {
        val dao = FakeSettingsDao()
        val repo = SettingsRepository(dao)

        repo.updateAppearanceSettings(
            repo.getAppearanceSettings().copy(
                foundation = FoundationThemeSelection(
                    paletteId = FoundationPaletteId.MINT,
                    appearanceMode = AppearanceMode.DARK
                )
            )
        )

        val stored = dao.getSetting(SettingsRepository.KEY_APPEARANCE_SETTINGS)
        val observed = repo.appearanceSettings.first()
        requireNotNull(stored)
        assertEquals(FoundationPaletteId.MINT, observed.foundation.paletteId)
        assertEquals(AppearanceMode.DARK, observed.foundation.appearanceMode)
    }

    @Test
    fun `update appearance mode only changes mode`() = runBlocking {
        val repo = SettingsRepository(FakeSettingsDao())
        repo.updateAppearanceSettings(
            repo.getAppearanceSettings().copy(
                foundation = FoundationThemeSelection(paletteId = FoundationPaletteId.ROSE)
            )
        )

        repo.updateAppearanceMode(AppearanceMode.LIGHT)

        val settings = repo.getAppearanceSettings()
        assertEquals(FoundationPaletteId.ROSE, settings.foundation.paletteId)
        assertEquals(AppearanceMode.LIGHT, settings.foundation.appearanceMode)
    }

    @Test
    fun `concurrent foundation updates preserve both changes`() = runBlocking {
        val dao = FakeSettingsDao(appearanceReadDelayMs = 100L)
        val paletteRepo = SettingsRepository(dao)
        val treatmentRepo = SettingsRepository(dao)

        val paletteUpdate = async(Dispatchers.Default) {
            paletteRepo.updateFoundationTheme { it.copy(paletteId = FoundationPaletteId.MINT) }
        }
        val treatmentUpdate = async(Dispatchers.Default) {
            treatmentRepo.updateFoundationTheme { it.copy(keyTreatment = KeyTreatment.OUTLINED) }
        }
        paletteUpdate.await()
        treatmentUpdate.await()

        val settings = SettingsRepository(dao).getAppearanceSettings().foundation
        assertEquals(FoundationPaletteId.MINT, settings.paletteId)
        assertEquals(KeyTreatment.OUTLINED, settings.keyTreatment)
    }

    private class FakeSettingsDao(
        private val appearanceReadDelayMs: Long = 0L
    ) : SettingsDao {
        private val values = MutableStateFlow<Map<String, String>>(emptyMap())
        private val toolbar = MutableStateFlow<List<ToolbarItemEntity>>(emptyList())

        override fun getAllSettings(): Flow<List<SettingsEntity>> =
            values.map { map -> map.map { SettingsEntity(it.key, it.value) } }

        override suspend fun getSetting(key: String): String? {
            if (key == SettingsRepository.KEY_APPEARANCE_SETTINGS && appearanceReadDelayMs > 0L) {
                delay(appearanceReadDelayMs)
            }
            return values.value[key]
        }

        override fun observeSetting(key: String): Flow<String?> = values.map { it[key] }

        override suspend fun upsertSetting(entity: SettingsEntity) {
            values.value = values.value + (entity.key to entity.stringValue)
        }

        override suspend fun getSettingCount(): Int = values.value.size

        override fun getToolbarItems(): Flow<List<ToolbarItemEntity>> = toolbar

        override suspend fun upsertToolbarItem(entity: ToolbarItemEntity) {
            toolbar.value = toolbar.value + entity
        }

        override suspend fun deleteToolbarItem(type: String) {
            toolbar.value = toolbar.value.filterNot { it.type == type }
        }

        override suspend fun deleteAllToolbarItems() {
            toolbar.value = emptyList()
        }

        override suspend fun getToolbarItemCount(): Int = toolbar.value.size
    }
}
