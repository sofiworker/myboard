package xyz.xiao6.myboard.store

import android.content.Context
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArraySet
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class SettingsStore(context: Context) {
    class ChangeListener internal constructor(internal val onChange: () -> Unit)

    private val db = SettingsDatabase.getInstance(context.applicationContext)
    private val dao = db.settingsDao()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val cache: MutableMap<String, String> = ConcurrentHashMap()
    private val listeners = CopyOnWriteArraySet<ChangeListener>()

    init {
        runBlocking(Dispatchers.IO) {
            cache.putAll(dao.getAll().associate { it.key to it.value })
        }
    }

    fun getEnabledLocaleTags(): List<String> {
        val raw = readString(KEY_ENABLED_LOCALES)?.trim().orEmpty()
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
        writeString(KEY_ENABLED_LOCALES, normalized.joinToString(","))
    }

    var onboardingCompleted: Boolean
        get() = readBoolean(KEY_ONBOARDING_COMPLETED) ?: false
        set(value) {
            writeBoolean(KEY_ONBOARDING_COMPLETED, value)
        }

    var userLocaleTag: String?
        get() = readString(KEY_USER_LOCALE_TAG)
        set(value) {
            val normalized = value?.let(::normalizeLocaleTag)?.takeIf { it.isNotBlank() }
            writeString(KEY_USER_LOCALE_TAG, normalized)
        }

    var clickSoundVolumePercent: Int
        get() = (readInt(KEY_CLICK_SOUND_VOLUME_PERCENT) ?: 0).coerceIn(0, 100)
        set(value) {
            writeInt(KEY_CLICK_SOUND_VOLUME_PERCENT, value.coerceIn(0, 100))
        }

    var vibrationFollowSystem: Boolean
        get() = readBoolean(KEY_VIBRATION_FOLLOW_SYSTEM) ?: true
        set(value) {
            writeBoolean(KEY_VIBRATION_FOLLOW_SYSTEM, value)
        }

    var vibrationStrengthPercent: Int
        get() = (readInt(KEY_VIBRATION_STRENGTH_PERCENT) ?: 50).coerceIn(0, 100)
        set(value) {
            writeInt(KEY_VIBRATION_STRENGTH_PERCENT, value.coerceIn(0, 100))
        }

    var clearInputAfterTokenClear: Boolean
        get() = readBoolean(KEY_CLEAR_INPUT_AFTER_TOKEN_CLEAR) ?: false
        set(value) {
            writeBoolean(KEY_CLEAR_INPUT_AFTER_TOKEN_CLEAR, value)
        }

    var clearInputAfterTokenClearDelayMs: Int
        get() =
            (readInt(KEY_CLEAR_INPUT_AFTER_TOKEN_CLEAR_DELAY_MS) ?: DEFAULT_CLEAR_INPUT_AFTER_TOKEN_CLEAR_DELAY_MS)
                .coerceIn(MIN_CLEAR_INPUT_AFTER_TOKEN_CLEAR_DELAY_MS, MAX_CLEAR_INPUT_AFTER_TOKEN_CLEAR_DELAY_MS)
        set(value) {
            val clamped = value.coerceIn(MIN_CLEAR_INPUT_AFTER_TOKEN_CLEAR_DELAY_MS, MAX_CLEAR_INPUT_AFTER_TOKEN_CLEAR_DELAY_MS)
            writeInt(KEY_CLEAR_INPUT_AFTER_TOKEN_CLEAR_DELAY_MS, clamped)
        }

    var toolbarMaxVisibleCount: Int
        get() = (readInt(KEY_TOOLBAR_MAX_VISIBLE_COUNT) ?: 0).coerceIn(0, 12)
        set(value) {
            writeInt(KEY_TOOLBAR_MAX_VISIBLE_COUNT, value.coerceIn(0, 12))
        }

    var toolbarItemOrder: List<String>
        get() {
            val raw = readString(KEY_TOOLBAR_ITEM_ORDER)?.trim().orEmpty()
            if (raw.isBlank()) return emptyList()
            return raw.split(',')
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .distinct()
        }
        set(value) {
            val normalized = value.map { it.trim() }.filter { it.isNotBlank() }.distinct()
            writeString(KEY_TOOLBAR_ITEM_ORDER, normalized.joinToString(","))
        }

    var globalKeyboardWidthRatio: Float?
        get() {
            val v = readFloat(KEY_GLOBAL_KB_WIDTH_RATIO)
            return v?.takeIf { it.isFinite() && it > 0f }
        }
        set(value) {
            val v = value?.takeIf { it.isFinite() && it > 0f }
            writeFloat(KEY_GLOBAL_KB_WIDTH_RATIO, v)
        }

    var globalKeyboardWidthDpOffset: Float?
        get() {
            val v = readFloat(KEY_GLOBAL_KB_WIDTH_DPOFFSET)
            return v?.takeIf { it.isFinite() }
        }
        set(value) {
            val v = value?.takeIf { it.isFinite() }
            writeFloat(KEY_GLOBAL_KB_WIDTH_DPOFFSET, v)
        }

    var globalKeyboardHeightRatio: Float?
        get() {
            val v = readFloat(KEY_GLOBAL_KB_HEIGHT_RATIO)
            return v?.takeIf { it.isFinite() && it > 0f }
        }
        set(value) {
            val v = value?.takeIf { it.isFinite() && it > 0f }
            writeFloat(KEY_GLOBAL_KB_HEIGHT_RATIO, v)
        }

    var globalKeyboardHeightDpOffset: Float?
        get() {
            val v = readFloat(KEY_GLOBAL_KB_HEIGHT_DPOFFSET)
            return v?.takeIf { it.isFinite() }
        }
        set(value) {
            val v = value?.takeIf { it.isFinite() }
            writeFloat(KEY_GLOBAL_KB_HEIGHT_DPOFFSET, v)
        }

    var suggestionEnabled: Boolean
        get() = readBoolean(KEY_SUGGESTION_ENABLED) ?: true
        set(value) {
            writeBoolean(KEY_SUGGESTION_ENABLED, value)
        }

    var suggestionLearningEnabled: Boolean
        get() = readBoolean(KEY_SUGGESTION_LEARNING_ENABLED) ?: true
        set(value) {
            writeBoolean(KEY_SUGGESTION_LEARNING_ENABLED, value)
        }

    var suggestionNgramEnabled: Boolean
        get() = readBoolean(KEY_SUGGESTION_NGRAM_ENABLED) ?: false
        set(value) {
            writeBoolean(KEY_SUGGESTION_NGRAM_ENABLED, value)
        }

    var suggestionCloudEnabled: Boolean
        get() = readBoolean(KEY_SUGGESTION_CLOUD_ENABLED) ?: false
        set(value) {
            writeBoolean(KEY_SUGGESTION_CLOUD_ENABLED, value)
        }

    var suggestionCloudEndpoint: String?
        get() = readString(KEY_SUGGESTION_CLOUD_ENDPOINT)
        set(value) {
            writeString(KEY_SUGGESTION_CLOUD_ENDPOINT, value?.trim())
        }

    var suggestionCloudAuthType: String
        get() = readString(KEY_SUGGESTION_CLOUD_AUTH_TYPE) ?: "NONE"
        set(value) {
            writeString(KEY_SUGGESTION_CLOUD_AUTH_TYPE, value.trim())
        }

    var suggestionCloudAuthValue: String?
        get() = readString(KEY_SUGGESTION_CLOUD_AUTH_VALUE)
        set(value) {
            writeString(KEY_SUGGESTION_CLOUD_AUTH_VALUE, value?.trim())
        }

    var suggestionCloudHeadersJson: String?
        get() = readString(KEY_SUGGESTION_CLOUD_HEADERS_JSON)
        set(value) {
            writeString(KEY_SUGGESTION_CLOUD_HEADERS_JSON, value?.trim())
        }

    var benchmarkDisableCandidates: Boolean
        get() = readBoolean(KEY_BENCHMARK_DISABLE_CANDIDATES) ?: false
        set(value) {
            writeBoolean(KEY_BENCHMARK_DISABLE_CANDIDATES, value)
        }

    var benchmarkDisableKeyPreview: Boolean
        get() = readBoolean(KEY_BENCHMARK_DISABLE_KEY_PREVIEW) ?: false
        set(value) {
            writeBoolean(KEY_BENCHMARK_DISABLE_KEY_PREVIEW, value)
        }

    var benchmarkDisableKeyDecorations: Boolean
        get() = readBoolean(KEY_BENCHMARK_DISABLE_KEY_DECORATIONS) ?: false
        set(value) {
            writeBoolean(KEY_BENCHMARK_DISABLE_KEY_DECORATIONS, value)
        }

    var benchmarkDisableKeyLabels: Boolean
        get() = readBoolean(KEY_BENCHMARK_DISABLE_KEY_LABELS) ?: false
        set(value) {
            writeBoolean(KEY_BENCHMARK_DISABLE_KEY_LABELS, value)
        }

    var debugTouchLoggingEnabled: Boolean
        get() = readBoolean(KEY_DEBUG_TOUCH_LOGGING_ENABLED) ?: false
        set(value) {
            writeBoolean(KEY_DEBUG_TOUCH_LOGGING_ENABLED, value)
        }

    fun getPreferredLayoutId(localeTag: String): String? {
        val key = KEY_PREFERRED_LAYOUT_PREFIX + normalizeLocaleTag(localeTag)
        return readString(key)?.trim()?.takeIf { it.isNotBlank() }
    }

    fun getEnabledLayoutIds(localeTag: String): List<String> {
        val key = KEY_ENABLED_LAYOUTS_PREFIX + normalizeLocaleTag(localeTag)
        val raw = readString(key)?.trim().orEmpty()
        if (raw.isBlank()) return emptyList()
        return raw.split(',')
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
    }

    fun setEnabledLayoutIds(localeTag: String, layoutIds: List<String>) {
        val key = KEY_ENABLED_LAYOUTS_PREFIX + normalizeLocaleTag(localeTag)
        val normalized = layoutIds.map { it.trim() }.filter { it.isNotBlank() }.distinct()
        writeString(key, normalized.joinToString(","))
    }

    fun setPreferredLayoutId(localeTag: String, layoutId: String?) {
        val key = KEY_PREFERRED_LAYOUT_PREFIX + normalizeLocaleTag(localeTag)
        val value = layoutId?.trim()?.takeIf { it.isNotBlank() }
        writeString(key, value)
    }

    fun getEnabledDictionaryIds(localeTag: String): List<String>? {
        val key = KEY_ENABLED_DICTIONARIES_PREFIX + normalizeLocaleTag(localeTag)
        val raw = readString(key) ?: return null
        if (raw == DICTIONARY_EMPTY_TOKEN) return emptyList()
        if (raw.isBlank()) return emptyList()
        return raw.split(',')
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
    }

    fun setEnabledDictionaryIds(localeTag: String, dictionaryIds: List<String>) {
        val key = KEY_ENABLED_DICTIONARIES_PREFIX + normalizeLocaleTag(localeTag)
        val normalized = dictionaryIds.map { it.trim() }.filter { it.isNotBlank() }.distinct()
        if (normalized.isEmpty()) {
            writeString(key, DICTIONARY_EMPTY_TOKEN)
        } else {
            writeString(key, normalized.joinToString(","))
        }
    }

    fun clearEnabledDictionaryIds(localeTag: String) {
        val key = KEY_ENABLED_DICTIONARIES_PREFIX + normalizeLocaleTag(localeTag)
        writeString(key, null)
    }

    fun clearAll() {
        cache.clear()
        scope.launch {
            dao.clearAll()
        }
        notifyChange()
    }

    fun addOnChangeListener(onChange: () -> Unit): ChangeListener {
        val listener = ChangeListener(onChange)
        listeners.add(listener)
        return listener
    }

    fun removeOnChangeListener(listener: ChangeListener) {
        listeners.remove(listener)
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
        private const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"
        private const val KEY_USER_LOCALE_TAG = "user_locale_tag"
        private const val KEY_ENABLED_LOCALES = "enabled_locale_tags"
        private const val KEY_ENABLED_LAYOUTS_PREFIX = "enabled_layout_ids:"
        private const val KEY_PREFERRED_LAYOUT_PREFIX = "preferred_layout_id:"
        private const val KEY_ENABLED_DICTIONARIES_PREFIX = "enabled_dictionary_ids:"
        private const val DICTIONARY_EMPTY_TOKEN = "__empty__"
        private const val KEY_CLICK_SOUND_VOLUME_PERCENT = "click_sound_volume_percent"
        private const val KEY_VIBRATION_FOLLOW_SYSTEM = "vibration_follow_system"
        private const val KEY_VIBRATION_STRENGTH_PERCENT = "vibration_strength_percent"
        private const val KEY_CLEAR_INPUT_AFTER_TOKEN_CLEAR = "clear_input_after_token_clear"
        private const val KEY_CLEAR_INPUT_AFTER_TOKEN_CLEAR_DELAY_MS = "clear_input_after_token_clear_delay_ms"

        private const val DEFAULT_CLEAR_INPUT_AFTER_TOKEN_CLEAR_DELAY_MS = 350
        private const val MIN_CLEAR_INPUT_AFTER_TOKEN_CLEAR_DELAY_MS = 120
        private const val MAX_CLEAR_INPUT_AFTER_TOKEN_CLEAR_DELAY_MS = 1500
        private const val KEY_TOOLBAR_MAX_VISIBLE_COUNT = "toolbar_max_visible_count"
        private const val KEY_TOOLBAR_ITEM_ORDER = "toolbar_item_order"

        private const val KEY_GLOBAL_KB_WIDTH_RATIO = "global_keyboard_width_ratio"
        private const val KEY_GLOBAL_KB_WIDTH_DPOFFSET = "global_keyboard_width_dp_offset"
        private const val KEY_GLOBAL_KB_HEIGHT_RATIO = "global_keyboard_height_ratio"
        private const val KEY_GLOBAL_KB_HEIGHT_DPOFFSET = "global_keyboard_height_dp_offset"

        private const val KEY_SUGGESTION_ENABLED = "suggestion_enabled"
        private const val KEY_SUGGESTION_LEARNING_ENABLED = "suggestion_learning_enabled"
        private const val KEY_SUGGESTION_NGRAM_ENABLED = "suggestion_ngram_enabled"
        private const val KEY_SUGGESTION_CLOUD_ENABLED = "suggestion_cloud_enabled"
        private const val KEY_SUGGESTION_CLOUD_ENDPOINT = "suggestion_cloud_endpoint"
        private const val KEY_SUGGESTION_CLOUD_AUTH_TYPE = "suggestion_cloud_auth_type"
        private const val KEY_SUGGESTION_CLOUD_AUTH_VALUE = "suggestion_cloud_auth_value"
        private const val KEY_SUGGESTION_CLOUD_HEADERS_JSON = "suggestion_cloud_headers_json"
        private const val KEY_BENCHMARK_DISABLE_CANDIDATES = "benchmark_disable_candidates"
        private const val KEY_BENCHMARK_DISABLE_KEY_PREVIEW = "benchmark_disable_key_preview"
        private const val KEY_BENCHMARK_DISABLE_KEY_DECORATIONS = "benchmark_disable_key_decorations"
        private const val KEY_BENCHMARK_DISABLE_KEY_LABELS = "benchmark_disable_key_labels"
        private const val KEY_DEBUG_TOUCH_LOGGING_ENABLED = "debug_touch_logging_enabled"
    }

    private fun readString(key: String): String? = cache[key]

    private fun writeString(key: String, value: String?) {
        if (value.isNullOrBlank()) {
            cache.remove(key)
            scope.launch { dao.deleteByKey(key) }
        } else {
            cache[key] = value
            scope.launch { dao.upsert(SettingsEntry(key, value)) }
        }
        notifyChange()
    }

    private fun readInt(key: String): Int? = cache[key]?.toIntOrNull()

    private fun writeInt(key: String, value: Int) {
        writeString(key, value.toString())
    }

    private fun readBoolean(key: String): Boolean? = cache[key]?.toBooleanStrictOrNull()

    private fun writeBoolean(key: String, value: Boolean) {
        writeString(key, value.toString())
    }

    private fun readFloat(key: String): Float? = cache[key]?.toFloatOrNull()

    private fun writeFloat(key: String, value: Float?) {
        writeString(key, value?.toString())
    }

    private fun notifyChange() {
        listeners.forEach { it.onChange() }
    }
}
