package com.db.dbmigrationtool

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import com.db.dbmigrationtool.data.Migration
import com.db.dbmigrationtool.exception.MigrationException
import com.db.dbmigrationtool.manager.SQLiteMigrationManager
import com.db.dbmigrationtool.tools.SQLiteMigrationTool
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockedStatic
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.eq
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
class SQLiteMigrationTest {

    private lateinit var sqliteMigrationManager: SQLiteMigrationManager
    private lateinit var sqliteDatabase: SQLiteDatabase

    @Mock
    private lateinit var logMock: MockedStatic<Log>

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)

        val context: Context = ApplicationProvider.getApplicationContext()
        val dbFile = File(context.getDatabasePath("test.db").absolutePath)
        if (dbFile.exists()) {
            dbFile.delete()
        }
        sqliteDatabase = context.openOrCreateDatabase("test.db", Context.MODE_PRIVATE, null)
    }

    @After
    fun tearDown() {
        sqliteDatabase.close()
        logMock.close()
    }

    private fun queryForTableExists(tableName: String): Boolean {
        val cursor = sqliteDatabase.rawQuery(
            "SELECT name FROM sqlite_master WHERE type='table' AND name=?", arrayOf(tableName)
        )
        cursor.use {
            return it.moveToFirst() && it.getString(0) == tableName
        }
    }

    @Test
    fun sqliteSingleMigrationTest() {
        val migration = Migration(
            version = 1,
            migrationScript = TestMigrationScripts.CREATE_USER_TABLE,
            rollbackScript = TestMigrationScripts.ROLLBACK_CREATE_USER_TABLE

        )
        sqliteMigrationManager =
            MigrationToolBuilder().addSingleMigration(migration).buildSQLMigrateManager()

        try {
            sqliteMigrationManager.migrate(SQLiteMigrationTool(sqliteDatabase), 0, 1)
        } catch (e: Exception) {
            e.printStackTrace()
            Assert.fail("Migration failed: ${e.message}")
        }
        Assert.assertTrue(queryForTableExists("users"))
    }

    @Test
    fun sqliteMultipleMigrationsTest() {
        val currentVersion = 0
        val targetVersion = 2

        val migrations = listOf(
            Migration(1, TestMigrationScripts.CREATE_USER_TABLE),
            Migration(2, TestMigrationScripts.ALTER_USER_TABLE_ADD_EMAIL)
        )
        sqliteMigrationManager =
            MigrationToolBuilder().addMultipleMigrations(migrations).buildSQLMigrateManager()

        try {
            sqliteMigrationManager.migrate(
                SQLiteMigrationTool(sqliteDatabase),
                currentVersion,
                targetVersion
            )
        } catch (e: Exception) {
            e.printStackTrace()
            Assert.fail("Migration failed: ${e.message}")
        }


        sqliteDatabase.rawQuery(
            "SELECT name FROM sqlite_master WHERE type='table' AND name='users'", null
        ).use { cursor ->
            if (cursor.moveToFirst()) {
                val tableName = cursor.getString(0)
                Assert.assertEquals("users", tableName)
            } else {
                Assert.fail("Direct SQL execution didn't create the table.")
            }
        }

        sqliteDatabase.rawQuery(
            "PRAGMA table_info(users)", null
        ).use { cursor ->
            var emailColumnExists = false
            while (cursor.moveToNext()) {
                val columnName = cursor.getString(cursor.getColumnIndex("name"))
                if (columnName == "email") {
                    emailColumnExists = true
                    break
                }
            }
            Assert.assertTrue("Email column should have been added", emailColumnExists)
        }
    }

    @Test
    fun sqliteMigrationRollback() {
        val migrations = listOf(
            Migration(
                1,
                TestMigrationScripts.CREATE_USER_TABLE,
                TestMigrationScripts.ROLLBACK_CREATE_USER_TABLE
            ),
            Migration(
                2,
                TestMigrationScripts.ALTER_USER_TABLE_ADD_EMAIL,
                TestMigrationScripts.ROLLBACK_REMOVE_EMAIL_COLUMN
            )
        )


        sqliteMigrationManager =
            MigrationToolBuilder().addMultipleMigrations(migrations).buildSQLMigrateManager()

        sqliteMigrationManager.migrate(
            SQLiteMigrationTool(sqliteDatabase),
            0,
            2
        )

        val expectedVersion = 1
        sqliteMigrationManager.rollbackToVersion(
            SQLiteMigrationTool(sqliteDatabase),
            expectedVersion
        )

        sqliteDatabase.rawQuery("PRAGMA table_info(users)", null).use { afterCursor ->
            var emailColumnExists = false

            while (afterCursor.moveToNext()) {
                val columnName = afterCursor.getString(afterCursor.getColumnIndex("name"))
                if (columnName == "email") {
                    emailColumnExists = true
                    break
                }
            }

            val currentVersion = sqliteMigrationManager.getCurrentVersion(SQLiteMigrationTool(sqliteDatabase))

            Assert.assertFalse(
                "Email column should have been removed after rollback",
                emailColumnExists
            )
            Assert.assertEquals(
                "Current version should be rolled back to $expectedVersion",
                expectedVersion,
                currentVersion
            )
        }

    }

    @Test(expected = MigrationException::class)
    fun sqliteMigrationRollback_invalid(){
        val migrations = listOf(
            Migration(1, "CREATE TABLE users"),
            Migration(2, "ALTER TABLE users ADD COLUMN email")
        )
        sqliteMigrationManager =
            MigrationToolBuilder().addMultipleMigrations(migrations).buildSQLMigrateManager()
        sqliteMigrationManager.rollbackToVersion(SQLiteMigrationTool(sqliteDatabase),2)
    }

    @Test
    fun sqliteNoMigrationNeeded() {
        val migration = Migration(
            version = 1,
            migrationScript = TestMigrationScripts.CREATE_USER_TABLE,
            rollbackScript = TestMigrationScripts.ROLLBACK_CREATE_USER_TABLE
        )
        sqliteMigrationManager =
            MigrationToolBuilder().addSingleMigration(migration).buildSQLMigrateManager()

        sqliteMigrationManager.migrate(SQLiteMigrationTool(sqliteDatabase), 1, 1)

        logMock.verify {
            Log.i(
                eq("DatabaseMigration"),
                eq("Database is already at version 1. No migration needed.")
            )
        }
    }

    @Test
    fun sqliteInitializeMigration() {
        val migration = Migration(
            version = 1,
            migrationScript = TestMigrationScripts.CREATE_USER_TABLE,
            rollbackScript = TestMigrationScripts.ROLLBACK_CREATE_USER_TABLE
        )

        sqliteMigrationManager =
            MigrationToolBuilder().addSingleMigration(migration).buildSQLMigrateManager()

        sqliteMigrationManager.initializeVersionTable(SQLiteMigrationTool(sqliteDatabase))

        sqliteDatabase.rawQuery(
            "SELECT name FROM sqlite_master WHERE type='table' AND name='schema_version'", null
        ).use { cursor ->
            val tableExists = cursor.moveToFirst()
            Assert.assertTrue("Version table should have been created", tableExists)
        }

        sqliteDatabase.rawQuery("SELECT version FROM schema_version LIMIT 1", null).use { versionCursor ->
            versionCursor.moveToFirst()
            val initialVersion = versionCursor.getInt(0)
            Assert.assertEquals("Version should be initialized to 0", 0, initialVersion)
        }

    }

    @Test
    fun sqliteGetCurrentVersion() {
        val migration = Migration(
            version = 1,
            migrationScript = TestMigrationScripts.CREATE_USER_TABLE,
            rollbackScript = TestMigrationScripts.ROLLBACK_CREATE_USER_TABLE
        )

        val targetVersion = 1

        sqliteMigrationManager =
            MigrationToolBuilder().addSingleMigration(migration).buildSQLMigrateManager()

        sqliteMigrationManager.migrate(
            SQLiteMigrationTool(sqliteDatabase),
            0,
            targetVersion
        )

        val currentVersion =
            sqliteMigrationManager.getCurrentVersion(SQLiteMigrationTool(sqliteDatabase))

        Assert.assertEquals(currentVersion, targetVersion)

    }

    @Test
    fun sqliteUpdateVersion() {
        val migration = Migration(
            version = 1,
            migrationScript = TestMigrationScripts.CREATE_USER_TABLE,
            rollbackScript = TestMigrationScripts.ROLLBACK_CREATE_USER_TABLE
        )

        val targetVersion = 1

        sqliteMigrationManager =
            MigrationToolBuilder().addSingleMigration(migration).buildSQLMigrateManager()

        sqliteMigrationManager.updateVersion(SQLiteMigrationTool(sqliteDatabase), targetVersion)

        val currentVersion =
            sqliteMigrationManager.getCurrentVersion(SQLiteMigrationTool(sqliteDatabase))

        Assert.assertEquals(currentVersion, targetVersion)
    }


    @Test
    fun testMigrationExceptionThrown(){
        val migrations = listOf(
            Migration(1, "CREATE TABLE users"),
            Migration(2, "INVALID MIGRATION SCRIPT")
        )
        sqliteMigrationManager =
            MigrationToolBuilder().addMultipleMigrations(migrations).buildSQLMigrateManager()


        val exception = Assert.assertThrows(MigrationException::class.java) {
            sqliteMigrationManager.migrate(SQLiteMigrationTool(sqliteDatabase),0,2)
        }
        Assert.assertEquals("Migration failed: incomplete input (code 1 SQLITE_ERROR): , while compiling: CREATE TABLE users", exception.message)
    }


}