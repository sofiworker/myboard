package xyz.xiao6.myboard.pack

import android.content.ContentResolver
import android.net.Uri
import java.io.InputStream

fun interface LanguagePackDocumentSource {
    fun open(uri: Uri): InputStream?
}

class ContentResolverLanguagePackDocumentSource(
    private val contentResolver: ContentResolver
) : LanguagePackDocumentSource {
    override fun open(uri: Uri): InputStream? = contentResolver.openInputStream(uri)
}
