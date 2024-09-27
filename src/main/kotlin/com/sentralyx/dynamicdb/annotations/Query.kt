package com.sentralyx.dynamicdb.annotations

/**
 * Annotation for defining custom SQL queries.
 *
 * This annotation can be applied to methods to specify a custom SQL query.
 * The query can include placeholders (e.g., `?`) for parameters, which should
 * be passed as arguments when the method is called.
 *
 * Example usage:
 *
 * ```
 * @Query("SELECT * FROM users WHERE id = ?")
 * fun getUserById(id: Int): User
 * ```
 *
 * @property value The SQL query string to be executed.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Query(val value: String)
