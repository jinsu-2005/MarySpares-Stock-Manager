package com.marytwowheelers.spares.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters

@Database(
    entities = [
        PartEntity::class,
        MovementEntity::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun partDao(): PartDao
    abstract fun movementDao(): MovementDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "mary_spares_database"
                )
                .fallbackToDestructiveMigration() // V1, so no migrations needed yet
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

class Converters {
    @TypeConverter
    fun fromSyncState(value: SyncState): String {
        return value.name
    }

    @TypeConverter
    fun toSyncState(value: String): SyncState {
        return SyncState.valueOf(value)
    }

    @TypeConverter
    fun fromMovementType(value: MovementType): String {
        return value.name
    }

    @TypeConverter
    fun toMovementType(value: String): MovementType {
        return MovementType.valueOf(value)
    }
}
