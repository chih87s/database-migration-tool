package com.db.dbmigrationtool

import android.content.Context
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.test.core.app.ApplicationProvider
import com.db.dbmigrationtool.data.Migration
import com.db.dbmigrationtool.manager.RoomMigrationManager
import com.db.dbmigrationtool.tools.RoomMigrationTool
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RoomMigrationTest {

    private lateinit var roomMigrationManager: RoomMigrationManager
    private lateinit var roomDatabase: TestDatabase


    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        roomDatabase = Room.inMemoryDatabaseBuilder(
            context, TestDatabase::class.java
        ).build()


        val migrations = listOf(
            Migration(1, TestMigrationScripts.CREATE_USER_TABLE),
            Migration(2, TestMigrationScripts.ALTER_USER_TABLE_ADD_PHONE),
            Migration(3, TestMigrationScripts.ALTER_USER_TABLE_ADD_EMAIL)
        )

        roomMigrationManager =
            MigrationToolBuilder().addMultipleMigrations(migrations).buildRoomMigrateManager()

    }

    @After
    fun tearDown() {
        roomDatabase.close()
    }


    @Test
    fun roomMigrationTest() {
        val currentVersion = 0
        val targetVersion = 3

        roomMigrationManager.migrate(RoomMigrationTool(roomDatabase), currentVersion, targetVersion)

        val cursor = roomDatabase.query("PRAGMA table_info(users)", null)

        val columns = mutableListOf<String>()
        if (cursor.moveToFirst()) {
            do {
                columns.add(cursor.getString(cursor.getColumnIndexOrThrow("name")))
            } while (cursor.moveToNext())
        }
        cursor.close()

        Assert.assertTrue(columns.containsAll(listOf("id", "name", "age", "phone", "email")))

    }

    @Test
    fun roomMigrationRollback() {
        val currentVersion = 3
        val targetVersion = 1

        roomMigrationManager.migrate(RoomMigrationTool(roomDatabase), currentVersion, targetVersion)
        val cursor = roomDatabase.query("PRAGMA table_info(users)", null)

        val columns = mutableListOf<String>()
        if (cursor.moveToFirst()) {
            do {
                columns.add(cursor.getString(cursor.getColumnIndexOrThrow("name")))
            } while (cursor.moveToNext())
        }
        cursor.close()

        Assert.assertTrue(columns.containsAll(listOf("id", "name", "age")))
    }

    @Test
    fun roomInitializeMigration() {
        val freshRoomDatabase = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext<Context>(),
            TestDatabase::class.java
        ).build()

        roomMigrationManager.initializeVersionTable(RoomMigrationTool(freshRoomDatabase))

        val versionTableExists = runCatching {
            val cursor = freshRoomDatabase.query("SELECT * FROM schema_version", null)
            cursor.use {
                it.moveToFirst() && it.getInt(it.getColumnIndexOrThrow("version")) == 0
            }
        }.getOrElse { false }

        Assert.assertTrue("Version table should be initialized with version 0", versionTableExists)

        freshRoomDatabase.close()
    }


}

@Entity(tableName = "users")
data class TestUser(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "age") val age: Int,
)

@Dao
interface UserDao {

}

@Database(entities = [TestUser::class], version = 1, exportSchema = false)
abstract class TestDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
}