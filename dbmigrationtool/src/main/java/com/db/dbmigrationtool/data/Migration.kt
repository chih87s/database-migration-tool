package com.db.dbmigrationtool.data

data class Migration(
    val version:Int,
    val migrationScript:String,
    val rollbackScript:String? = null
)