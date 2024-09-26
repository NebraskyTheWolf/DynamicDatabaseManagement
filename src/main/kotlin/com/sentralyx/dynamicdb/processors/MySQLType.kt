package com.sentralyx.dynamicdb.processors

enum class MySQLType(val sqlType: String) {
    TINYINT("TINYINT"),
    SMALLINT("SMALLINT"),
    MEDIUMINT("MEDIUMINT"),
    INT("INT"),
    BIGINT("BIGINT"),
    FLOAT("FLOAT"),
    DOUBLE("DOUBLE"),
    DECIMAL("DECIMAL"),
    BIT("BIT"),
    CHAR("CHAR"),
    VARCHAR("VARCHAR"),
    TEXT("TEXT"),
    MEDIUMTEXT("MEDIUMTEXT"),
    LONGTEXT("LONGTEXT"),
    BINARY("BINARY"),
    VARBINARY("VARBINARY"),
    BLOB("BLOB"),
    MEDIUMBLOB("MEDIUMBLOB"),
    LONGBLOB("LONGBLOB"),
    DATE("DATE"),
    TIME("TIME"),
    DATETIME("DATETIME"),
    TIMESTAMP("TIMESTAMP"),
    YEAR("YEAR"),
    ENUM("ENUM"),
    SET("SET");

    companion object {
        fun isValidType(type: String): Boolean {
            return entries.any { it.sqlType.equals(type, ignoreCase = true) }
        }
    }
}
