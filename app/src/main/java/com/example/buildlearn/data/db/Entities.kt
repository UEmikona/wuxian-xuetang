package com.example.buildlearn.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** 本地账号。密码只保存哈希与盐，不保存明文。 */
@Entity(
    tableName = "users",
    indices = [Index(value = ["username"], unique = true)]
)
data class UserEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val username: String,
    val passwordHash: String,
    val salt: String,
    val createdAt: Long
)

/** 课程。内容来自 assets/courses.json。priceCents 为 0 表示免费。 */
@Entity(tableName = "courses")
data class CourseEntity(
    @PrimaryKey val id: String,
    val title: String,
    val subtitle: String,
    val description: String,
    val instructor: String,
    val category: String,
    val priceCents: Int,
    val coverColor: String,
    val rating: Double,
    val ratingCount: Int,
    val studentCount: Int,
    val featured: Boolean,
    val orderIndex: Int
)

/** 课程评论，来自 assets/courses.json。 */
@Entity(
    tableName = "reviews",
    foreignKeys = [
        ForeignKey(
            entity = CourseEntity::class,
            parentColumns = ["id"],
            childColumns = ["courseId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("courseId")]
)
data class ReviewEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val courseId: String,
    val author: String,
    val rating: Int,
    val content: String,
    val date: String,
    val orderIndex: Int
)

/** 收藏：每位用户对每门课程最多一条。 */
@Entity(tableName = "favorites", primaryKeys = ["userId", "courseId"])
data class FavoriteEntity(
    val userId: Long,
    val courseId: String,
    val createdAt: Long
)

/** 购买（或免费领取）记录：拥有该课程的学习权限。 */
@Entity(tableName = "purchases", primaryKeys = ["userId", "courseId"])
data class PurchaseEntity(
    val userId: Long,
    val courseId: String,
    val purchasedAt: Long
)

/** 课时。视频地址可以是 https 直链，也可以是 HLS（.m3u8）。 */
@Entity(
    tableName = "lessons",
    foreignKeys = [
        ForeignKey(
            entity = CourseEntity::class,
            parentColumns = ["id"],
            childColumns = ["courseId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("courseId")]
)
data class LessonEntity(
    @PrimaryKey val id: String,
    val courseId: String,
    val title: String,
    val videoUrl: String,
    val durationMs: Long,
    val orderIndex: Int
)

/** 每位用户、每个课时的进度。completed 一旦为 true 就不会再被改回 false。 */
@Entity(tableName = "progress", primaryKeys = ["userId", "lessonId"])
data class ProgressEntity(
    val userId: Long,
    val lessonId: String,
    val positionMs: Long,
    val completed: Boolean,
    val updatedAt: Long
)

/** 笔记，记录写下时的播放位置，点击可以跳回对应时间点。 */
@Entity(
    tableName = "notes",
    indices = [Index("userId"), Index(value = ["userId", "lessonId"])]
)
data class NoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long,
    val lessonId: String,
    val positionMs: Long,
    val text: String,
    val createdAt: Long
)

/** 已解锁的成就，code 对应 AchievementCode 的枚举名。 */
@Entity(tableName = "achievements", primaryKeys = ["userId", "code"])
data class AchievementEntity(
    val userId: Long,
    val code: String,
    val unlockedAt: Long
)

/** 每日学习时长，day 格式为 yyyy-MM-dd。 */
@Entity(tableName = "study_days", primaryKeys = ["userId", "day"])
data class StudyDayEntity(
    val userId: Long,
    val day: String,
    val seconds: Long
)
