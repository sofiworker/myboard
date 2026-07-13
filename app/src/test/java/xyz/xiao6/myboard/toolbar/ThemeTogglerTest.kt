package xyz.xiao6.myboard.toolbar

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import xyz.xiao6.myboard.data.dao.SettingsDao
import xyz.xiao6.myboard.data.entity.SettingsEntity
import xyz.xiao6.myboard.data.entity.ToolbarItemEntity
import xyz.xiao6.myboard.data.repository.SettingsRepository
import xyz.xiao6.myboard.theme.foundation.AppearanceMode
import xyz.xiao6.myboard.theme.foundation.ThemeVariant

class ThemeTogglerTest {
    @Test
    fun `toggle from effective light writes dark mode`() = runBlocking {
        val dao = FakeSettingsDao()
        val repo = SettingsRepository(dao)
        val toggler = ThemeToggler(repo)

        toggler.toggle(ThemeVariant.LIGHT)

        assertEquals(AppearanceMode.DARK, repo.getAppearanceSettings().foundation.appearanceMode)
    }

    @Test
    fun `toggle from follow system dark writes light mode`() = runBlocking {
        val dao = FakeSettingsDao()
        val repo = SettingsRepository(dao)
        val toggler = ThemeToggler(repo)

        toggler.toggle(ThemeVariant.DARK)

        assertEquals(AppearanceMode.LIGHT, repo.getAppearanceSettings().foundation.appearanceMode)
    }

    private class FakeSettingsDao : SettingsDao {
        private val values = MutableStateFlow<Map<String, String>>(emptyMap())
        private val toolbar = MutableStateFlow<List<ToolbarItemEntity>>(emptyList())

        override fun getAllSettings(): Flow<List<SettingsEntity>> =
            values.map { map -> map.map { SettingsEntity(it.key, it.value) } }

        override suspend fun getSetting(key: String): String? = values.value[key]

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
