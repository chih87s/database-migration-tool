package com.db.dbmigrationtool


import com.db.dbmigrationtool.data.Migration
import org.junit.Assert
import org.junit.Test

class MigrationValidationTest {

    @Test
    fun testDuplicateMigrationVersionsThrowsException(){
        val migrations = listOf(
            Migration(1, "CREATE TABLE users"),
            Migration(2, "ALTER TABLE users ADD COLUMN email"),
            Migration(2, "ADD INDEX users_email_index")
        )

        val builder = MigrationToolBuilder().addMultipleMigrations(migrations)

        val sqlException = Assert.assertThrows(IllegalArgumentException::class.java){
            builder.buildSQLMigrateManager()
        }

        val roomException = Assert.assertThrows(IllegalArgumentException::class.java){
            builder.buildRoomMigrateManager()
        }

        Assert.assertEquals(
            "Duplicate migration versions found: [1, 2, 2]",
            sqlException.message
        )

        Assert.assertEquals(
            "Duplicate migration versions found: [1, 2, 2]",
            roomException.message
        )

    }


    @Test
    fun testValidMigrationsNoException(){
        val migrations = listOf(
            Migration(1, "CREATE TABLE users"),
            Migration(2, "ALTER TABLE users ADD COLUMN email"),
            Migration(3, "ADD INDEX users_email_index")
        )
        val builder = MigrationToolBuilder().addMultipleMigrations(migrations)

        try {
            builder.buildSQLMigrateManager()
            builder.buildRoomMigrateManager()
        } catch (e: Exception) {
            Assert.fail("validateMigrations should not throw an exception for valid migrations.")
        }
    }

}