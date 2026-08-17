package com.example.fgfchallenge.feature.logs.presentation.model

/**
 * Chronological order the result set is presented in.
 *
 * State stores the order rather than its label so the screen resolves the localized sort copy and
 * presentation holds no UI strings. This milestone only tracks the selected order; actually
 * reordering the rows belongs to the processing pipeline in Roadmap #7.
 */
internal enum class LogSortOrder {
    NewestFirst,
    OldestFirst,
    ;

    fun toggled(): LogSortOrder =
        when (this) {
            NewestFirst -> OldestFirst
            OldestFirst -> NewestFirst
        }
}
