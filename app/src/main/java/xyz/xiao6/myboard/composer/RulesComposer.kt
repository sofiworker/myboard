package xyz.xiao6.myboard.composer

class RulesComposer(
    override val id: String,
    private val rules: Map<String, String>,
) : Composer {
    override val toRead: Int = (rules.keys.maxOfOrNull { it.length } ?: 1).coerceAtLeast(1) - 1

    private val ruleOrder: List<String> = rules.keys.sortedByDescending { it.length }

    override fun getActions(precedingText: String, toInsert: String): Pair<Int, String> {
        val str = precedingText + toInsert
        val lower = str.lowercase()
        for (key in ruleOrder) {
            if (lower.endsWith(key)) {
                val value = rules.getValue(key)
                return (key.length - 1) to value
            }
        }
        return 0 to toInsert
    }
}
