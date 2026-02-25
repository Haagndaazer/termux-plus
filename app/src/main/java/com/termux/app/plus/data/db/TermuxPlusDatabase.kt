package com.termux.app.plus.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.termux.app.plus.data.dao.ConnectionProfileDao
import com.termux.app.plus.data.dao.SnippetDao
import com.termux.app.plus.data.model.ConnectionProfile
import com.termux.app.plus.data.model.Snippet

@Database(
    entities = [ConnectionProfile::class, Snippet::class],
    version = 2,
    exportSchema = true
)
abstract class TermuxPlusDatabase : RoomDatabase() {

    abstract fun connectionProfileDao(): ConnectionProfileDao
    abstract fun snippetDao(): SnippetDao

    companion object {
        @Volatile
        private var INSTANCE: TermuxPlusDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE snippets ADD COLUMN autoExecute INTEGER NOT NULL DEFAULT 1")
            }
        }

        fun getInstance(context: Context): TermuxPlusDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    TermuxPlusDatabase::class.java,
                    "termux_plus.db"
                ).addMigrations(MIGRATION_1_2)
                .build().also { INSTANCE = it }
            }
        }
    }
}
