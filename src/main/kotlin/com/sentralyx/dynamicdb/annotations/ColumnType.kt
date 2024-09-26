package com.sentralyx.dynamicdb.annotations

import com.sentralyx.dynamicdb.processors.MySQLType

/**
 * Annotation that specifies the SQL type and size constraints for a field in a database entity.
 *
 * This annotation should be applied to fields within a data class that is marked
 * as a database entity. It provides the necessary metadata for generating the
 * corresponding database schema.
 *
 * @property type The SQL type of the field (e.g., STRING, INT) as defined in [MySQLType].
 * @property size The size of the column for string types. Defaults to 1.
 */
@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class ColumnType(
    val type: MySQLType,
    val size: Int = 1 // Default size
)
