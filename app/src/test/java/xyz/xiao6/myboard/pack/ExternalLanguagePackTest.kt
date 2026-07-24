package xyz.xiao6.myboard.pack

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import xyz.xiao6.myboard.contract.manifest.LayoutMirrorPolicy
import xyz.xiao6.myboard.contract.manifest.TextDirection
import xyz.xiao6.myboard.contract.state.Script

class ExternalLanguagePackTest {

    private val decoder = JsonLanguagePackManifestDecoder()

    @Test
    fun `decodes canonical arabic manifest`() {
        val manifest = decoder.decode(validArabicManifest().encodeToByteArray())

        assertEquals("external.arabic", manifest.identity.packageId)
        assertEquals("1.2.3", manifest.identity.version.toString())
        assertEquals(Script.ARAB, manifest.defaults.script)
        assertEquals(TextDirection.RTL, manifest.scripts.single().descriptor.direction)
        assertEquals(LayoutMirrorPolicy.MIRROR_HORIZONTAL, manifest.scripts.single().descriptor.layoutMirror)
        assertEquals("2.0.0", manifest.dependencies.single().versionRange.maximumExclusive.toString())
    }

    @Test
    fun `rejects unknown manifest fields`() {
        val json = validArabicManifest().replace(
            "\"manifestVersion\": 1,",
            "\"manifestVersion\": 1, \"manifestVersoin\": 1,"
        )

        val error = assertThrows(IllegalArgumentException::class.java) {
            decoder.decode(json.encodeToByteArray())
        }

        assertTrue(error.message.orEmpty().contains("manifest", ignoreCase = true))
    }

    @Test
    fun `rejects non canonical semantic versions`() {
        val json = validArabicManifest().replace("\"1.2.3\"", "\"01.2.3\"")

        val error = assertThrows(IllegalArgumentException::class.java) {
            decoder.decode(json.encodeToByteArray())
        }

        assertTrue(error.message.orEmpty().contains("version", ignoreCase = true))
    }

    @Test
    fun `rejects descriptor mismatch during semantic validation`() {
        val json = validArabicManifest().replace(
            "\"script\": \"ARAB\", \"displayNames\"",
            "\"script\": \"LATN\", \"displayNames\""
        )

        val error = assertThrows(IllegalArgumentException::class.java) {
            decoder.decode(json.encodeToByteArray())
        }

        assertTrue(error.message.orEmpty().contains("descriptor", ignoreCase = true))
    }

    @Test
    fun `rejects inverted version range`() {
        val json = validArabicManifest().replace(
            "\"minimum\": \"1.0.0\", \"maximumExclusive\": \"2.0.0\"",
            "\"minimum\": \"2.0.0\", \"maximumExclusive\": \"1.0.0\""
        )

        val error = assertThrows(IllegalArgumentException::class.java) {
            decoder.decode(json.encodeToByteArray())
        }

        assertTrue(error.message.orEmpty().contains("range", ignoreCase = true))
    }

    @Test
    fun `rejects malformed utf8`() {
        val error = assertThrows(IllegalArgumentException::class.java) {
            decoder.decode(byteArrayOf(0xC3.toByte(), 0x28))
        }

        assertTrue(error.message.orEmpty().isNotBlank())
    }

    @Test
    fun `rejects missing script descriptor`() {
        val json = validArabicManifest().replace(
            Regex("""\s*\"descriptor\": \{.*?\},\s*\"defaultSchema\"""", RegexOption.DOT_MATCHES_ALL),
            "\n              \"defaultSchema\""
        )

        val error = assertThrows(IllegalArgumentException::class.java) {
            decoder.decode(json.encodeToByteArray())
        }

        assertTrue(error.message.orEmpty().contains("descriptor", ignoreCase = true))
    }

    private fun validArabicManifest(): String = """
        {
          "manifestVersion": 1,
          "identity": { "packageId": "external.arabic", "version": "1.2.3" },
          "minAppVersion": "1.0.0",
          "locale": "ar-SA",
          "displayName": { "en-US": "Arabic", "ar-SA": "العربية" },
          "defaults": {
            "script": "ARAB",
            "schema": "DIRECT",
            "layout": {
              "packageId": "external.arabic",
              "path": "layouts/arabic.json",
              "kind": "LAYOUT",
              "sha256": "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
            }
          },
          "scripts": [
            {
              "id": "ARAB",
              "descriptor": {
                "script": "ARAB", "displayNames": { "en-US": "Arabic" },
                "direction": "RTL", "layoutMirror": "MIRROR_HORIZONTAL"
              },
              "defaultSchema": "DIRECT"
            }
          ],
          "dependencies": [
            {
              "packageId": "builtin",
              "versionRange": { "minimum": "1.0.0", "maximumExclusive": "2.0.0" },
              "optional": true
            }
          ],
          "capabilities": [
            {
              "id": {
                "packageId": "external.arabic", "locale": "ar-SA",
                "script": "ARAB", "schema": "DIRECT"
              },
              "engine": { "engineId": "direct" },
              "layout": {
                "packageId": "external.arabic",
                "path": "layouts/arabic.json",
                "kind": "LAYOUT",
                "sha256": "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
              },
              "dictionaries": [],
              "candidatePolicyId": "direct_default",
              "displayPolicyId": "hidden",
              "supportsShift": true
            }
          ]
        }
    """.trimIndent()
}
