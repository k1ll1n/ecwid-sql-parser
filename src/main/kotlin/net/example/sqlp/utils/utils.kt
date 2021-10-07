package net.example.sqlp.utils

fun getAnchorForFromEnd(sql: String): Int =
    listOf("inner join", "cross join", "left join", "right join", "full outer join", "where", "group", "having", "order", "limit", "offset")
        .mapNotNull {
            val position = sql.indexOf(it)
            if (position > 0)  position else null
        }.minOrNull() ?: sql.length

fun getGag(value: String, char: String = "_"): String =
    StringBuilder().apply {
        repeat(value.length) { append(char)  }
    }.toString()

fun temporarilyReplaceNestedSelectsOnGag(sql: String): String {
    tailrec fun replace(target: String, values: List<String>?, currentPosition: Int = 0): String =
        when {
            values == null -> target
            values.size - 1 < currentPosition -> target
            else -> {
                val value = values[currentPosition]
                replace(target.replace(value, getGag(value)), values, currentPosition + 1)
            }
        }

    return replace(sql, "\\(.*.\\)".toRegex().find(sql)?.groupValues)
}

fun getStartPositionForJoins(sql: String): Int =
    listOf("inner join", "cross join", "left join", "right join", "full outer join")
        .map { sql.indexOf(it) }
        .filter { it > 0 }
        .sortedBy { it }
        .first()

fun clearQueryForHaving(sql: String, count: Int = 0): String {
    val anchors = listOf("order", "limit", "offset")
    if (count == anchors.size) return sql

    return clearQueryForHaving(sql.replace("${anchors[count]} .*".toRegex(), ""), count + 1)
}

fun clearQueryForSorting(sql: String, count: Int = 0): String {
    val anchors = listOf("limit", "offset")
    if (count == anchors.size) return sql
    return clearQueryForSorting(sql.replace("${anchors[count]} .*".toRegex(), ""), count + 1)
}

fun fixJoins(sql: String): String {
    tailrec fun replaceOnGag(sql: String, count: Int = 0): String {
        val anchors = listOf("where", "group", "having", "order", "limit", "offset", "inner join", "cross join", "left join", "right join", "full outer join")
        if (count == anchors.size) return sql

        val gag = getGag(anchors[count])
        return replaceOnGag(sql.replace(anchors[count], gag), count + 1)
    }
    val tmpSql = temporarilyReplaceNestedSelectsOnGag(sql)
    val afterReplace = replaceOnGag(tmpSql)

    if (!afterReplace.contains(" join "))  return sql

    val buf = StringBuffer(sql)
    val start = tmpSql.indexOf(" join ")
    val end = start + " join ".length
    buf.replace(start, end, " inner join ")
    return buf.toString()
}

fun getJoinType(stringJoin: String): String {
    val joinTypes = listOf("inner join", "cross join", "left join", "right join", "full outer join")

    joinTypes.forEach {type ->
        if (stringJoin.contains(type)) {
            return type.replace("join", "").trim()
        }
    }

    return "inner"
}