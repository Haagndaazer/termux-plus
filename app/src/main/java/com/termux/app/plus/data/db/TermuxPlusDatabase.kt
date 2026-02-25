package com.termux.app.plus.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.termux.app.plus.data.dao.ConnectionProfileDao
import com.termux.app.plus.data.dao.SnippetDao
import com.termux.app.plus.data.model.ConnectionProfile
import com.termux.app.plus.data.model.Snippet

@Database(
    entities = [ConnectionProfile::class, Snippet::class],
    version = 1,
    exportSchema = true
)
abstract class TermuxPlusDatabase : RoomDatabase() {

    abstract fun connectionProfileDao(): ConnectionProfileDao
    abstract fun snippetDao(): SnippetDao

    companion object {
        @Volatile
        private var INSTANCE: TermuxPlusDatabase? = null

        fun getInstance(context: Context): TermuxPlusDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    TermuxPlusDatabase::class.java,
                    "termux_plus.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
