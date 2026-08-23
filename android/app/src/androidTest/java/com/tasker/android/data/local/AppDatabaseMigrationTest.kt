package com.tasker.android.data.local

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppDatabaseMigrationTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val databaseName = "migration-test.db"

    @Before
    @After
    fun deleteTestDatabase() {
        context.deleteDatabase(databaseName)
    }

    @Test
    fun migrationFrom1To2PreservesExistingNote() {
        val versionOne = openHelper(version = 1) { database ->
            database.execSQL(
                """
                CREATE TABLE notes (
                    id TEXT NOT NULL PRIMARY KEY,
                    title TEXT NOT NULL,
                    content_md TEXT NOT NULL,
                    created_at TEXT NOT NULL,
                    updated_at TEXT NOT NULL,
                    is_deleted INTEGER NOT NULL
                )
                """.trimIndent()
            )
            database.execSQL("CREATE INDEX index_notes_updated_at ON notes(updated_at)")
            database.execSQL(
                """
                INSERT INTO notes (id, title, content_md, created_at, updated_at, is_deleted)
                VALUES ('note-1', 'Persistent note', 'Still here', '2026-08-01', '2026-08-01', 0)
                """.trimIndent()
            )
        }
        versionOne.writableDatabase
        versionOne.close()

        val versionTwo = openHelper(version = 2) { }.also { helper ->
            helper.writableDatabase
        }
        versionTwo.readableDatabase.query(
            "SELECT title, content_md, reminder_at, reminder_offsets FROM notes WHERE id = 'note-1'"
        ).use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("Persistent note", cursor.getString(0))
            assertEquals("Still here", cursor.getString(1))
            assertNull(cursor.getString(2))
            assertEquals("0", cursor.getString(3))
        }
        versionTwo.close()
    }

    private fun openHelper(
        version: Int,
        onCreate: (SupportSQLiteDatabase) -> Unit,
    ): SupportSQLiteOpenHelper = FrameworkSQLiteOpenHelperFactory().create(
        SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(databaseName)
            .callback(
                object : SupportSQLiteOpenHelper.Callback(version) {
                    override fun onCreate(database: SupportSQLiteDatabase) = onCreate(database)

                    override fun onUpgrade(
                        database: SupportSQLiteDatabase,
                        oldVersion: Int,
                        newVersion: Int,
                    ) {
                        MIGRATION_1_2.migrate(database)
                    }
                }
            )
            .build()
    )
}
