package xyz.xiao6.myboard.theme.foundation

enum class KeyStyleRole(val ref: String) {
    DEFAULT("key_default"),
    FUNCTION("key_function"),
    ACTION("key_action"),
    SPACE("key_space"),
    CANDIDATE("key_candidate");

    companion object {
        val fallbackRef: String
            get() = DEFAULT.ref

        fun fromRef(ref: String): KeyStyleRole? =
            entries.firstOrNull { it.ref == ref }
    }
}

enum class FeedbackTokenId(
    val ref: String,
    val soundResName: String = ref
) {
    KEY_TAP("key_tap"),
    KEY_LONG_PRESS("key_long_press"),
    KEY_ACTION("key_action"),
    KEY_SPACE("key_space")
}
