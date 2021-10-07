package net.example.sqlp

import net.example.sqlp.models.Query

fun main() {
    val sqlExample = """SELECT
                               a.id as complex_id, thumbnail_original, title, a.completion_decade, a.completion_year,
                               b.min_price, a.address, d.name as house_material, c.floor_count, e.name as metro,
                               a.comfort_class, f.name as district, count(book.id), sum(book.cost) 
                        FROM residential_complexes a, test1 x, test2 z
                        LEFT JOIN houses c ON c.residential_complex_id = a.id
                        JOIN (SELECT id, name FROM house_materials) d ON d.id = c.house_material_id
                        LEFT JOIN metro_stations e on e.id = a.metro_station_id
                        LEFT JOIN districts f on a.district_id = f.id
                        WHERE
                              status = 1
                          AND
                              developer_id = 132
                        GROUP BY a.id, b.min_price, house_material, c.floor_count
                        HAVING COUNT(*) > 1 AND SUM(book.cost) > 500
                        ORDER BY country DESC
                        OFFSET 20
                        LIMIT 10;"""

    val sqlParser = SQLParser()
    val res: Query = sqlParser.parse(sqlExample)
    println(res.columns)
    println("###########################")
    println(res.mainTable)
    println("###########################")
    println(res.joins)
    println("###########################")
    println(res.whereConditions)
    println("###########################")
    println(res.groupByColumns)
    println("###########################")
    println(res.havingConditions)
    println("###########################")
    println(res.sortConditions)
    println("###########################")
    println(res.limit)
    println("###########################")
    println(res.offset)
    println("###########################")
}