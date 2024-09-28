package com.sentralyx.dynamicdb.processors

enum class ForeignKeyType {
    SET_NULL,
    SET_DEFAULT,
    CASCADE,
    NO_ACTION
}