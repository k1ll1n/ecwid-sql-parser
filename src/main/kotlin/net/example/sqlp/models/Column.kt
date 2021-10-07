package net.example.sqlp.models

data class Column(
    val sqlFunc: String?,
    val table: String?,
    val name: String,
    val alias: String?,
)
