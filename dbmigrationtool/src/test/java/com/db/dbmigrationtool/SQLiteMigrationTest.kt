package com.db.dbmigrationtool

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import com.db.dbmigrationtool.data.Migration
import com.db.dbmigrationtool.manager.SQLiteMigrationManager
import com.db.dbmigrationtool.tools.SQLiteMigrationTool
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
class SQLiteMigrationTest {


    private lateinit var sqliteMigrationManager: SQLiteMigrationManager
    private lateinit var sqliteDatabase: SQLiteDatabase
    private lateinit var dbFile: File

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        val context = ApplicationProvider.getApplicationContext<Context>()

        dbFile = File(context.filesDir, "test_db.db")

        sqliteDatabase = SQLiteDatabase.openOrCreateDatabase(dbFile, null)

        val migrations = listOf(
            Migration(1, TestMigrationScripts.CREATE_USER_TABLE),
            Migration(2, TestMigrationScripts.ALTER_USER_TABLE_ADD_EMAIL)
        )
        sqliteMigrationManager =
            MigrationToolBuilder().addMultipleMigrations(migrations).buildSQLMigrateManager()
    }

    @After
    fun tearDown() {
        sqliteDatabase.close()

        if (dbFile.exists()) {
            dbFile.delete()
        }
    }

    @Test
    fun test() {

    }

    @Test
    fun sqliteMigrationTest() {
        val currentVersion = 0
        val targetVersion = 2


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


        val cursor = sqliteDatabase.rawQuery(
            "SELECT name FROM sqlite_master WHERE type='table' AND name='users'",
            null
        )

        if (cursor.moveToFirst()) {
            val tableName = cursor.getString(0)
            Assert.assertEquals("users", tableName)
        } else {
            Assert.fail("Direct SQL execution didn't create the table.")
        }

        if (cursor.moveToNext()) {
            val columnName = cursor.getString(cursor.getColumnIndexOrThrow("name"))
            if (columnName == "email") {
                Assert.assertTrue("Email column should have been added", true)
            } else {
                Assert.fail("Cannot find email column")
            }
        }
        sqliteMigrationManager.getCurrentVersion(SQLiteMigrationTool(sqliteDatabase))
        cursor.close()

    }

    //
    @Test
    fun sqliteMigrationRollback() {
        val currentVersion = 2
        val targetVersion = 0
        sqliteMigrationManager.migrate(
            SQLiteMigrationTool(sqliteDatabase),
            currentVersion,
            targetVersion
        )

        val cursor = sqliteDatabase.rawQuery("PRAGMA table_info(users)", null)

        if (cursor.moveToFirst()) {
            val columnName = cursor.getString(cursor.getColumnIndexOrThrow("name"))
            Assert.assertFalse(
                "Email column should have been removed after rollback",
                columnName != "email"
            )
        }

        cursor.close()
    }


    @Test
    fun sqliteInitializeMigration() {

        sqliteMigrationManager.initializeVersionTable(SQLiteMigrationTool(sqliteDatabase))

        val cursor = sqliteDatabase.rawQuery(
            "SELECT name FROM sqlite_master WHERE type='table' AND name='schema_version'", null
        )
        val tableExists = cursor.moveToFirst()
        cursor.close()

        Assert.assertTrue("Version table should have been created", tableExists)

        val versionCursor =
            sqliteDatabase.rawQuery("SELECT version FROM schema_version LIMIT 1", null)
        versionCursor.moveToFirst()
        val initialVersion = versionCursor.getInt(0)
        versionCursor.close()

        Assert.assertEquals("Version should be initialized to 0", 0, initialVersion)

    }
}