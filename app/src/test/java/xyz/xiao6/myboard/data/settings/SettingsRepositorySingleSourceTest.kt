package xyz.xiao6.myboard.data.settings

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsRepositorySingleSourceTest {

    @Test
    fun `settings screens never create a production database or repository`() {
        val sourceRoot = sourceRoot()
        val settingsDirectory = File(sourceRoot, "xyz/xiao6/myboard/ui/settings")
        val violations = settingsDirectory.listFiles()
            .orEmpty()
            .filter { it.extension == "kt" }
            .filter { file ->
                val source = file.readText()
                "SettingsDatabase.getInstance" in source || "SettingsRepository(" in source
            }
            .map(File::getName)

        assertTrue("Settings repository creation found in: $violations", violations.isEmpty())
    }

    @Test
    fun `settings activity is the production settings composition root`() {
        val source = File(
            sourceRoot(),
            "xyz/xiao6/myboard/activity/SettingsActivity.kt"
        ).readText()

        assertTrue("SettingsActivity must create the shared repository", "SettingsRepository(" in source)
        assertTrue("SettingsActivity must use the process package store", "PackageStoreProvider.get" in source)
        assertTrue("SettingsActivity must create the package coordinator", "LanguagePackCoordinator(" in source)
        assertTrue("Language settings must receive the shared coordinator", "LanguageSettingsViewModel.Factory(repo, packageCoordinator)" in source)
        assertTrue("Input settings must be reachable from SettingsActivity", "composable(\"input\")" in source)
        assertFalse("SettingsActivity must not create a repository per route", "composable(\"settings\") {\n                            val repo" in source)
    }

    private fun sourceRoot(): File = sequenceOf(
        File("src/main/java"),
        File("app/src/main/java")
    ).first(File::exists)
}
