package net.example.sqlp.models

data class Join(
    val type: String,
    val table: String?,
    val alias: String?,
    val connectColumns: Pair<Column, Column>,
    val joinWithSelect: Boolean,
    val selectQuery: Query?
)
