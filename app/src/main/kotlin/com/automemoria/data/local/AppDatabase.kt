package moe.memesta.automemoria.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import moe.memesta.automemoria.data.local.dao.*
import moe.memesta.automemoria.data.local.entity.*

@Database(
    entities = [
        HabitEntity::class,
        HabitLogEntity::class,
        GoalEntity::class,
        GoalMilestoneEntity::class,
        CalendarEventEntity::class,
        BoardEntity::class,
        BoardColumnEntity::class,
        CardEntity::class,
        NoteEntity::class,
        NoteLinkEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun habitDao(): HabitDao
    abstract fun habitLogDao(): HabitLogDao
    abstract fun goalDao(): GoalDao
    abstract fun goalMilestoneDao(): GoalMilestoneDao
    abstract fun calendarEventDao(): CalendarEventDao
    abstract fun boardDao(): BoardDao
    abstract fun boardColumnDao(): BoardColumnDao
    abstract fun cardDao(): CardDao
    abstract fun noteDao(): NoteDao
    abstract fun noteLinkDao(): NoteLinkDao

    companion object {
        const val DATABASE_NAME = "automemoria.db"
    }
}
