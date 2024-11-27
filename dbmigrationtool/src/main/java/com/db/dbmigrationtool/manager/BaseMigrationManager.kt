package com.db.dbmigrationtool.manager

import android.util.Log
import com.db.dbmigrationtool.data.Migration
import com.db.dbmigrationtool.exception.MigrationException
import com.db.dbmigrationtool.interfaces.DatabaseMigration
import com.db.dbmigrationtool.tools.DatabaseMigrationTool


abstract class BaseMigrationManager(
    private val migrations:List<Migration>
): DatabaseMigration {

    abstract fun executeMigrationScript(dbTool: DatabaseMigrationTool, script: String)
    abstract fun getCurrentVersion(dbTool: DatabaseMigrationTool): Int
    abstract fun updateVersion(dbTool: DatabaseMigrationTool, version: Int)
    abstract fun initializeVersionTable(dbTool: DatabaseMigrationTool)

    override fun migrate(dbTool: DatabaseMigrationTool, currentVersion: Int, targetVersion: Int) {

        if (currentVersion < targetVersion) {
            // Forward migration
            migrations.filter { it.version in (currentVersion + 1)..targetVersion }
                .forEach { migration ->
                    executeMigrationScript(dbTool, migration.migrationScript)
                    updateVersion(dbTool, migration.version)
                    Log.i("DatabaseMigration", "Migrated to version ${migration.version}")
                }
        } else if (currentVersion > targetVersion) {
            // Rollback migrations
            migrations.filter { it.version in (targetVersion + 1)..currentVersion }
                .reversed()
                .forEach { migration ->
                    migration.rollbackScript?.let { rollbackScript ->
                        executeMigrationScript(dbTool, rollbackScript)
                        updateVersion(dbTool, migration.version - 1)
                        Log.i("DatabaseMigration", "Rolled back version ${migration.version}")
                    }
                }
        } else {
            Log.i(
                "DatabaseMigration",
                "Database is already at version $currentVersion. No migration needed."
            )
        }
    }



    override fun rollbackToVersion(dbTool: DatabaseMigrationTool, targetVersion: Int) {
        val currentVersion = getCurrentVersion(dbTool)
        if (targetVersion >= currentVersion) {
            throw MigrationException("Target version must be less than the current version.")
        }

        migrations.filter { it.version in (targetVersion + 1)..currentVersion }
            .reversed()
            .forEach { migration ->
                migration.rollbackScript?.let { rollbackScript ->
                    executeMigrationScript(dbTool, rollbackScript)
                }
                updateVersion(dbTool, migration.version - 1)
                Log.i("DatabaseMigration", "Rolled back to version ${migration.version - 1}")
            }
    }
}