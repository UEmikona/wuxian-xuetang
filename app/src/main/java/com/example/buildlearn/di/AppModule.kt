package com.example.buildlearn.di

import android.content.Context
import androidx.room.Room
import com.example.buildlearn.data.db.AchievementDao
import com.example.buildlearn.data.db.AppDatabase
import com.example.buildlearn.data.db.CourseDao
import com.example.buildlearn.data.db.FavoriteDao
import com.example.buildlearn.data.db.LessonDao
import com.example.buildlearn.data.db.NoteDao
import com.example.buildlearn.data.db.ProgressDao
import com.example.buildlearn.data.db.PurchaseDao
import com.example.buildlearn.data.db.ReviewDao
import com.example.buildlearn.data.db.StudyDayDao
import com.example.buildlearn.data.db.UserDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "buildlearn.db")
            // 版本 1 从未对外发布。版本 2 起的结构变化直接重建数据库；
            // 之后正式发布时应改为编写 Migration。
            .fallbackToDestructiveMigration(true)
            .build()

    @Provides fun provideUserDao(db: AppDatabase): UserDao = db.userDao()
    @Provides fun provideCourseDao(db: AppDatabase): CourseDao = db.courseDao()
    @Provides fun provideLessonDao(db: AppDatabase): LessonDao = db.lessonDao()
    @Provides fun provideProgressDao(db: AppDatabase): ProgressDao = db.progressDao()
    @Provides fun provideNoteDao(db: AppDatabase): NoteDao = db.noteDao()
    @Provides fun provideAchievementDao(db: AppDatabase): AchievementDao = db.achievementDao()
    @Provides fun provideStudyDayDao(db: AppDatabase): StudyDayDao = db.studyDayDao()
    @Provides fun provideReviewDao(db: AppDatabase): ReviewDao = db.reviewDao()
    @Provides fun provideFavoriteDao(db: AppDatabase): FavoriteDao = db.favoriteDao()
    @Provides fun providePurchaseDao(db: AppDatabase): PurchaseDao = db.purchaseDao()
}
