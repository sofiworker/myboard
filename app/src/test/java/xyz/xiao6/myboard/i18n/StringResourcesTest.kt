package xyz.xiao6.myboard.i18n

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StringResourcesTest {

    @Test
    fun `all supported locales define the same string keys`() {
        val resourceRoot = resourceRoot()
        val defaultKeys = stringKeys(File(resourceRoot, "values/strings.xml"))

        listOf("values-zh-rCN", "values-ja").forEach { directory ->
            val localizedFile = File(resourceRoot, "$directory/strings.xml")
            assertTrue("Missing localized strings: ${localizedFile.path}", localizedFile.isFile)
            assertEquals(
                "String keys differ for $directory",
                defaultKeys,
                stringKeys(localizedFile)
            )
        }
    }

    private fun stringKeys(file: File): Set<String> =
        STRING_NAME.findAll(file.readText())
            .map { it.groupValues[1] }
            .toSet()

    private fun resourceRoot(): File = sequenceOf(
        File("src/main/res"),
        File("app/src/main/res")
    ).first(File::exists)

    private companion object {
        val STRING_NAME = Regex("""<string\s+name="([^"]+)"""")
    }
}
