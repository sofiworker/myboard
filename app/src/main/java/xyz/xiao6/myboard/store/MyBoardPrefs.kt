package xyz.xiao6.myboard.store

import android.content.Context
import java.util.Locale

class MyBoardPrefs(context: Context) {
    private val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getEnabledLocaleTags(): List<String> {
        val raw = sp.getString(KEY_ENABLED_LOCALES, null)?.trim().orEmpty()
        if (raw.isBlank()) return emptyList()
        return raw.split(',')
            .mapNotNull { it.trim().takeIf { t -> t.isNotBlank() }?.let(::normalizeLocaleTag) }
            .filter { it.isNotBlank() }
            .distinct()
    }

    fun setEnabledLocaleTags(localeTags: List<String>) {
        val normalized = localeTags.mapNotNull { it.trim().takeIf { t -> t.isNotBlank() }?.let(::normalizeLocaleTag) }
            .filter { it.isNotBlank() }
            .distinct()
        sp.edit().putString(KEY_ENABLED_LOCALES, normalized.joinToString(",")).apply()
    }

    var onboardingCompleted: Boolean
        get() = sp.getBoolean(KEY_ONBOARDING_COMPLETED, false)
        set(value) {
            sp.edit().putBoolean(KEY_ONBOARDING_COMPLETED, value).apply()
        }

    var userLocaleTag: String?
        get() = sp.getString(KEY_USER_LOCALE_TAG, null)
        set(value) {
            val normalized = value?.let(::normalizeLocaleTag)?.takeIf { it.isNotBlank() }
            sp.edit().putString(KEY_USER_LOCALE_TAG, normalized).apply()
        }

    var clickSoundVolumePercent: Int
        get() = sp.getInt(KEY_CLICK_SOUND_VOLUME_PERCENT, 0).coerceIn(0, 100)
        set(value) {
            sp.edit().putInt(KEY_CLICK_SOUND_VOLUME_PERCENT, value.coerceIn(0, 100)).apply()
        }

    var vibrationFollowSystem: Boolean
        get() = sp.getBoolean(KEY_VIBRATION_FOLLOW_SYSTEM, true)
        set(value) {
            sp.edit().putBoolean(KEY_VIBRATION_FOLLOW_SYSTEM, value).apply()
        }

    var vibrationStrengthPercent: Int
        get() = sp.getInt(KEY_VIBRATION_STRENGTH_PERCENT, 50).coerceIn(0, 100)
        set(value) {
            sp.edit().putInt(KEY_VIBRATION_STRENGTH_PERCENT, value.coerceIn(0, 100)).apply()
        }

    fun getPreferredLayoutId(localeTag: String): String? {
        val key = KEY_PREFERRED_LAYOUT_PREFIX + normalizeLocaleTag(localeTag)
        return sp.getString(key, null)?.trim()?.takeIf { it.isNotBlank() }
    }

    fun getEnabledLayoutIds(localeTag: String): List<String> {
        val key = KEY_ENABLED_LAYOUTS_PREFIX + normalizeLocaleTag(localeTag)
        val raw = sp.getString(key, null)?.trim().orEmpty()
        if (raw.isBlank()) return emptyList()
        return raw.split(',')
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
    }

    fun setEnabledLayoutIds(localeTag: String, layoutIds: List<String>) {
        val key = KEY_ENABLED_LAYOUTS_PREFIX + normalizeLocaleTag(localeTag)
        val normalized = layoutIds.map { it.trim() }.filter { it.isNotBlank() }.distinct()
        sp.edit().putString(key, normalized.joinToString(",")).apply()
    }

    fun setPreferredLayoutId(localeTag: String, layoutId: String?) {
        val key = KEY_PREFERRED_LAYOUT_PREFIX + normalizeLocaleTag(localeTag)
        val value = layoutId?.trim()?.takeIf { it.isNotBlank() }
        sp.edit().putString(key, value).apply()
    }

    fun clearAll() {
        sp.edit().clear().apply()
    }

    private fun normalizeLocaleTag(tag: String): String {
        val t = tag.trim().replace('_', '-')
        val parts = t.split('-').filter { it.isNotBlank() }
        if (parts.isEmpty()) return ""
        val language = parts[0].lowercase(Locale.ROOT)
        val region = parts.getOrNull(1)?.uppercase(Locale.ROOT)
        return if (region.isNullOrBlank()) language else "$language-$region"
    }

    private companion object {
        private const val PREFS_NAME = "myboard_prefs"
        private const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"
        private const val KEY_USER_LOCALE_TAG = "user_locale_tag"
        private const val KEY_ENABLED_LOCALES = "enabled_locale_tags"
        private const val KEY_ENABLED_LAYOUTS_PREFIX = "enabled_layout_ids:"
        private const val KEY_PREFERRED_LAYOUT_PREFIX = "preferred_layout_id:"
        private const val KEY_CLICK_SOUND_VOLUME_PERCENT = "click_sound_volume_percent"
        private const val KEY_VIBRATION_FOLLOW_SYSTEM = "vibration_follow_system"
        private const val KEY_VIBRATION_STRENGTH_PERCENT = "vibration_strength_percent"
    }
}
