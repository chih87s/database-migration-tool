# DatabaseMigrationTool

> A robust, flexible library for managing complex database schema migrations in Android projects. Supports both native SQLite and the Room library, ensuring data integrity and smooth transitions during schema updates.

[![API](https://img.shields.io/badge/API-29%2B-yellow.svg?style=flat)](https://developer.android.com/about/versions/10)
[![Kotlin Version](https://img.shields.io/badge/Kotlin-2.0.21-blue.svg)](https://kotlinlang.org)
[![LICENSE](https://img.shields.io/badge/License-MIT-blue.svg)](https://github.com/chih87s/database-migration-tool/blob/main/LICENSE)

## Table of Contents
- [Features](#features)
- [Installation](#installation)
- [Getting Started](#getting-started)
- [Usage](#usage)
  - [Room Integration](#room-integration)
  - [SQLite Integration](#sqlite-integration)
- [Advanced Features](#advanced-features)
- [License](#license)
- [Contributing](#contributing)
  
---

## Features
- 🚀 **Supports SQLite and Room**: Seamlessly integrates with Room and native SQLite databases.
- 🔄 **Complex Migrations**: Handle forward and rollback migrations with custom scripts.
- 📊 **Version Management**: Tracks schema versions to ensure consistent updates.
- 🔐 **Transactional Integrity**: Ensures database consistency with transaction-based migrations.
- 📋 **Detailed Logging**: Logs detailed information for debugging and auditing.

## Installation
Add it in build.gradle at the end of repositories:

```Kotlin DSL
dependencies {
  implementation ("com.github.chih87s:database-migration-tool:0.0.1")
}
```
or Maven:
```xml
<dependency>
  <groupId>com.github.chih87s</groupId>
  <artifactId>database-migration-tool</artifactId>
  <version>0.0.1</version>
</dependency>
```

---

## Getting Started
Step 1: Create Migration Scripts
Define your schema changes as Migration objects:
```kotlin
val migration1to2 = Migration(
  version = 2,
  migrationScript = "ALTER TABLE users ADD COLUMN birthdate TEXT;",
  rollbackScript = "ALTER TABLE users DROP COLUMN birthdate;"
)
```

Step 2: Build a Migration Manager
Use MigrationToolBuilder to construct a migration manager

For Room:
```kotlin
val migrationManager = MigrationToolBuilder()
  .addSingleMigration(migration1to2) // add single migration or multiple migrations
  .buildRoomMigrateManager()
```

For SQLite:
```kotlin
val migrationManager = MigrationToolBuilder()
    .addSingleMigration(migration1to2) // add single migration or multiple migrations
    .buildSQLMigrateManager()
```

Step 3: Execute migration by passing the current and target versions:
```kotlin
migrationManager.migrate(dbTool, currentVersion = 1, targetVersion = 3)
```

Step 4: If a migration fails or if you need to revert to a previous version, you can use the rollback functionality:
```kotlin
migrationManager.rollbackToVersion(dbTool, targetVersion = 1)
```
---

## Usage
### Room Integration
Integrate the migration manager with your Room database:
```kotlin
val db = Room.databaseBuilder(context, AppDatabase::class.java, "app-database")
  .addCallback(object : RoomDatabase.Callback() {
    override fun onOpen(db: SupportSQLiteDatabase) {
      super.onOpen(db)
      val dbTool = RoomMigrationTool(db)
      migrationManager.migrate(dbTool, db.version, TARGET_VERSION)
    }
  }
)
.build()
```

### SQLite Integration
Integrate with your SQLiteOpenHelper:
```kotlin
val dbHelper = object : SQLiteOpenHelper(context, "app-database", null, 2) {
    override fun onCreate(db: SQLiteDatabase) {
        // Initial schema setup
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        val dbTool = SQLiteMigrationTool(db)
        migrationManager.migrate(dbTool, oldVersion, newVersion)
    }
}
```
---

### Advanced Features
- Handling Complex Migration
- Rollback Support

## License
`database-migration-tool` is available under the MIT license. See the [LICENSE](https://github.com/chih87s/database-migration-tool/blob/main/LICENSE) file for more information.

## Contributing
Contributions are welcome! 🎉 We appreciate your help to make this tool even better.

### Steps to Contribute:

1. **Fork this repository**:
   - Click the "Fork" button on the top right of the repository page.

2. **Create a new branch**:
   - Name your branch based on the feature or fix you're working on (e.g., `feature/migration-enhancement`).

   ```bash
   git checkout -b feature/migration-enhancement
