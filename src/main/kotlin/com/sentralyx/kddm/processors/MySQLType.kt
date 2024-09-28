package com.sentralyx.kddm.processors

enum class MySQLType(val sqlType: String, val isSizeable: Boolean = false, val maxSize: Int = 0) {
    JSON("JSON"),
    BOOL("BOOL"),
    TINYINT("TINYINT", true, 255),
    SMALLINT("SMALLINT", true, Short.MAX_VALUE.toInt()),
    MEDIUMINT("MEDIUMINT", true, 16777215),
    INT("INT", true, Int.MAX_VALUE),
    BIGINT("BIGINT", true, Long.MAX_VALUE.toInt()),
    FLOAT("FLOAT", true, Float.MAX_VALUE.toInt()),
    DOUBLE("DOUBLE", true, Double.MAX_VALUE.toInt()),
    DECIMAL("DECIMAL", true, 65),
    BIT("BIT", true, 64),
    CHAR("CHAR", true, 255),
    VARCHAR("VARCHAR", true, 65535),
    TEXT("TEXT", false),
    MEDIUMTEXT("MEDIUMTEXT"),
    LONGTEXT("LONGTEXT"),
    BINARY("BINARY", true, 255),
    VARBINARY("VARBINARY", true, 65535),
    BLOB("BLOB"),
    MEDIUMBLOB("MEDIUMBLOB"),
    LONGBLOB("LONGBLOB"),
    DATE("DATE"),
    TIME("TIME"),
    DATETIME("DATETIME"),
    TIMESTAMP("TIMESTAMP"),
    YEAR("YEAR", true, 4),
    ENUM("ENUM", true, 65535),
    SET("SET", true, 64);

    companion object {
        fun isValidType(type: String): Boolean {
            return entries.any { it.sqlType.equals(type, ignoreCase = true) }
        }
    }
}
