package moe.memesta.automemoria.di

import android.content.Context
import androidx.room.Room
import moe.memesta.automemoria.data.local.AppDatabase
import moe.memesta.automemoria.data.local.dao.*
import moe.memesta.automemoria.data.remote.SupabaseClientProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.jan.supabase.SupabaseClient
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, AppDatabase.DATABASE_NAME)
            .fallbackToDestructiveMigration() // Replace with proper migrations in production
            .build()

    @Provides fun provideHabitDao(db: AppDatabase): HabitDao = db.habitDao()
    @Provides fun provideHabitLogDao(db: AppDatabase): HabitLogDao = db.habitLogDao()
    @Provides fun provideGoalDao(db: AppDatabase): GoalDao = db.goalDao()
    @Provides fun provideGoalMilestoneDao(db: AppDatabase): GoalMilestoneDao = db.goalMilestoneDao()
    @Provides fun provideCalendarEventDao(db: AppDatabase): CalendarEventDao = db.calendarEventDao()
    @Provides fun provideBoardDao(db: AppDatabase): BoardDao = db.boardDao()
    @Provides fun provideBoardColumnDao(db: AppDatabase): BoardColumnDao = db.boardColumnDao()
    @Provides fun provideCardDao(db: AppDatabase): CardDao = db.cardDao()
    @Provides fun provideNoteDao(db: AppDatabase): NoteDao = db.noteDao()
    @Provides fun provideNoteLinkDao(db: AppDatabase): NoteLinkDao = db.noteLinkDao()
}

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideSupabaseClient(): SupabaseClient = SupabaseClientProvider.client
}
