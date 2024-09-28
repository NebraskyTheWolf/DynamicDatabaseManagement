package com.sentralyx.kddm.connector

import com.sentralyx.kddm.DatabaseConnectionException
import com.sentralyx.kddm.DatabaseNotConnectedException
import org.apache.commons.dbcp2.BasicDataSource
import java.sql.Connection
import javax.sql.DataSource

object MySQLConnector {

    private var dataSource: DataSource? = null

    /**
     * Establishes a connection to the database using the provided credentials.
     * Automatically selects the correct JDBC driver for MySQL or MariaDB based on the URL.
     *
     * @param url The database URL.
     * @param username The username for the database.
     * @param password The password for the database.
     * @throws DatabaseConnectionException If the connection fails.
     */
    @Throws(DatabaseConnectionException::class)
    fun connect(url: String, username: String, password: String) {
        val driver = when {
            url.startsWith("jdbc:mysql://") -> {
                "com.mysql.cj.jdbc.Driver"
            }
            url.startsWith("jdbc:mariadb://") -> {
                "org.mariadb.jdbc.Driver"
            }
            else -> {
                throw DatabaseConnectionException("Unsupported database URL: $url")
            }
        }


        // Set a JDBC/MySQL connection
        val dataSource = BasicDataSource()

        dataSource.driverClassName = driver
        dataSource.url = url
        dataSource.username = username
        dataSource.password = password
        dataSource.initialSize = 1
        dataSource.maxTotal = 12

        this.dataSource = dataSource
    }

    /**
     * Returns the active database connection.
     *
     * @throws DatabaseNotConnectedException If no active connection is found.
     */
    @Throws(DatabaseNotConnectedException::class)
    fun getConnection(): Connection {
        return dataSource!!.connection ?: throw DatabaseNotConnectedException("No active database connection found.")
    }

    /**
     * Checks if there is an active connection to the database.
     *
     * @return true if connected, false otherwise.
     */
    fun isConnected(): Boolean {
        return !getConnection().isClosed
    }
}
