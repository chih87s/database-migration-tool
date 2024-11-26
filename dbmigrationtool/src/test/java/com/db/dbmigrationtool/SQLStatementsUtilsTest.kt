package com.db.dbmigrationtool

import com.db.dbmigrationtool.utils.SQLStatementsUtils
import org.junit.Assert
import org.junit.Test


class SQLStatementsUtilsTest {

    @Test
    fun testSplitSQLStatements_multipleStatements() {
        val input = """
            CREATE TABLE users (id INTEGER PRIMARY KEY);
            INSERT INTO users (name, age) VALUES ('Alice', 30);
            INSERT INTO users (name, age) VALUES ('Bob', 25);
        """.trimIndent()

        val result = SQLStatementsUtils.splitSQLStatements(input)

        Assert.assertEquals(3, result.size)
        Assert.assertEquals("CREATE TABLE users (id INTEGER PRIMARY KEY)", result[0])
        Assert.assertEquals("INSERT INTO users (name, age) VALUES ('Alice', 30)", result[1])
        Assert.assertEquals("INSERT INTO users (name, age) VALUES ('Bob', 25)", result[2])
    }

    @Test
    fun testSplitSQLStatements_singleStatement() {
        val input = "CREATE TABLE users (id INTEGER PRIMARY KEY);"

        val result = SQLStatementsUtils.splitSQLStatements(input)

        Assert.assertEquals(1, result.size)
        Assert.assertEquals("CREATE TABLE users (id INTEGER PRIMARY KEY)", result[0])
    }

    @Test
    fun testSplitSQLStatements_emptyInput() {
        val input = ""

        val result = SQLStatementsUtils.splitSQLStatements(input)

        Assert.assertTrue(result.isEmpty())
    }

    @Test
    fun testSplitSQLStatements_withExtraSpaces() {
        val input = """
            CREATE TABLE users (id INTEGER PRIMARY KEY);
            INSERT INTO users (name, age) VALUES ('Alice', 30);
        """.trimIndent()

        val result = SQLStatementsUtils.splitSQLStatements(input)

        Assert.assertEquals(2, result.size)
        Assert.assertEquals("CREATE TABLE users (id INTEGER PRIMARY KEY)", result[0])
        Assert.assertEquals("INSERT INTO users (name, age) VALUES ('Alice', 30)", result[1])
    }

    @Test
    fun testSplitSQLStatements_noSemicolon() {
        val input = "CREATE TABLE users (id INTEGER PRIMARY KEY)"

        val result = SQLStatementsUtils.splitSQLStatements(input)

        Assert.assertEquals(1, result.size)
        Assert.assertEquals("CREATE TABLE users (id INTEGER PRIMARY KEY)", result[0])
    }

    @Test
    fun testSplitSQLStatements_withSpacesAroundSemicolons() {
        val input = """
            CREATE TABLE users (id INTEGER PRIMARY KEY) ;
            INSERT INTO users (name, age) VALUES ('Alice', 30) ;
        """.trimIndent()

        val result = SQLStatementsUtils.splitSQLStatements(input)

        Assert.assertEquals(2, result.size)
        Assert.assertEquals("CREATE TABLE users (id INTEGER PRIMARY KEY)", result[0])
        Assert.assertEquals("INSERT INTO users (name, age) VALUES ('Alice', 30)", result[1])
    }

}