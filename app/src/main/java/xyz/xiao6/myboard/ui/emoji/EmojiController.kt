package xyz.xiao6.myboard.ui.emoji

data class EmojiState(
    val menu: EmojiMenu = EmojiMenu.EMOJI,
    val selectedCategoryIndex: Int = 0,
    val isSearching: Boolean = false,
    val query: String = "",
)

class EmojiController(
    private val catalogProvider: EmojiCatalogProvider = BuiltInEmojiCatalogProvider,
) {
    private val catalog by lazy { catalogProvider.load() }
    private var state: EmojiState = EmojiState()

    var onStateChanged: ((EmojiUiState, Boolean) -> Unit)? = null

    fun attach(onStateChanged: (EmojiUiState, Boolean) -> Unit) {
        this.onStateChanged = onStateChanged
        emit(keepPage = false)
    }

    fun refresh(keepPage: Boolean) {
        emit(keepPage)
    }

    fun selectMenu(menu: EmojiMenu) {
        if (state.menu == menu) return
        state = state.copy(menu = menu, selectedCategoryIndex = 0, isSearching = false, query = "")
        emit(keepPage = false)
    }

    fun toggleSearch() {
        state =
            if (state.isSearching) {
                state.copy(isSearching = false, query = "")
            } else {
                state.copy(isSearching = true)
            }
        emit(keepPage = false)
    }

    fun updateQuery(query: String) {
        if (state.query == query) return
        state = state.copy(query = query)
        emit(keepPage = false)
    }

    fun selectCategory(index: Int) {
        val max = currentCategories().lastIndex.coerceAtLeast(0)
        val next = index.coerceIn(0, max)
        if (state.selectedCategoryIndex == next) return
        state = state.copy(selectedCategoryIndex = next)
        emit(keepPage = false)
    }

    private fun currentCategories(): List<EmojiCategory> {
        return when (state.menu) {
            EmojiMenu.EMOJI -> catalog.emojiCategories
            EmojiMenu.KAOMOJI -> catalog.kaomojiCategories.map { EmojiCategory(it.categoryId, it.name, emptyList()) }
        }
    }

    private fun emit(keepPage: Boolean) {
        onStateChanged?.invoke(buildUiState(), keepPage)
    }

    private fun buildUiState(): EmojiUiState {
        val categories = currentCategories()
        val selectedIndex = state.selectedCategoryIndex.coerceIn(0, categories.lastIndex.coerceAtLeast(0))
        val q = state.query.trim()

        val items =
            when (state.menu) {
                EmojiMenu.EMOJI -> {
                    val current = catalog.emojiCategories.getOrNull(selectedIndex)
                    val raw = current?.items.orEmpty()
                    if (q.isBlank()) {
                        raw.map { it.emoji }
                    } else {
                        val locale = java.util.Locale.getDefault().language.lowercase(java.util.Locale.ROOT)
                        raw.filter { matchesQuery(it, q, locale) }.map { it.emoji }
                    }
                }
                EmojiMenu.KAOMOJI -> {
                    val current = catalog.kaomojiCategories.getOrNull(selectedIndex)
                    val raw = current?.items.orEmpty()
                    if (q.isBlank()) raw else raw.filter { it.contains(q, ignoreCase = true) }
                }
            }
        val gridConfig =
            when (state.menu) {
                EmojiMenu.EMOJI -> EmojiGridConfig(columns = 8, rows = 4, textSizeSp = 24f, cellHeightDp = 60f)
                EmojiMenu.KAOMOJI -> EmojiGridConfig(columns = 2, rows = 6, textSizeSp = 20f, cellHeightDp = 60f)
            }
        return EmojiUiState(
            menu = state.menu,
            categories = categories,
            selectedCategoryIndex = selectedIndex,
            items = items,
            isSearching = state.isSearching,
            query = state.query,
            gridConfig = gridConfig,
        )
    }

    private fun matchesQuery(item: EmojiItem, query: String, locale: String): Boolean {
        val q = query.lowercase(java.util.Locale.ROOT)
        val name = pickLocalizedName(item.name, locale).lowercase(java.util.Locale.ROOT)
        val keywords = pickLocalizedKeywords(item.keywords, locale)
            .joinToString(" ")
            .lowercase(java.util.Locale.ROOT)
        return name.contains(q) || keywords.contains(q) || item.emoji.contains(query)
    }

    private fun pickLocalizedName(name: EmojiName, locale: String): String {
        return when {
            locale.startsWith("zh") && name.zh.isNotBlank() -> name.zh
            name.en.isNotBlank() -> name.en
            else -> ""
        }
    }

    private fun pickLocalizedKeywords(keywords: EmojiKeywords, locale: String): List<String> {
        return when {
            locale.startsWith("zh") && keywords.zh.isNotEmpty() -> keywords.zh
            keywords.en.isNotEmpty() -> keywords.en
            else -> emptyList()
        }
    }
}
