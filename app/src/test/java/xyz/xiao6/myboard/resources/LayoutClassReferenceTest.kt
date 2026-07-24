package xyz.xiao6.myboard.resources

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class LayoutClassReferenceTest {

    @Test
    fun `layout resources only reference classes that exist`() {
        val projectRoot = projectRoot()
        val layoutDirectory = File(projectRoot, "src/main/res/layout")
        val sourceDirectory = File(projectRoot, "src/main/java")
        val missingClasses = layoutDirectory.listFiles()
            .orEmpty()
            .flatMap { file ->
                CLASS_TAG.findAll(file.readText()).map { it.groupValues[1] }.toList()
            }
            .filterNot { className ->
                val sourcePath = className.replace('.', File.separatorChar)
                File(sourceDirectory, "$sourcePath.kt").isFile ||
                    File(sourceDirectory, "$sourcePath.java").isFile
            }

        assertTrue("Layout resources reference missing classes: $missingClasses", missingClasses.isEmpty())
    }

    private fun projectRoot(): File = sequenceOf(File("."), File("app"))
        .first { File(it, "src/main").isDirectory }

    private companion object {
        val CLASS_TAG = Regex("""<([a-zA-Z_]\w*(?:\.[a-zA-Z_]\w*)+)""")
    }
}
