package com.db.dbmigrationtool.manager

import android.content.ContentValues
import android.util.Log
import com.db.dbmigrationtool.data.Migration
import com.db.dbmigrationtool.exception.MigrationException
import com.db.dbmigrationtool.tools.DatabaseMigrationTool
import com.db.dbmigrationtool.tools.SQLiteMigrationTool
import com.db.dbmigrationtool.utils.SQLStatementsUtils

class SQLiteMigrationManager internal constructor(
    migrations: List<Migration>,
) : BaseMigrationManager(migrations) {
    override fun executeMigrationScript(
        dbTool: DatabaseMigrationTool,
        script: String,
    ) {
        val sqLiteDatabase = (dbTool as SQLiteMigrationTool).database
        val statements = SQLStatementsUtils.splitSQLStatements(script)

        sqLiteDatabase.beginTransaction()
        try {
            statements.forEach { statement ->
                Log.d("SQLiteMigration", "Executing script: $statement")
                sqLiteDatabase.execSQL(statement)
            }
            sqLiteDatabase.setTransactionSuccessful()
        } catch (e: Exception) {
            Log.e("SQLiteMigration", "Migration failed with script: $script", e)
            throw MigrationException("Migration failed: ${e.message}", e)
        } finally {
            sqLiteDatabase.endTransaction()
        }
    }

    override fun updateVersion(
        dbTool: DatabaseMigrationTool,
        version: Int,
    ) {
        val sqLiteDatabase = (dbTool as SQLiteMigrationTool).database
        initializeVersionTable(dbTool)

        sqLiteDatabase.beginTransaction()
        try {
            val contentValues =
                ContentValues().apply {
                    put("version", version)
                }
            sqLiteDatabase.update("schema_version", contentValues, null, null)
            sqLiteDatabase.setTransactionSuccessful()
            Log.d("SQLiteMigration", "Updated schema_version to version $version")
        } catch (e: Exception) {
            Log.e("SQLiteUpdate", "Error updating version to $version")
        } finally {
            sqLiteDatabase.endTransaction()
        }
    }

    override fun getCurrentVersion(dbTool: DatabaseMigrationTool): Int {
        val sqLiteDatabase = (dbTool as SQLiteMigrationTool).database
        initializeVersionTable(dbTool)

        var version = 0
        sqLiteDatabase.rawQuery("SELECT version FROM schema_version LIMIT 1;", null).use { cursor ->
            if (cursor.moveToFirst()) {
                version = cursor.getInt(0)
            } else {
                Log.e("SQLiteMigration", "No rows found in schema_version table.")
            }
        }
        Log.d("SQLiteMigration", "Current version from schema_version: $version")

        return version
    }

    override fun initializeVersionTable(dbTool: DatabaseMigrationTool) {
        val sqLiteDatabase = (dbTool as SQLiteMigrationTool).database
        sqLiteDatabase.execSQL("CREATE TABLE IF NOT EXISTS schema_version (version INTEGER PRIMARY KEY);")

        sqLiteDatabase.rawQuery("SELECT COUNT(*) FROM schema_version;", null).use { cursor ->
            if (cursor.moveToFirst() && cursor.getInt(0) == 0) {
                sqLiteDatabase.execSQL("INSERT INTO schema_version (version) VALUES (0);")
            }
        }
    }
}
