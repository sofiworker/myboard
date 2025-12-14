package xyz.xiao6.myboard.controller

/**
 * 候选引擎：根据当前输入内容返回候选词/候选字。
 * Candidate engine: returns candidates for current input.
 */
fun interface CandidateEngine {
    fun query(lastCommittedText: String): List<String>
}

/**
 * 最简实现：仅在输入“符号字符”时给出候选（用于演示候选栏按需显示）。
 * Minimal demo: only returns candidates for symbol characters to demonstrate on-demand candidate bar.
 */
object SimpleSymbolCandidateEngine : CandidateEngine {
    override fun query(lastCommittedText: String): List<String> {
        if (lastCommittedText.length != 1) return emptyList()
        val c = lastCommittedText[0]
        if (c.isLetterOrDigit() || c.isWhitespace()) return emptyList()

        return when (c) {
            '.' -> listOf("…", "。")
            ',' -> listOf("，")
            '?' -> listOf("？")
            '!' -> listOf("！")
            ':' -> listOf("：")
            ';' -> listOf("；")
            '@' -> listOf("＠", "📧")
            '#' -> listOf("＃")
            '$' -> listOf("￥", "€", "£")
            '%' -> listOf("％")
            '&' -> listOf("＆")
            '*' -> listOf("×", "⭐")
            '+' -> listOf("＋")
            '-' -> listOf("－", "—")
            '/' -> listOf("／")
            '\\' -> listOf("＼")
            '(' -> listOf("（")
            ')' -> listOf("）")
            '[' -> listOf("【")
            ']' -> listOf("】")
            '{' -> listOf("｛")
            '}' -> listOf("｝")
            else -> emptyList()
        }
    }
}

