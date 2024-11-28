package com.db.dbmigrationtool

import com.db.dbmigrationtool.data.Migration
import com.db.dbmigrationtool.manager.RoomMigrationManager
import com.db.dbmigrationtool.manager.SQLiteMigrationManager

class MigrationToolBuilder(
    migrations: List<Migration> = listOf(),
) {
    private val migrations = migrations.toMutableList()

    fun addSingleMigration(migration: Migration): MigrationToolBuilder {
        migrations.add(migration)
        return this
    }

    fun addMultipleMigrations(migrationList: List<Migration>): MigrationToolBuilder {
        migrations.addAll(migrationList)
        return this
    }

    fun buildSQLMigrateManager(): SQLiteMigrationManager {
        validateMigrations()
        return SQLiteMigrationManager(migrations)
    }

    fun buildRoomMigrateManager(): RoomMigrationManager {
        validateMigrations()
        return RoomMigrationManager(migrations)
    }

    private fun validateMigrations() {
        migrations.sortBy { it.version }
        val versions = migrations.map { it.version }
        if (versions.size != versions.toSet().size) {
            throw IllegalArgumentException("Duplicate migration versions found: $versions")
        }
    }
}
