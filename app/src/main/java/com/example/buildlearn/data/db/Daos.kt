package com.example.buildlearn.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(user: UserEntity): Long

    @Query("SELECT * FROM users WHERE username = :username LIMIT 1")
    suspend fun findByName(username: String): UserEntity?

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    suspend fun findById(id: Long): UserEntity?
}

@Dao
interface CourseDao {
    @Query("SELECT * FROM courses ORDER BY orderIndex, title")
    fun observeAll(): Flow<List<CourseEntity>>

    @Query("SELECT * FROM courses ORDER BY orderIndex, title")
    suspend fun getAll(): List<CourseEntity>

    @Query("DELETE FROM courses")
    suspend fun deleteAll()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(courses: List<CourseEntity>)

    /** 整体替换目录：课程内容以 assets 为准，进度与笔记保存在其他表中，不受影响。 */
    @Transaction
    suspend fun replaceAll(courses: List<CourseEntity>) {
        deleteAll()
        insertAll(courses)
    }
}

@Dao
interface LessonDao {
    @Query("SELECT * FROM lessons WHERE courseId = :courseId ORDER BY orderIndex")
    fun observeByCourse(courseId: String): Flow<List<LessonEntity>>

    @Query("SELECT * FROM lessons WHERE courseId = :courseId ORDER BY orderIndex")
    suspend fun getByCourse(courseId: String): List<LessonEntity>

    @Query("SELECT * FROM lessons")
    fun observeAll(): Flow<List<LessonEntity>>

    @Query("SELECT * FROM lessons")
    suspend fun getAll(): List<LessonEntity>

    @Query("SELECT * FROM lessons WHERE id = :lessonId LIMIT 1")
    suspend fun getById(lessonId: String): LessonEntity?

    @Query("DELETE FROM lessons")
    suspend fun deleteAll()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(lessons: List<LessonEntity>)
}

@Dao
interface ProgressDao {
    @Query("SELECT * FROM progress WHERE userId = :userId")
    fun observeAll(userId: Long): Flow<List<ProgressEntity>>

    @Query("SELECT * FROM progress WHERE userId = :userId")
    suspend fun getAll(userId: Long): List<ProgressEntity>

    @Query("SELECT * FROM progress WHERE userId = :userId AND lessonId = :lessonId LIMIT 1")
    suspend fun get(userId: Long, lessonId: String): ProgressEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(progress: ProgressEntity)
}

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes WHERE userId = :userId ORDER BY createdAt DESC")
    fun observeAll(userId: Long): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE userId = :userId AND lessonId = :lessonId ORDER BY positionMs")
    fun observeForLesson(userId: Long, lessonId: String): Flow<List<NoteEntity>>

    @Query("SELECT COUNT(*) FROM notes WHERE userId = :userId")
    suspend fun count(userId: Long): Int

    @Insert
    suspend fun insert(note: NoteEntity): Long

    @Query("DELETE FROM notes WHERE id = :noteId AND userId = :userId")
    suspend fun delete(noteId: Long, userId: Long)
}

@Dao
interface AchievementDao {
    @Query("SELECT * FROM achievements WHERE userId = :userId")
    fun observeAll(userId: Long): Flow<List<AchievementEntity>>

    @Query("SELECT * FROM achievements WHERE userId = :userId")
    suspend fun getAll(userId: Long): List<AchievementEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfAbsent(achievement: AchievementEntity): Long
}

@Dao
interface StudyDayDao {
    @Query("SELECT * FROM study_days WHERE userId = :userId")
    fun observeAll(userId: Long): Flow<List<StudyDayEntity>>

    @Query("SELECT * FROM study_days WHERE userId = :userId")
    suspend fun getAll(userId: Long): List<StudyDayEntity>

    /** 累加学习秒数，不存在时新建。 */
    @Query(
        """
        INSERT INTO study_days (userId, day, seconds) VALUES (:userId, :day, :seconds)
        ON CONFLICT(userId, day) DO UPDATE SET seconds = seconds + excluded.seconds
        """
    )
    suspend fun addSeconds(userId: Long, day: String, seconds: Long)
}

@Dao
interface ReviewDao {
    @Query("SELECT * FROM reviews WHERE courseId = :courseId ORDER BY orderIndex")
    fun observeByCourse(courseId: String): Flow<List<ReviewEntity>>

    @Query("DELETE FROM reviews")
    suspend fun deleteAll()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(reviews: List<ReviewEntity>)
}

@Dao
interface FavoriteDao {
    @Query("SELECT * FROM favorites WHERE userId = :userId ORDER BY createdAt DESC")
    fun observeAll(userId: Long): Flow<List<FavoriteEntity>>

    @Query("SELECT * FROM favorites WHERE userId = :userId AND courseId = :courseId LIMIT 1")
    suspend fun get(userId: Long, courseId: String): FavoriteEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(favorite: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE userId = :userId AND courseId = :courseId")
    suspend fun delete(userId: Long, courseId: String)
}

@Dao
interface PurchaseDao {
    @Query("SELECT * FROM purchases WHERE userId = :userId ORDER BY purchasedAt DESC")
    fun observeAll(userId: Long): Flow<List<PurchaseEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfAbsent(purchase: PurchaseEntity): Long
}
