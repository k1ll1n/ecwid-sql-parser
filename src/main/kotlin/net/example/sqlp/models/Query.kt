package net.example.sqlp.models

data class Query(
    val source: String,
    val columns: List<Column>,
    val mainTable: List<MainTable>,
    val joins: List<Join>,
    val whereConditions: List<Condition>,
    val groupByColumns: List<Column>,
    val havingConditions: List<HavingCondition>,
    val sortConditions: List<SortCondition>,
    val limit: Int,
    val offset: Int
)