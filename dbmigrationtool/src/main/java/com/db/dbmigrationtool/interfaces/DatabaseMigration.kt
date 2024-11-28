package com.db.dbmigrationtool.interfaces

import com.db.dbmigrationtool.tools.DatabaseMigrationTool

interface DatabaseMigration {
    fun migrate(
        dbTool: DatabaseMigrationTool,
        currentVersion: Int,
        targetVersion: Int,
    )

    fun rollbackToVersion(
        dbTool: DatabaseMigrationTool,
        targetVersion: Int,
    )
}
