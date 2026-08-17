package com.example.fgfchallenge.feature.logs.data.local

import androidx.room.RoomRawQuery
import androidx.sqlite.SQLiteStatement
import com.example.fgfchallenge.feature.logs.data.model.LogQuery
import com.example.fgfchallenge.feature.logs.data.model.LogSortDirection

/**
 * Builds every statement the log viewer runs against Room from one [LogQuery].
 *
 * The point of this file is parity: [pagedSelect] and [severityCountSelect] share
 * [whereClause], so the summary cannot count a different set of rows than the list shows. Adding
 * a condition to one path adds it to both by construction rather than by discipline.
 *
 * No user value is ever concatenated into SQL. The statement text is assembled from constants and
 * generated `?` placeholders only; values are bound positionally in [Predicate.bindTo].
 */
internal object LogQuerySql {
    /**
     * Escape character for `LIKE`. Backslash is not special to SQLite string literals, so it needs
     * no doubling inside the `ESCAPE` clause below.
     */
    private const val LIKE_ESCAPE_CHARACTER = '\\'

    /**
     * Rows for one page of results, newest or oldest first. Room appends its own `LIMIT`/`OFFSET`
     * to this statement when it drives a `PagingSource`, so no paging bounds appear here.
     */
    fun pagedSelect(query: LogQuery): RoomRawQuery {
        val predicate = whereClause(query)
        val direction = if (query.sortDirection == LogSortDirection.NewestFirst) "DESC" else "ASC"
        // The ID is the tie-breaker so equal timestamps cannot reorder between two loads of the
        // same page — Paging would otherwise duplicate or skip rows across a page boundary.
        val sql =
            "SELECT * FROM ${LogEntity.TABLE_NAME}" +
                predicate.sqlSuffix +
                " ORDER BY ${LogEntity.COLUMN_TIMESTAMP} $direction, ${LogEntity.COLUMN_ID} $direction"
        return RoomRawQuery(sql) { statement -> predicate.bindTo(statement) }
    }

    /**
     * One row per severity present in the complete filtered result. The total is the sum of those
     * counts, so it and the per-severity counts can never disagree.
     */
    fun severityCountSelect(query: LogQuery): RoomRawQuery {
        val predicate = whereClause(query)
        val sql =
            "SELECT ${LogEntity.COLUMN_SEVERITY}, COUNT(*) AS ${SeverityCountRow.COLUMN_COUNT}" +
                " FROM ${LogEntity.TABLE_NAME}" +
                predicate.sqlSuffix +
                " GROUP BY ${LogEntity.COLUMN_SEVERITY}"
        return RoomRawQuery(sql) { statement -> predicate.bindTo(statement) }
    }

    /**
     * Translates the active conditions of [query] into one `WHERE` clause.
     *
     * An inactive category contributes no SQL at all rather than a tautology, so the query planner
     * sees only the restrictions that exist. Active categories are joined with `AND`; the `OR`s
     * live inside a category (message-or-ID, and the `IN` lists).
     */
    private fun whereClause(query: LogQuery): Predicate {
        val conditions = mutableListOf<String>()
        val arguments = mutableListOf<Any>()

        val search = query.literalSearch
        if (search.isNotBlank()) {
            // Case folding is SQLite's ASCII-only `LIKE` behavior, which matches the fixture's
            // ASCII messages and IDs; the escape clause is what keeps `%` and `_` literal.
            val like = "LIKE ? ESCAPE '$LIKE_ESCAPE_CHARACTER'"
            conditions += "(${LogEntity.COLUMN_MESSAGE} $like OR ${LogEntity.COLUMN_ID} $like)"
            val pattern = search.toLiteralContainsPattern()
            arguments.add(pattern)
            arguments.add(pattern)
        }

        if (query.selectedTags.isNotEmpty()) {
            conditions += "${LogEntity.COLUMN_TAG} IN (${placeholders(query.selectedTags.size)})"
            arguments.addAll(query.selectedTags)
        }

        if (query.selectedSeverities.isNotEmpty()) {
            conditions +=
                "${LogEntity.COLUMN_SEVERITY} IN (${placeholders(query.selectedSeverities.size)})"
            arguments.addAll(query.selectedSeverities.map { it.name })
        }

        query.aiGeneratedConstraint?.let { isAiGenerated ->
            conditions += "${LogEntity.COLUMN_IS_AI_GENERATED} = ?"
            arguments.add(if (isAiGenerated) 1L else 0L)
        }

        query.startInclusiveUtc?.let { start ->
            conditions += "${LogEntity.COLUMN_TIMESTAMP} >= ?"
            arguments.add(start.toEpochMilli())
        }

        query.endExclusiveUtc?.let { end ->
            conditions += "${LogEntity.COLUMN_TIMESTAMP} < ?"
            arguments.add(end.toEpochMilli())
        }

        query.minimumLatencyInclusive?.let { minimum ->
            conditions += "${LogEntity.COLUMN_LATENCY_MS} >= ?"
            arguments.add(minimum)
        }

        query.maximumLatencyInclusive?.let { maximum ->
            conditions += "${LogEntity.COLUMN_LATENCY_MS} <= ?"
            arguments.add(maximum)
        }

        return Predicate(
            sqlSuffix = if (conditions.isEmpty()) "" else " WHERE ${conditions.joinToString(" AND ")}",
            arguments = arguments,
        )
    }

    private fun placeholders(count: Int): String = List(count) { "?" }.joinToString(", ")

    /**
     * Turns raw search text into a `LIKE` pattern that matches it as a literal substring.
     *
     * The wildcards are escaped before the surrounding `%` are added, so searching `%_` looks for
     * those two characters instead of matching every row. The escape character itself is escaped
     * first, or escaping would corrupt a search containing a backslash.
     */
    private fun String.toLiteralContainsPattern(): String {
        val escaped =
            buildString(length) {
                for (character in this@toLiteralContainsPattern) {
                    if (character == LIKE_ESCAPE_CHARACTER || character == '%' || character == '_') {
                        append(LIKE_ESCAPE_CHARACTER)
                    }
                    append(character)
                }
            }
        return "%$escaped%"
    }

    /**
     * A `WHERE` clause and the values its placeholders expect, in order. Keeping them together is
     * what makes an argument/placeholder mismatch impossible to introduce from a call site.
     */
    private class Predicate(
        val sqlSuffix: String,
        val arguments: List<Any>,
    ) {
        fun bindTo(statement: SQLiteStatement) {
            arguments.forEachIndexed { position, argument ->
                // SQLite binds from 1. Only these two types are ever produced above: text for
                // search patterns, tags, and severity names; integers for everything else.
                val index = position + 1
                when (argument) {
                    is String -> statement.bindText(index, argument)
                    is Long -> statement.bindLong(index, argument)
                    else -> error("Unsupported bind argument: ${argument::class.simpleName}")
                }
            }
        }
    }
}
