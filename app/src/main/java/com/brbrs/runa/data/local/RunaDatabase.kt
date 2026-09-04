package com.brbrs.runa.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.brbrs.runa.data.local.dao.JournalEntryDao
import com.brbrs.runa.data.local.entity.JournalEntryEntity

@Database(
    entities = [JournalEntryEntity::class],
    version  = 3,
    exportSchema = false,
)
abstract class RunaDatabase : RoomDatabase() {
    abstract fun journalEntryDao(): JournalEntryDao

    companion object {
        const val DATABASE_NAME = "runa.db"

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE journal_entries ADD COLUMN locationName TEXT")
                db.execSQL("ALTER TABLE journal_entries ADD COLUMN latitude REAL")
                db.execSQL("ALTER TABLE journal_entries ADD COLUMN longitude REAL")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE journal_entries ADD COLUMN tagsJson TEXT NOT NULL DEFAULT '[]'")
            }
        }
    }
}
