package com.db.dbmigrationtool

import android.content.Context
import android.util.Log
import androidx.room.ColumnInfo
import androidx.room.Database
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.test.core.app.ApplicationProvider
import com.db.dbmigrationtool.data.Migration
import com.db.dbmigrationtool.exception.MigrationException
import com.db.dbmigrationtool.manager.RoomMigrationManager
import com.db.dbmigrationtool.tools.RoomMigrationTool
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockedStatic
import org.mockito.Mockito.mockStatic
import org.mockito.kotlin.eq
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RoomMigrationTest {
    private lateinit var roomMigrationManager: RoomMigrationManager
    private lateinit var roomDatabase: TestDatabase

    @Mock
    private lateinit var logMock: MockedStatic<Log>

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        roomDatabase =
            Room
                .inMemoryDatabaseBuilder(
                    context,
                    TestDatabase::class.java,
                ).build()

        logMock = mockStatic(Log::class.java)
    }

    @After
    fun tearDown() {
        roomDatabase.close()
        logMock.close()
    }

    private fun queryForTableExists(tableName: String): Boolean {
        val cursor =
            roomDatabase.query(
                "SELECT name FROM sqlite_master WHERE type='table' AND name=?",
                arrayOf(tableName),
            )
        cursor.use {
            return it.moveToFirst() && it.getString(0) == tableName
        }
    }

    @Test
    fun roomSingleMigrationTest() {
        val migration =
            Migration(
                version = 1,
                migrationScript = TestMigrationScripts.CREATE_USER_TABLE,
                rollbackScript = TestMigrationScripts.ROLLBACK_CREATE_USER_TABLE,
            )

        roomMigrationManager =
            MigrationToolBuilder().addSingleMigration(migration).buildRoomMigrateManager()

        try {
            roomMigrationManager.migrate(RoomMigrationTool(roomDatabase), 0, 1)
        } catch (e: Exception) {
            e.printStackTrace()
            Assert.fail("Migration failed: ${e.message}")
        }

        Assert.assertTrue(queryForTableExists("users"))
    }

    @Test
    fun roomMultipleMigrationsTest() {
        val currentVersion = 0
        val targetVersion = 3
        val migrations =
            listOf(
                Migration(1, TestMigrationScripts.CREATE_USER_TABLE),
                Migration(2, TestMigrationScripts.ALTER_USER_TABLE_ADD_PHONE),
                Migration(3, TestMigrationScripts.ALTER_USER_TABLE_ADD_EMAIL),
            )

        roomMigrationManager =
            MigrationToolBuilder().addMultipleMigrations(migrations).buildRoomMigrateManager()

        roomMigrationManager.migrate(RoomMigrationTool(roomDatabase), currentVersion, targetVersion)

        val cursor = roomDatabase.query("PRAGMA table_info(users)", null)

        val columns = mutableListOf<String>()
        cursor.use {
            if (it.moveToFirst()) {
                do {
                    columns.add(it.getString(it.getColumnIndexOrThrow("name")))
                } while (it.moveToNext())
            }
        }
        Assert.assertTrue(columns.containsAll(listOf("id", "name", "age", "phone", "email")))
    }

    @Test
    fun roomMigrationRollback() {
        val migrations =
            listOf(
                Migration(
                    1,
                    TestMigrationScripts.CREATE_USER_TABLE,
                    TestMigrationScripts.ROLLBACK_CREATE_USER_TABLE,
                ),
                Migration(
                    2,
                    TestMigrationScripts.ALTER_USER_TABLE_ADD_EMAIL,
                    TestMigrationScripts.ROLLBACK_REMOVE_EMAIL_COLUMN,
                ),
                Migration(
                    3,
                    TestMigrationScripts.ALTER_USER_TABLE_ADD_PHONE,
                    TestMigrationScripts.ROLLBACK_REMOVE_PHONE_COLUMN,
                ),
            )

        roomMigrationManager =
            MigrationToolBuilder().addMultipleMigrations(migrations).buildRoomMigrateManager()

        roomMigrationManager.migrate(
            RoomMigrationTool(roomDatabase),
            0,
            3,
        )

        roomMigrationManager.rollbackToVersion(RoomMigrationTool(roomDatabase), 1)

        val columns = mutableListOf<String>()
        val afterCursor = roomDatabase.query("PRAGMA table_info(users)", null)
        afterCursor.use {
            while (it.moveToNext()) {
                columns.add(it.getString(it.getColumnIndex("name")))
            }
        }

        Assert.assertFalse("Phone column should have been removed", columns.contains("phone"))
        Assert.assertFalse("Email column should have been removed", columns.contains("email"))
    }

    @Test(expected = MigrationException::class)
    fun roomMigrationRollback_invalid() {
        val migrations =
            listOf(
                Migration(1, "CREATE TABLE users"),
                Migration(2, "ALTER TABLE users ADD COLUMN email"),
            )
        roomMigrationManager =
            MigrationToolBuilder().addMultipleMigrations(migrations).buildRoomMigrateManager()
        roomMigrationManager.rollbackToVersion(RoomMigrationTool(roomDatabase), 2)
    }

    @Test
    fun sqlite_migration_target_less_than_current() {
        val migrations =
            listOf(
                Migration(
                    1,
                    TestMigrationScripts.CREATE_USER_TABLE,
                    TestMigrationScripts.ROLLBACK_CREATE_USER_TABLE,
                ),
                Migration(
                    2,
                    TestMigrationScripts.ALTER_USER_TABLE_ADD_EMAIL,
                    TestMigrationScripts.ROLLBACK_REMOVE_EMAIL_COLUMN,
                ),
            )
        roomMigrationManager =
            MigrationToolBuilder().addMultipleMigrations(migrations).buildRoomMigrateManager()

        roomMigrationManager.migrate(RoomMigrationTool(roomDatabase), 0, 2)

        roomMigrationManager.migrate(RoomMigrationTool(roomDatabase), 2, 1)

        roomDatabase.query("PRAGMA table_info(users)", null).use { cursor ->
            var emailColumnExists = false
            while (cursor.moveToNext()) {
                val columnName = cursor.getString(cursor.getColumnIndex("name"))
                if (columnName == "email") {
                    emailColumnExists = true
                    break
                }
            }
            Assert.assertFalse(
                "Email column should have been removed after rollback",
                emailColumnExists,
            )
        }
    }

    @Test
    fun roomInitializeMigration() {
        val freshRoomDatabase =
            Room
                .inMemoryDatabaseBuilder(
                    ApplicationProvider.getApplicationContext(),
                    TestDatabase::class.java,
                ).build()

        val migration =
            Migration(
                version = 1,
                migrationScript = TestMigrationScripts.CREATE_USER_TABLE,
                rollbackScript = TestMigrationScripts.ROLLBACK_CREATE_USER_TABLE,
            )

        roomMigrationManager =
            MigrationToolBuilder().addSingleMigration(migration).buildRoomMigrateManager()

        roomMigrationManager.initializeVersionTable(RoomMigrationTool(freshRoomDatabase))

        val versionTableExists =
            runCatching {
                val cursor = freshRoomDatabase.query("SELECT * FROM schema_version", null)
                cursor.use {
                    it.moveToFirst() && it.getInt(it.getColumnIndexOrThrow("version")) == 0
                }
            }.getOrElse { false }

        Assert.assertTrue("Version table should be initialized with version 0", versionTableExists)
        freshRoomDatabase.close()
    }

    @Test
    fun roomGetCurrentVersion() {
        val migration =
            Migration(
                version = 1,
                migrationScript = TestMigrationScripts.CREATE_USER_TABLE,
                rollbackScript = TestMigrationScripts.ROLLBACK_CREATE_USER_TABLE,
            )

        val targetVersion = 1

        roomMigrationManager =
            MigrationToolBuilder().addSingleMigration(migration).buildRoomMigrateManager()

        roomMigrationManager.migrate(RoomMigrationTool(roomDatabase), 0, targetVersion)

        val currentVersion = roomMigrationManager.getCurrentVersion(RoomMigrationTool(roomDatabase))

        Assert.assertEquals(currentVersion, targetVersion)
    }

    @Test
    fun roomUpdateVersion() {
        val migration =
            Migration(
                version = 1,
                migrationScript = TestMigrationScripts.CREATE_USER_TABLE,
                rollbackScript = TestMigrationScripts.ROLLBACK_CREATE_USER_TABLE,
            )
        val targetVersion = 1
        roomMigrationManager =
            MigrationToolBuilder().addSingleMigration(migration).buildRoomMigrateManager()
        roomMigrationManager.updateVersion(RoomMigrationTool(roomDatabase), targetVersion)
        val currentVersion = roomMigrationManager.getCurrentVersion(RoomMigrationTool(roomDatabase))
        Assert.assertEquals(currentVersion, targetVersion)
    }

    @Test
    fun roomNoMigrationNeeded() {
        val migration =
            Migration(
                version = 1,
                migrationScript = TestMigrationScripts.CREATE_USER_TABLE,
                rollbackScript = TestMigrationScripts.ROLLBACK_CREATE_USER_TABLE,
            )
        roomMigrationManager =
            MigrationToolBuilder().addSingleMigration(migration).buildRoomMigrateManager()

        roomMigrationManager.migrate(RoomMigrationTool(roomDatabase), 1, 1)

        logMock.verify {
            Log.i(
                eq("DatabaseMigration"),
                eq("Database is already at version 1. No migration needed."),
            )
        }
    }

    @Test
    fun testMigrationExceptionThrown() {
        val migrations =
            listOf(
                Migration(1, "CREATE TABLE users"),
                Migration(2, "INVALID MIGRATION SCRIPT"),
            )
        roomMigrationManager =
            MigrationToolBuilder().addMultipleMigrations(migrations).buildRoomMigrateManager()

        val exception =
            Assert.assertThrows(MigrationException::class.java) {
                roomMigrationManager.migrate(RoomMigrationTool(roomDatabase), 0, 2)
            }
        Assert.assertEquals(
            "Migration failed: incomplete input (code 1 SQLITE_ERROR): , while compiling: CREATE TABLE users",
            exception.message,
        )
    }
}

@Entity(tableName = "users")
data class TestUser(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "age") val age: Int,
)

@Database(entities = [TestUser::class], version = 1, exportSchema = false)
abstract class TestDatabase : RoomDatabase()
