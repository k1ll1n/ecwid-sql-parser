package net.example.sqlp.models

data class HavingCondition(
    val column: Column,
    val operator: String,
    val value: String,
)