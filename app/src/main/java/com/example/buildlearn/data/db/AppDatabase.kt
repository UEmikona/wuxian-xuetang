package com.example.buildlearn.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        UserEntity::class,
        CourseEntity::class,
        LessonEntity::class,
        ProgressEntity::class,
        NoteEntity::class,
        AchievementEntity::class,
        StudyDayEntity::class,
        ReviewEntity::class,
        FavoriteEntity::class,
        PurchaseEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun courseDao(): CourseDao
    abstract fun lessonDao(): LessonDao
    abstract fun progressDao(): ProgressDao
    abstract fun noteDao(): NoteDao
    abstract fun achievementDao(): AchievementDao
    abstract fun studyDayDao(): StudyDayDao
    abstract fun reviewDao(): ReviewDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun purchaseDao(): PurchaseDao
}
