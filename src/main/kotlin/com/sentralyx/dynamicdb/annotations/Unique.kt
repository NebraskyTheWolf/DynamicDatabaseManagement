package com.sentralyx.dynamicdb.annotations

/**
 * Annotation that indicates a field must contain unique values within a database table.
 *
 * This annotation should be applied to a field within a data class that represents
 * a value that should be unique across all records in the corresponding database table.
 * When a field is annotated with @Unique, the database enforces this constraint,
 * preventing the insertion of duplicate values in that field.
 *
 * Note: This annotation can be used in conjunction with the @PrimaryKey annotation,
 * but multiple fields in the same database table cannot have the @Unique constraint.
 */
@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class Unique
