package net.example.sqlp

import net.example.sqlp.models.*
import net.example.sqlp.utils.*

class SQLParser {
    fun parse(sqlQuery: String): Query {
        val queryString = sqlQuery.lowercase()

        val columns = getQueryColumns(queryString)

        val cutePositionAfterMainTable = getAnchorForFromEnd(queryString)
        if (cutePositionAfterMainTable == -1) throw IllegalStateException("Не удалось определить основную таблицу")

        val mainTable = getMainTable(queryString, cutePositionAfterMainTable)

        val joins: List<Join> = if (queryString.hasJoin()) {
            val fixedQueryString = fixJoins(queryString)
            val stringJoins = getStringJoins(fixedQueryString)
            getJoins(stringJoins)
        } else emptyList()

        val whereConditions: List<Condition> = if (queryString.hasWhere()) {
            getWhereConditions(queryString)
        } else emptyList()

        val groupByColumns: List<Column> = if (queryString.hasGroup()) {
            getGroupByColumns(queryString)
        } else emptyList()

        val havingConditions: List<HavingCondition> = if (queryString.hasHaving()) {
            getHavingConditions(queryString)
        } else emptyList()

        val sortConditions: List<SortCondition> = if (queryString.hasOrder()) {
            getSortingConditions(queryString)
        } else emptyList()

        val limit = if (queryString.hasLimit()) {
            getLimit(queryString)
        } else 0

        val offset = if (queryString.hasOffset()) {
            getOffset(queryString)
        } else 0

        return Query(sqlQuery, columns, mainTable, joins, whereConditions,
            groupByColumns, havingConditions, sortConditions, limit, offset)
    }

    private fun getQueryColumns(queryString: String): List<Column> {
        val stringColumns = queryString
            .substring("select".let { queryString.indexOf(it) + it.length }, queryString.indexOf("from"))
            .replace("\\s+".toRegex(), " ")
            .replace(",\\s".toRegex(), ",")
            .trim()

        return stringColumns.split(",").map {
            getColumn(it)
        }
    }

    private fun getColumn(stringColumn: String): Column {
        val alias = if (stringColumn.hasAliasInColumn()) {
            getAliasForColumn(stringColumn)
        } else null

        val func = if (stringColumn.hasSqlFuncInColumn()) {
            getFuncForColumn(stringColumn)
        } else null

        val clearedColumn = getClearedColumn(stringColumn)
        val table = getColumnTable(clearedColumn)
        val name = getColumnName(clearedColumn)

        return Column(func, table, name, alias)
    }

    private fun getAliasForColumn(strColumn: String): String {
        val pair = strColumn.split(" as ")
        return pair.last().trim()
    }

    private fun getFuncForColumn(strColumn: String): String {
        val column = strColumn.split(" as ").first().trim()
        val pair = column.split("(")
        return pair.first().trim()
    }

    private fun getClearedColumn(strColumn: String): String =
        strColumn.split(" as ").first().trim()
            .split("(").last().replace(")", "").trim()

    private fun getColumnName(strColumn: String): String {
        if (strColumn.hasDotInColumn()) return strColumn.split(".").last()

        return strColumn
    }

    private fun getColumnTable(strColumn: String): String? {
        if (strColumn.hasDotInColumn()) return strColumn.split(".").first()

        return null
    }

    private fun getMainTable(queryString: String, endPosition: Int): List<MainTable> =
        queryString.substring("from".let { queryString.indexOf(it) + it.length }, endPosition).trim()
            .split(",")
            .map { table -> table.trim() }
            .map { table ->
                if (table.contains(" ")) {
                    val (name, alias) = table.split(" ")
                    MainTable(name, alias)
                } else {
                    MainTable(table, null)
                }
            }

    private fun getStringJoins(sql: String): List<String> {
        val fixedString = sql.replace("\\s+".toRegex(), " ")
        val clearedString = getClearedJoinsString(fixedString)

        tailrec fun setDividerToJoins(sql: String, count: Int = 0): String  {
            val anchors = listOf("inner join", "cross join", "left join", "right join", "full outer join")
            if (count == anchors.size) return sql

            return setDividerToJoins(sql.replace(anchors[count], "#join#${anchors[count]}"), count + 1)
        }

        return setDividerToJoins(clearedString).trim().split("#join#")
    }

    private fun getClearedJoinsString(sql: String): String =
        getStartPositionForJoins(sql).let { startPosition ->
            sql.substring(startPosition, sql.length)
        }

    private fun getJoins(stringJoins: List<String>): List<Join> =
        stringJoins
            .filter { it.isNotEmpty() }
            .map { join ->
                val joinType = getJoinType(join)

                val stringJoin: String = join.split("join").last().trim()

                val joinPair = stringJoin.split(" on ")

                val tableInfo = joinPair.first()
                val (c1, c2) = joinPair.last().split(" = ")
                val connectColumns = Pair(getColumn(c1), getColumn(c2))

                if (!tableInfo.hasNestedSelect()) {
                    getJoinWithoutSelect(tableInfo, joinType, connectColumns)
                } else {
                    getJoinWithSelect(tableInfo, joinType, connectColumns)
                }
            }

    private fun getJoinWithoutSelect(joinLeftSide: String, joinType: String, connectColumns: Pair<Column, Column>): Join {
        val joinTable: String
        val joinAlias: String?
        if (joinLeftSide.contains(" ")) {
            val (table, alias) = joinLeftSide.split(" ")
            joinTable = table
            joinAlias = alias
        } else {
            joinTable = joinLeftSide
            joinAlias = null
        }
        val joinWithSelect = false
        val joinQuery = null

        return Join(joinType, joinTable, joinAlias, connectColumns, joinWithSelect, joinQuery)
    }

    private fun getJoinWithSelect(joinLeftSide: String, joinType: String, connectColumns: Pair<Column, Column>): Join {
        val select: String
        val joinAlias: String?

        if (joinLeftSide.contains("(select")) {
            joinAlias = joinLeftSide.split(") ").last()
            select = joinLeftSide.split(") ").first().replace("(", "")
        } else {
            joinAlias = null
            select = joinLeftSide
        }

        val joinWithSelect = true
        val joinQuery = SQLParser().parse(select)
        return Join(joinType, null, joinAlias, connectColumns, joinWithSelect, joinQuery)
    }

    private fun getWhereConditions(sql: String): List<Condition> {
        val clearedString = clearWhereString(sql)

        val stringConditions: List<String> = clearedString
            .substring("where".let { clearedString.indexOf(it) + it.length })
            .replace("\\s+".toRegex(), " ")
            .trim()
            .split(" and ")

        return stringConditions
            .mapNotNull { source ->
                val pattern = "\\s(\\W|\\D|\\S)\\s".toRegex()
                pattern.find(source)?.value?.let { operator -> source to operator }
            }
            .map { (source, operator) ->
                val pair = source.split(operator)
                Condition(key = pair.first(), operator.trim(), value = pair.last())
            }
    }

    private fun clearWhereString(sql: String, count: Int = 0): String {
        val anchors = listOf("group", "having", "order", "limit", "offset")
        if (count == anchors.size) return sql

        return clearWhereString(sql.replace("${anchors[count]} .*".toRegex(), ""), count + 1)
    }

    private fun getGroupByColumns(sql: String): List<Column> {
        val clearedString = clearGroupByString(sql)

        return clearedString.substring("group by".let { clearedString.indexOf(it) + it.length })
            .replace(",\\s".toRegex(), ",")
            .trim()
            .split(",")
            .map { getColumn(it) }
    }

    private fun clearGroupByString(sql: String, count: Int = 0): String {
        val anchors = listOf("having", "order", "limit", "offset")
        if (count == anchors.size) return sql

        return clearGroupByString(sql.replace("${anchors[count]} .*".toRegex(), ""), count + 1)
    }

    private fun getHavingConditions(sql: String): List<HavingCondition> {
        val tmpSql = clearQueryForHaving(sql)

        return tmpSql
            .substring("having".let { tmpSql.indexOf(it) + it.length })
            .trim()
            .split(" and ")
            .mapNotNull { source ->
                val pattern = "\\s(\\W|\\D|\\S)\\s".toRegex()
                pattern.find(source)?.value?.let { operator -> source to operator }
            }
            .map { (source, operator) ->
                val pair = source.split(operator)
                val column = getColumn(pair.first())
                HavingCondition(column = column, operator.trim(), value = pair.last())
            }
    }

    private fun getSortingConditions(sql: String): List<SortCondition> {
        val tmpSql = clearQueryForSorting(sql)
        return tmpSql
            .substring("order by".let { tmpSql.indexOf(it) + it.length })
            .trim()
            .split(",")
            .map {
                if (it.contains("desc") || it.contains("asc")) {
                    val sortPair = it.split(" ")
                    val direction = sortPair.last()
                    val stringColumn = sortPair.first()
                    val column: Column = getColumn(stringColumn)

                    SortCondition(column, direction)
                } else {
                    val column: Column = getColumn(it)

                    SortCondition(column)
                }
            }
    }

    private fun getLimit(sql: String): Int {
        val value = "limit \\d{1,}".toRegex().find(sql)?.value
        return value?.split(" ")?.last()?.toInt() ?: 0
    }

    private fun getOffset(sql: String): Int {
        val value = "offset \\d{1,}".toRegex().find(sql)?.value
        return value?.split(" ")?.last()?.toInt() ?: 0
    }
}