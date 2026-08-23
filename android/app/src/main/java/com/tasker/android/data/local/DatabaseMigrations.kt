package com.tasker.android.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE notes ADD COLUMN reminder_at TEXT DEFAULT NULL")
        database.execSQL("ALTER TABLE notes ADD COLUMN reminder_offsets TEXT NOT NULL DEFAULT '0'")
    }
}
