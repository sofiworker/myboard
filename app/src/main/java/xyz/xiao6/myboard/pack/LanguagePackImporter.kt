package xyz.xiao6.myboard.pack

import java.io.InputStream
import xyz.xiao6.myboard.contract.manifest.LanguagePackManifest

fun interface LanguagePackManifestDecoder {
    fun decode(bytes: ByteArray): LanguagePackManifest
}

interface LanguagePackImporter {
    suspend fun import(
        input: InputStream,
        manifestDecoder: LanguagePackManifestDecoder
    ): PackageOperationResult
}

class TransactionalLanguagePackImporter(
    private val packageStore: PackageStore,
    private val archiveStager: PackageArchiveStager = PackageArchiveStager()
) : LanguagePackImporter {
    override suspend fun import(
        input: InputStream,
        manifestDecoder: LanguagePackManifestDecoder
    ): PackageOperationResult = runCatching {
        archiveStager.stage(input, manifestDecoder::decode)
    }.fold(
        onSuccess = packageStore::install,
        onFailure = { error ->
            PackageOperationResult.Failure(
                listOf(error.message ?: "Language package could not be staged")
            )
        }
    )
}
