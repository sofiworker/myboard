package xyz.xiao6.myboard.decoder

import java.util.Locale

internal fun normalizePinyinKey(rawKey: String): String {
    return rawKey.trim()
        .replace("'", "")
        .replace(" ", "")
        .lowercase(Locale.ROOT)
        .filter { it in 'a'..'z' }
}

