package com.example.buildlearn.data.repo

import com.example.buildlearn.data.db.AchievementDao
import com.example.buildlearn.data.db.AchievementEntity
import com.example.buildlearn.data.db.LessonEntity
import com.example.buildlearn.data.db.NoteDao
import com.example.buildlearn.data.db.NoteEntity
import com.example.buildlearn.data.db.ProgressDao
import com.example.buildlearn.data.db.ProgressEntity
import com.example.buildlearn.data.db.StudyDayDao
import com.example.buildlearn.domain.AchievementCode
import com.example.buildlearn.domain.LearnerSnapshot
import com.example.buildlearn.domain.STUDY_DAY_MIN_SECONDS
import com.example.buildlearn.domain.currentStreak
import com.example.buildlearn.domain.evaluateAchievements
import com.example.buildlearn.domain.isLessonCompleted
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/** 学习相关的数据操作：进度、笔记、学习时长与成就。 */
@Singleton
class LearningRepository @Inject constructor(
    private val progressDao: ProgressDao,
    private val noteDao: NoteDao,
    private val achievementDao: AchievementDao,
    private val studyDayDao: StudyDayDao,
    private val courses: CourseRepository
) {
    fun observeProgress(userId: Long): Flow<List<ProgressEntity>> = progressDao.observeAll(userId)

    suspend fun getProgress(userId: Long, lessonId: String): ProgressEntity? =
        progressDao.get(userId, lessonId)

    /**
     * 保存播放位置。durationMs 应优先使用播放器读到的真实时长。
     * 一旦完成就保持完成状态，不会被后续的较小位置覆盖。
     * 返回本次是否由"未完成"变为"已完成"。
     */
    suspend fun savePosition(
        userId: Long,
        lessonId: String,
        positionMs: Long,
        durationMs: Long
    ): Boolean {
        val existing = progressDao.get(userId, lessonId)
        val wasCompleted = existing?.completed == true
        val completed = wasCompleted || isLessonCompleted(positionMs, durationMs)
        progressDao.upsert(
            ProgressEntity(
                userId = userId,
                lessonId = lessonId,
                positionMs = positionMs,
                completed = completed,
                updatedAt = System.currentTimeMillis()
            )
        )
        return completed && !wasCompleted
    }

    /** 手动标记完成。 */
    suspend fun markCompleted(userId: Long, lessonId: String) {
        val existing = progressDao.get(userId, lessonId)
        progressDao.upsert(
            ProgressEntity(
                userId = userId,
                lessonId = lessonId,
                positionMs = existing?.positionMs ?: 0L,
                completed = true,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun addStudySeconds(userId: Long, seconds: Long) {
        if (seconds <= 0) return
        studyDayDao.addSeconds(userId, today().toString(), seconds)
    }

    fun observeNotes(userId: Long): Flow<List<NoteEntity>> = noteDao.observeAll(userId)

    fun observeNotesForLesson(userId: Long, lessonId: String): Flow<List<NoteEntity>> =
        noteDao.observeForLesson(userId, lessonId)

    suspend fun addNote(userId: Long, lessonId: String, positionMs: Long, text: String) {
        noteDao.insert(
            NoteEntity(
                userId = userId,
                lessonId = lessonId,
                positionMs = positionMs,
                text = text.trim(),
                createdAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun deleteNote(userId: Long, noteId: Long) = noteDao.delete(noteId, userId)

    fun observeAchievements(userId: Long): Flow<List<AchievementEntity>> =
        achievementDao.observeAll(userId)

    /** 重新计算成就，返回本次新解锁的成就。 */
    suspend fun unlockNewAchievements(userId: Long): List<AchievementCode> {
        val snapshot = buildSnapshot(userId)
        val earned = evaluateAchievements(snapshot)
        val now = System.currentTimeMillis()
        val newlyUnlocked = mutableListOf<AchievementCode>()
        for (code in earned) {
            val inserted = achievementDao.insertIfAbsent(
                AchievementEntity(userId = userId, code = code.name, unlockedAt = now)
            )
            if (inserted != -1L) newlyUnlocked += code
        }
        return newlyUnlocked
    }

    /** 学习统计，供个人页与成就判断使用。 */
    fun observeStudyDays(userId: Long): Flow<Map<LocalDate, Long>> =
        studyDayDao.observeAll(userId).map { rows ->
            rows.associate { LocalDate.parse(it.day) to it.seconds }
        }

    suspend fun buildSnapshot(userId: Long): LearnerSnapshot {
        val progress = progressDao.getAll(userId)
        val completedIds = progress.filter { it.completed }.map { it.lessonId }.toSet()

        val lessonsByCourse = courses.getAllLessons().groupBy { it.courseId }
        val finishedCourses = lessonsByCourse.values.count { lessons ->
            lessons.isNotEmpty() && lessons.all { it.id in completedIds }
        }

        val studyDays = studyDayDao.getAll(userId)
        val activeDays = studyDays
            .filter { it.seconds >= STUDY_DAY_MIN_SECONDS }
            .map { LocalDate.parse(it.day) }
            .toSet()

        return LearnerSnapshot(
            completedLessons = completedIds.size,
            finishedCourses = finishedCourses,
            noteCount = noteDao.count(userId),
            streakDays = currentStreak(activeDays, today()),
            totalStudySeconds = studyDays.sumOf { it.seconds }
        )
    }

    private fun today(): LocalDate = LocalDate.now(ZoneId.systemDefault())
}
