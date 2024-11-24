package com.db.dbmigrationtool.manager

import android.util.Log
import com.db.dbmigrationtool.data.Migration
import com.db.dbmigrationtool.exception.MigrationException
import com.db.dbmigrationtool.tools.DatabaseMigrationTool
import com.db.dbmigrationtool.tools.SQLiteMigrationTool

class SQLiteMigrationManager internal constructor(
    migrations: List<Migration>
) : BaseMigrationManager(migrations)
{

    override fun executeMigrationScript(dbTool: DatabaseMigrationTool, script: String) {
        val sqLiteDatabase = (dbTool as SQLiteMigrationTool).database
        sqLiteDatabase.beginTransaction()
        try {
            Log.d("MigrationScript", "Executing script: $script")
            sqLiteDatabase.execSQL(script)
            sqLiteDatabase.setTransactionSuccessful()
            println("executeMigrationScript：Executing script:")
        } catch (e: Exception) {
            Log.e("SQLiteMigration", "Migration failed with script: $script", e)
            println("executeMigrationScript：Migration failed with script:$script")

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
            sqLiteDatabase.execSQL("INSERT OR REPLACE INTO schema_version (version) VALUES ($version);")
            sqLiteDatabase.setTransactionSuccessful()
            Log.d("SQLiteMigration", "Updated schema_version to version $version")
            println("updateVersion： Updated schema_version to version $version\"")

        } catch (e: Exception) {
            Log.e("SQLiteUpdate", "Error updating version to $version", e)
            println("updateVersion ：Error updating schema_version to version $version\" $e")

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
            version = cursor.getInt(0)
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
            Log.d("SQLiteInit", "Table initialized with version 0")
            println("initializeVersionTable： Table initialized with version 0")
        }
        cursor.close()

    }
}