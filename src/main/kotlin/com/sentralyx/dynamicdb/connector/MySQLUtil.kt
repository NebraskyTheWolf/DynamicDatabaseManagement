package com.sentralyx.dynamicdb.connector

import com.sentralyx.dynamicdb.annotations.ColumnType
import com.sentralyx.dynamicdb.annotations.PrimaryKey
import com.sentralyx.dynamicdb.annotations.Unique
import java.sql.Connection
import java.sql.ResultSet
import javax.lang.model.element.Element
import javax.lang.model.element.ElementKind

/**
 * Checks if a table exists in the database, and creates it if it does not.
 *
 * @param connection The database connection to use for checking and creating the table.
 * @param tableName The name of the table to check and create.
 * @param element The element representing the database entity which contains field information.
 */
fun checkAndCreateTable(connection: Connection, tableName: String, element: Element) {
    if (!doesTableExist(connection, tableName)) {
        createTable(connection, tableName, element)
    }
}

/**
 * Checks if a table exists in the database.
 *
 * @param connection The database connection to use for checking the table existence.
 * @param tableName The name of the table to check.
 * @return `true` if the table exists, `false` otherwise.
 */
fun doesTableExist(connection: Connection, tableName: String): Boolean {
    val meta = connection.metaData
    val resultSet: ResultSet = meta.getTables(null, null, tableName, null)
    return resultSet.next()
}

/**
 * Creates a table in the database based on the provided entity element.
 *
 * @param connection The database connection to use for creating the table.
 * @param tableName The name of the table to create.
 * @param element The element representing the database entity which contains field information.
 * @throws IllegalArgumentException If a field does not have a valid SQLType annotation.
 */
fun createTable(connection: Connection, tableName: String, element: Element) {
    val fields = element.enclosedElements.filter { it.kind == ElementKind.FIELD }

    val fieldsSql = fields.joinToString(", ") { field ->
        val fieldName = field.simpleName.toString()
        val sqlType = field.getAnnotation(ColumnType::class.java)
        val isPrimaryKey = field.getAnnotation(PrimaryKey::class.java) != null
        val isUnique = field.getAnnotation(Unique::class.java) != null

        val baseSqlType = sqlType?.type?.name ?: throw IllegalArgumentException("Field $fieldName must have a SQLType annotation")
        val size = sqlType.size

        val constraints = mutableListOf<String>()
        if (isPrimaryKey) constraints.add("PRIMARY KEY")
        if (isUnique) constraints.add("UNIQUE")

        "$fieldName $baseSqlType($size) ${constraints.joinToString(" ")}".trim()
    }

    val sql = "CREATE TABLE $tableName ($fieldsSql)"
    connection.createStatement().use { statement ->
        statement.executeUpdate(sql)
    }
}
