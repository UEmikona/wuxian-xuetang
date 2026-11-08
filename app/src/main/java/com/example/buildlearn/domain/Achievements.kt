package com.example.buildlearn.domain

/**
 * 成就定义。新增成就时，只需在这里加一个枚举值，并在 [evaluateAchievements] 中补充判断条件。
 */
enum class AchievementCode(val title: String, val description: String) {
    FIRST_LESSON("第一步", "完成第一个课时"),
    FINISH_COURSE("结业", "完成一门课程的全部课时"),
    NOTE_TAKER("做笔记的人", "累计写下 5 条笔记"),
    STREAK_3("三日不辍", "连续 3 天学习"),
    STUDY_1H("一小时学者", "累计学习满 1 小时");

    companion object {
        fun fromCode(code: String): AchievementCode? = entries.firstOrNull { it.name == code }
    }
}

/** 判断成就所需的学习统计快照。 */
data class LearnerSnapshot(
    val completedLessons: Int,
    val finishedCourses: Int,
    val noteCount: Int,
    val streakDays: Int,
    val totalStudySeconds: Long
)

/**
 * 根据统计快照，计算当前满足条件的全部成就。
 * 成就一旦解锁就不会失效，调用方应只做"新增"，不做"删除"。
 */
fun evaluateAchievements(snapshot: LearnerSnapshot): Set<AchievementCode> = buildSet {
    if (snapshot.completedLessons >= 1) add(AchievementCode.FIRST_LESSON)
    if (snapshot.finishedCourses >= 1) add(AchievementCode.FINISH_COURSE)
    if (snapshot.noteCount >= 5) add(AchievementCode.NOTE_TAKER)
    if (snapshot.streakDays >= 3) add(AchievementCode.STREAK_3)
    if (snapshot.totalStudySeconds >= 3600L) add(AchievementCode.STUDY_1H)
}
