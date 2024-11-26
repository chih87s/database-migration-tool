package com.db.dbmigrationtool.manager

import android.content.ContentValues
import android.util.Log
import com.db.dbmigrationtool.data.Migration
import com.db.dbmigrationtool.exception.MigrationException
import com.db.dbmigrationtool.tools.DatabaseMigrationTool
import com.db.dbmigrationtool.tools.SQLiteMigrationTool
import com.db.dbmigrationtool.utils.SQLStatementsUtils

class SQLiteMigrationManager internal constructor(
    migrations: List<Migration>
) : BaseMigrationManager(migrations)
{

    override fun executeMigrationScript(dbTool: DatabaseMigrationTool, script: String) {
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

    override fun updateVersion(dbTool: DatabaseMigrationTool, version: Int) {
        val sqLiteDatabase = (dbTool as SQLiteMigrationTool).database
        initializeVersionTable(dbTool)

        sqLiteDatabase.beginTransaction()
        try {
            val contentValues = ContentValues().apply {
                put("version", version)
            }

            val rowsAffected = sqLiteDatabase.update("schema_version", contentValues, null, null)
            if (rowsAffected == 0) {
                sqLiteDatabase.insert("schema_version", null, contentValues)
            }
            sqLiteDatabase.setTransactionSuccessful()
            Log.d("SQLiteMigration", "Updated schema_version to version $version")
        } catch (e: Exception) {
            Log.e("SQLiteUpdate", "Error updating version to $version", e)
        } finally {
            sqLiteDatabase.endTransaction()
        }
    }

    override fun getCurrentVersion(dbTool: DatabaseMigrationTool): Int {
        val sqLiteDatabase = (dbTool as SQLiteMigrationTool).database
        initializeVersionTable(dbTool)

        val cursor = sqLiteDatabase.rawQuery("SELECT version FROM schema_version LIMIT 1;", null)
        var version = 0

        if (cursor.moveToFirst()) {
            val columnIndex = cursor.getColumnIndex("version")
            if (columnIndex != -1) {
                version = cursor.getInt(columnIndex)
            } else {
                Log.e("SQLiteMigration", "Column 'version' does not exist in the result set.")
            }
        } else {
            Log.e("SQLiteMigration", "No rows found in schema_version table.")
        }

        cursor.close()
        Log.d("SQLiteMigration", "Current version from schema_version: $version")
        return version
    }

    override fun initializeVersionTable(dbTool: DatabaseMigrationTool) {
        val sqLiteDatabase = (dbTool as SQLiteMigrationTool).database
        sqLiteDatabase.execSQL("CREATE TABLE IF NOT EXISTS schema_version (version INTEGER PRIMARY KEY);")

        val cursor = sqLiteDatabase.rawQuery("SELECT COUNT(*) FROM schema_version;", null)
        if (cursor.moveToFirst() && cursor.getInt(0) == 0) {
            sqLiteDatabase.execSQL("INSERT INTO schema_version (version) VALUES (0);")
        }
        cursor.close()

    }
}