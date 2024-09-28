package com.sentralyx.kddm.annotations

/**
 * Annotation that marks a data class as a database entity.
 *
 * This annotation indicates that the annotated class is a representation of a
 * database table. It provides the necessary metadata for generating the
 * corresponding database schema and managing database operations.
 *
 * @property tableName Specifies the name of the corresponding database table.
 *                     If not specified, the class name in lowercase will be used as the table name.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class DatabaseEntity(
    val tableName: String = ""
)
