package xyz.xiao6.myboard.data.settings

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import xyz.xiao6.myboard.contract.state.LocaleTag
import xyz.xiao6.myboard.contract.state.OrthogonalState
import xyz.xiao6.myboard.contract.state.Schema
import xyz.xiao6.myboard.contract.state.Script
import xyz.xiao6.myboard.data.dao.SettingsDao
import xyz.xiao6.myboard.data.entity.SettingsEntity
import xyz.xiao6.myboard.data.entity.ToolbarItemEntity
import xyz.xiao6.myboard.data.repository.LanguagePackPreferences
import xyz.xiao6.myboard.data.repository.SettingsRepository

class LanguagePackPreferencesTest {

    @Test
    fun `language pack preferences use canonical sorted json and round trip atomically`() = runBlocking {
        val dao = FakeSettingsDao()
        val repository = SettingsRepository(dao)
        val arabic = OrthogonalState(LocaleTag("ar-SA"), Script.ARAB, Schema("DIRECT"))
        val thai = OrthogonalState(LocaleTag("th-TH"), Script.THAI, Schema("DIRECT"))

        repository.updateLanguagePackPreferences(
            LanguagePackPreferences(
                enabledPackageIds = setOf("external.thai", "external.arabic"),
                providerPreferences = mapOf(thai to "external.thai", arabic to "external.arabic")
            )
        )

        assertEquals(
            "[\"external.arabic\",\"external.thai\"]",
            dao.values.getValue(SettingsRepository.KEY_ENABLED_LANGUAGE_PACKAGES)
        )
        assertEquals(
            "{\"ar-SA|ARAB|DIRECT\":\"external.arabic\",\"th-TH|THAI|DIRECT\":\"external.thai\"}",
            dao.values.getValue(SettingsRepository.KEY_PROVIDER_PREFERENCES)
        )
        assertEquals(1, dao.batchWrites)
        assertEquals(2, dao.lastBatchSize)
        assertEquals(
            setOf("external.arabic", "external.thai"),
            repository.getLanguagePackPreferences().enabledPackageIds
        )
    }

    @Test
    fun `malformed language pack preference json falls back to empty`() = runBlocking {
        val dao = FakeSettingsDao().apply {
            values[SettingsRepository.KEY_ENABLED_LANGUAGE_PACKAGES] = "not-json"
            values[SettingsRepository.KEY_PROVIDER_PREFERENCES] = "[]"
        }

        val preferences = SettingsRepository(dao).getLanguagePackPreferences()

        assertTrue(preferences.enabledPackageIds.isEmpty())
        assertTrue(preferences.providerPreferences.isEmpty())
    }

    private class FakeSettingsDao : SettingsDao {
        val values = linkedMapOf<String, String>()
        private val settingsFlow = MutableStateFlow<List<SettingsEntity>>(emptyList())
        private val toolbarFlow = MutableStateFlow<List<ToolbarItemEntity>>(emptyList())
        var batchWrites = 0
        var lastBatchSize = 0

        override fun getAllSettings(): Flow<List<SettingsEntity>> = settingsFlow
        override suspend fun getSetting(key: String): String? = values[key]
        override fun observeSetting(key: String): Flow<String?> = MutableStateFlow(values[key])
        override suspend fun upsertSetting(entity: SettingsEntity) {
            values[entity.key] = entity.stringValue
        }
        override suspend fun upsertSettings(entities: List<SettingsEntity>) {
            batchWrites += 1
            lastBatchSize = entities.size
            entities.forEach { upsertSetting(it) }
        }
        override suspend fun getSettingCount(): Int = values.size
        override fun getToolbarItems(): Flow<List<ToolbarItemEntity>> = toolbarFlow
        override suspend fun upsertToolbarItem(entity: ToolbarItemEntity) = Unit
        override suspend fun deleteToolbarItem(type: String) = Unit
        override suspend fun deleteAllToolbarItems() = Unit
        override suspend fun getToolbarItemCount(): Int = 0
    }
}
