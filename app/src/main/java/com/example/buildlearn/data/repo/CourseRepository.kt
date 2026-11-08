package com.example.buildlearn.data.repo

import android.content.Context
import com.example.buildlearn.data.db.CourseDao
import com.example.buildlearn.data.db.CourseEntity
import com.example.buildlearn.data.db.LessonDao
import com.example.buildlearn.data.db.LessonEntity
import com.example.buildlearn.data.db.ReviewDao
import com.example.buildlearn.data.db.ReviewEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 课程目录。
 *
 * 内容来源：assets/courses.json（占位内容）。格式见 docs/COURSES.md。
 * 启动时读取该文件并替换数据库中的目录与评论；收藏、购买、进度和笔记保存在其他表中，不受影响。
 */
@Singleton
class CourseRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val courseDao: CourseDao,
    private val lessonDao: LessonDao,
    private val reviewDao: ReviewDao
) {
    fun observeCourses(): Flow<List<CourseEntity>> = courseDao.observeAll()

    fun observeAllLessons(): Flow<List<LessonEntity>> = lessonDao.observeAll()

    fun observeLessons(courseId: String): Flow<List<LessonEntity>> = lessonDao.observeByCourse(courseId)

    fun observeReviews(courseId: String): Flow<List<ReviewEntity>> = reviewDao.observeByCourse(courseId)

    suspend fun getCourse(courseId: String): CourseEntity? =
        courseDao.getAll().firstOrNull { it.id == courseId }

    suspend fun getLesson(lessonId: String): LessonEntity? = lessonDao.getById(lessonId)

    suspend fun getLessonsOf(courseId: String): List<LessonEntity> = lessonDao.getByCourse(courseId)

    suspend fun getAllLessons(): List<LessonEntity> = lessonDao.getAll()

    suspend fun getAllCourses(): List<CourseEntity> = courseDao.getAll()

    /** 从 assets 读取目录并写入数据库。文件缺失或格式错误时保留原有目录。 */
    suspend fun reloadCatalogFromAssets() {
        val text = runCatching {
            context.assets.open(CATALOG_FILE).bufferedReader().use { it.readText() }
        }.getOrNull() ?: return

        val root = runCatching { JSONObject(text) }.getOrNull() ?: return
        val courseArray = root.optJSONArray("courses") ?: return

        // 先在内存中完整解析，任何字段错误都会在写库前抛出，保证原目录不被破坏。
        val courses = mutableListOf<CourseEntity>()
        val lessons = mutableListOf<LessonEntity>()
        val reviews = mutableListOf<ReviewEntity>()

        for (i in 0 until courseArray.length()) {
            val c = courseArray.getJSONObject(i)
            val courseId = c.getString("id")
            courses += CourseEntity(
                id = courseId,
                title = c.getString("title"),
                subtitle = c.optString("subtitle", ""),
                description = c.optString("description", ""),
                instructor = c.optString("instructor", ""),
                category = c.optString("category", "其他"),
                priceCents = c.optInt("priceCents", 0),
                coverColor = c.optString("coverColor", "#5B6CFF"),
                rating = c.optDouble("rating", 0.0),
                ratingCount = c.optInt("ratingCount", 0),
                studentCount = c.optInt("studentCount", 0),
                featured = c.optBoolean("featured", false),
                orderIndex = i
            )

            c.optJSONArray("lessons")?.let { lessonArray ->
                for (j in 0 until lessonArray.length()) {
                    val l = lessonArray.getJSONObject(j)
                    lessons += LessonEntity(
                        id = l.getString("id"),
                        courseId = courseId,
                        title = l.getString("title"),
                        videoUrl = l.getString("videoUrl"),
                        durationMs = l.optLong("durationMs", 0L),
                        orderIndex = j
                    )
                }
            }

            c.optJSONArray("reviews")?.let { reviewArray ->
                for (k in 0 until reviewArray.length()) {
                    val r = reviewArray.getJSONObject(k)
                    reviews += ReviewEntity(
                        courseId = courseId,
                        author = r.getString("author"),
                        rating = r.optInt("rating", 5),
                        content = r.getString("content"),
                        date = r.optString("date", ""),
                        orderIndex = k
                    )
                }
            }
        }

        lessonDao.deleteAll()
        courseDao.replaceAll(courses)
        lessonDao.insertAll(lessons)
        reviewDao.deleteAll()
        reviewDao.insertAll(reviews)
    }

    companion object {
        const val CATALOG_FILE = "courses.json"
    }
}
