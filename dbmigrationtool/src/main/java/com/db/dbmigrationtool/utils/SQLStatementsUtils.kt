package com.db.dbmigrationtool.utils

object SQLStatementsUtils {
    fun splitSQLStatements(script: String): List<String> =
        script
            .trim()
            .split(";")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
}
