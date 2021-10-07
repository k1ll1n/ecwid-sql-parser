package net.example.sqlp.utils

fun String.hasJoin() = this.contains("join")
fun String.hasWhere() = this.contains("where")
fun String.hasGroup() = this.contains("group")
fun String.hasHaving() = this.contains("having")
fun String.hasOrder() = this.contains("order")
fun String.hasLimit() = this.contains("limit")
fun String.hasOffset() = this.contains("offset")
fun String.hasNestedSelect() = this.contains("select")
fun String.hasAliasInColumn(): Boolean = this.contains(" as ")
fun String.hasDotInColumn(): Boolean = this.contains(".")
fun String.hasSqlFuncInColumn(): Boolean = this.contains("(") && this.contains(")")