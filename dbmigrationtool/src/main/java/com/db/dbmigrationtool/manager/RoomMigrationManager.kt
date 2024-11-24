package com.db.dbmigrationtool.manager

import android.util.Log
import com.db.dbmigrationtool.data.Migration
import com.db.dbmigrationtool.exception.MigrationException
import com.db.dbmigrationtool.tools.DatabaseMigrationTool
import com.db.dbmigrationtool.tools.RoomMigrationTool

class RoomMigrationManager internal constructor(
    migrations: List<Migration>
): BaseMigrationManager(migrations)
{

    override fun executeMigrationScript(dbTool: DatabaseMigrationTool, script: String) {
        val roomDatabase = (dbTool as RoomMigrationTool).database
        val supportSQLiteDatabase = roomDatabase.openHelper.writableDatabase
        supportSQLiteDatabase.beginTransaction()
        try {
            supportSQLiteDatabase.execSQL(script)
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
        val cursor = supportSQLiteDatabase.query("SELECT version FROM schema_version LIMIT 1;")
        val version = if (cursor.moveToFirst()) cursor.getInt(0) else 0
        cursor.close()
        return version
    }

    override fun updateVersion(dbTool: DatabaseMigrationTool, version: Int) {
        val roomDatabase = (dbTool as RoomMigrationTool).database
        val supportSQLiteDatabase = roomDatabase.openHelper.writableDatabase
        initializeVersionTable(dbTool)
        supportSQLiteDatabase.execSQL("INSERT OR REPLACE INTO schema_version (version) VALUES ($version);")
    }

    override fun initializeVersionTable(dbTool: DatabaseMigrationTool) {
        val roomDatabase = (dbTool as RoomMigrationTool).database
        val supportSQLiteDatabase = roomDatabase.openHelper.writableDatabase
        supportSQLiteDatabase.execSQL("CREATE TABLE IF NOT EXISTS schema_version (version INTEGER PRIMARY KEY);")
        val cursor = supportSQLiteDatabase.query("SELECT COUNT(*) FROM schema_version;")
        if (cursor.moveToFirst() && cursor.getInt(0) == 0) {
            supportSQLiteDatabase.execSQL("INSERT INTO schema_version (version) VALUES (0);")
        }
        cursor.close()
    }
}