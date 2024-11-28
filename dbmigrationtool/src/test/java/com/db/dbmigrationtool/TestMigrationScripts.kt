package com.db.dbmigrationtool

object TestMigrationScripts {
    // Version 1: Initial table creation (Rollback: Drop table)
    const val CREATE_USER_TABLE = """
        CREATE TABLE IF NOT EXISTS users (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            name TEXT NOT NULL,
            age INTEGER NOT NULL
        );
    """

    // Rollback migration script for version 1: Drop users table
    const val ROLLBACK_CREATE_USER_TABLE = """
        DROP TABLE IF EXISTS users;
    """

    // Forward migration script for version 2: Add email column to users table
    const val ALTER_USER_TABLE_ADD_EMAIL = """
        ALTER TABLE users ADD COLUMN email TEXT;
    """

    // Rollback migration script for version 2
    // Remove email column from users table
    const val ROLLBACK_REMOVE_EMAIL_COLUMN = """
        CREATE TABLE IF NOT EXISTS users_without_email AS SELECT id, name, age FROM users;
        DROP TABLE users;
        ALTER TABLE users_without_email RENAME TO users;
    """

    // Forward migration script for version 3: Add phone column to users table
    const val ALTER_USER_TABLE_ADD_PHONE = """
        ALTER TABLE users ADD COLUMN phone TEXT;
    """

    // Rollback migration script for version 3
    // Remove phone column from users table
    const val ROLLBACK_REMOVE_PHONE_COLUMN = """
        CREATE TABLE users_without_phone AS SELECT id, name, age, email FROM users;   
        DROP TABLE users;
        ALTER TABLE users_without_phone RENAME TO users;
    """
}
