package com.db.dbmigrationtool.manager

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import com.db.dbmigrationtool.data.Migration
import com.db.dbmigrationtool.exception.MigrationException
import com.db.dbmigrationtool.tools.DatabaseMigrationTool
import com.db.dbmigrationtool.tools.RoomMigrationTool
import com.db.dbmigrationtool.utils.SQLStatementsUtils

class RoomMigrationManager internal constructor(
    migrations: List<Migration>
) : BaseMigrationManager(migrations) {

    override fun executeMigrationScript(dbTool: DatabaseMigrationTool, script: String) {
        val roomDatabase = (dbTool as RoomMigrationTool).database
        val supportSQLiteDatabase = roomDatabase.openHelper.writableDatabase
        val statements = SQLStatementsUtils.splitSQLStatements(script)

        supportSQLiteDatabase.beginTransaction()
        try {
            statements.forEach { statement ->
                supportSQLiteDatabase.execSQL(statement)
                Log.d("RoomMigration", "Executed: $statement")
            }
            supportSQLiteDatabase.setTransactionSuccessful()
        } catch (e: Exception) {
            Log.e("RoomMigration", "Migration failed with script: $script", e)
            throw MigrationException("Migration failed: ${e.message}", e)
        } finally {
            supportSQLiteDatabase.endTransaction()
        }
    }

    override fun getCurrentVersion(dbTool: DatabaseMigrationTool): Int {
        val roomDatabase = (dbTool as RoomMigrationTool).database
        val supportSQLiteDatabase = roomDatabase.openHelper.writableDatabase
        initializeVersionTable(dbTool)

        var version = 0

        supportSQLiteDatabase.query("SELECT version FROM schema_version LIMIT 1;").use { cursor ->
            if (cursor.moveToFirst()) {
                version = cursor.getInt(0)
            } else {
                Log.e("RoomMigration", "No rows found in schema_version table.")
            }
        }

        Log.d("RoomMigration", "Current version from schema_version: $version")
        return version
    }

    override fun updateVersion(dbTool: DatabaseMigrationTool, version: Int) {
        val roomDatabase = (dbTool as RoomMigrationTool).database
        val supportSQLiteDatabase = roomDatabase.openHelper.writableDatabase
        initializeVersionTable(dbTool)

        supportSQLiteDatabase.beginTransaction()
        try {

            val contentValues = ContentValues().apply {
                put("version", version)
            }

            supportSQLiteDatabase.update(
                "schema_version",
                SQLiteDatabase.CONFLICT_NONE,
                contentValues,
                null,
                null
            )

            supportSQLiteDatabase.setTransactionSuccessful()
        } catch (e: Exception) {
            Log.e("SQLiteUpdate", "Error updating version to $version", e)
        } finally {
            supportSQLiteDatabase.endTransaction()
        }

    }

    override fun initializeVersionTable(dbTool: DatabaseMigrationTool) {
        val roomDatabase = (dbTool as RoomMigrationTool).database
        val supportSQLiteDatabase = roomDatabase.openHelper.writableDatabase
        supportSQLiteDatabase.execSQL("CREATE TABLE IF NOT EXISTS schema_version (version INTEGER PRIMARY KEY);")
        supportSQLiteDatabase.query("SELECT COUNT(*) FROM schema_version;").use { cursor ->
            if (cursor.moveToFirst() && cursor.getInt(0) == 0) {
                supportSQLiteDatabase.execSQL("INSERT INTO schema_version (version) VALUES (0);")
            }
        }
    }
}