package com.sentralyx.dynamicdb.connector

import com.sentralyx.dynamicdb.DatabaseConnectionException
import com.sentralyx.dynamicdb.DatabaseNotConnectedException
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException


object MySQLConnector {
    private var connection: Connection? = null

    /**
     * Establishes a connection to the database using the provided credentials.
     *
     * @param url The database URL.
     * @param username The username for the database.
     * @param password The password for the database.
     * @throws DatabaseConnectionException If the connection fails.
     */
    @Throws(DatabaseConnectionException::class)
    fun connect(url: String, username: String, password: String) {
        try {
            if (connection == null || connection!!.isClosed) {
                connection = DriverManager.getConnection(url, username, password)
            }
        } catch (e: SQLException) {
            throw DatabaseConnectionException("Failed to connect to the database: ${e.message}", e)
        }
    }

    /**
     * Returns the active database connection.
     *
     * @throws DatabaseNotConnectedException If no active connection is found.
     */
    @Throws(DatabaseNotConnectedException::class)
    fun getConnection(): Connection {
        return connection ?: throw DatabaseNotConnectedException("No active database connection found.")
    }

    /**
     * Closes the database connection.
     *
     * @throws DatabaseConnectionException If disconnection fails.
     */
    @Throws(DatabaseConnectionException::class)
    fun disconnect() {
        try {
            connection?.close()
            connection = null // Clear the connection after closing
        } catch (e: SQLException) {
            throw DatabaseConnectionException("Failed to disconnect from the database: ${e.message}", e)
        }
    }

    /**
     * Checks if there is an active connection to the database.
     *
     * @return true if connected, false otherwise.
     */
    fun isConnected(): Boolean {
        return connection != null && !connection!!.isClosed
    }

    /**
     * Resets the connection to null, allowing for a fresh connection attempt.
     */
    fun resetConnection() {
        connection = null
    }
}
