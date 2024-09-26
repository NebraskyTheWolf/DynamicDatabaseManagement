package com.sentralyx.dynamicdb.annotations

/**
 * Annotation that identifies a field as the primary key of a database table.
 *
 * This annotation should be applied to a field within a data class that
 * represents a unique identifier for records in the corresponding database table.
 * A primary key ensures that each record can be uniquely identified.
 *
 * Note: Only one field within a database entity can be annotated with this
 * annotation to maintain the integrity of the primary key constraint.
 */
@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class PrimaryKey
