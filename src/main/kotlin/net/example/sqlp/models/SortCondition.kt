package net.example.sqlp.models

data class SortCondition(
    val column: Column,
    val direction: String? = null
)