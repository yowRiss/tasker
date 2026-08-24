package com.tasker.android.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE notes ADD COLUMN reminder_at TEXT DEFAULT NULL")
        database.execSQL("ALTER TABLE notes ADD COLUMN reminder_offsets TEXT NOT NULL DEFAULT '0'")
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `targets` (
                `id` TEXT NOT NULL,
                `name` TEXT NOT NULL,
                `target_amount` REAL NOT NULL,
                `current_amount` REAL NOT NULL,
                `target_date` TEXT,
                `category_id` TEXT,
                `account_id` TEXT,
                `color` TEXT,
                `icon` TEXT,
                `status` TEXT NOT NULL,
                `notes` TEXT,
                `created_at` TEXT NOT NULL,
                `updated_at` TEXT NOT NULL,
                `is_deleted` INTEGER NOT NULL,
                PRIMARY KEY(`id`)
            )
            """.trimIndent()
        )
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_targets_category_id` ON `targets` (`category_id`)")
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_targets_account_id` ON `targets` (`account_id`)")
    }
}

