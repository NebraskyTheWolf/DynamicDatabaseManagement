package com.sentralyx.kddm

class DatabaseConnectionException(message: String, cause: Throwable? = null) : Exception(message, cause)
class DatabaseNotConnectedException(message: String) : Exception(message)
