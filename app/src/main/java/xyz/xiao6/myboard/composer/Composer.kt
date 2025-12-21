package xyz.xiao6.myboard.composer

/**
 * Composer transforms recent input characters into composed text.
 * It does not touch dictionaries; it only rewrites the input stream.
 */
interface Composer {
    val id: String
    val toRead: Int

    /**
     * @return Pair(deleteCount, replacementText)
     */
    fun getActions(precedingText: String, toInsert: String): Pair<Int, String>
}

object AppenderComposer : Composer {
    override val id: String = "appender"
    override val toRead: Int = 0

    override fun getActions(precedingText: String, toInsert: String): Pair<Int, String> {
        return 0 to toInsert
    }
}
