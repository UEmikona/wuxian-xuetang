package com.example.buildlearn.domain

import java.time.LocalDate

/** 课时完成阈值：播放到总时长的 95% 即视为完成。 */
const val LESSON_COMPLETE_RATIO = 0.95

/** 学习日阈值：当天累计学习满 60 秒，才算一个学习日。 */
const val STUDY_DAY_MIN_SECONDS = 60L

/** 判断课时是否已完成。总时长未知（<= 0）时不自动判定。 */
fun isLessonCompleted(positionMs: Long, durationMs: Long): Boolean {
    if (durationMs <= 0L) return false
    return positionMs >= durationMs * LESSON_COMPLETE_RATIO
}

/** 课程完成百分比，取整数。 */
fun coursePercent(completedLessons: Int, totalLessons: Int): Int {
    if (totalLessons <= 0) return 0
    return completedLessons * 100 / totalLessons
}

/**
 * 连续学习天数。
 * 从今天（若今天还没学习，则从昨天）开始向前数，直到遇到没有学习的一天。
 * 这样即使今天还没开始学习，连续记录也不会立刻断掉。
 */
fun currentStreak(studyDays: Set<LocalDate>, today: LocalDate): Int {
    var day = if (today in studyDays) today else today.minusDays(1)
    var count = 0
    while (day in studyDays) {
        count++
        day = day.minusDays(1)
    }
    return count
}
