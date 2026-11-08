package com.example.buildlearn.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class LearningRulesTest {

    @Test
    fun lesson_isCompleted_atNinetyFivePercent() {
        assertTrue(isLessonCompleted(positionMs = 95_000, durationMs = 100_000))
        assertFalse(isLessonCompleted(positionMs = 94_999, durationMs = 100_000))
    }

    @Test
    fun lesson_withUnknownDuration_isNotCompleted() {
        assertFalse(isLessonCompleted(positionMs = 999_999, durationMs = 0))
    }

    @Test
    fun coursePercent_handlesEmptyCourse() {
        assertEquals(0, coursePercent(completedLessons = 0, totalLessons = 0))
        assertEquals(50, coursePercent(completedLessons = 2, totalLessons = 4))
        assertEquals(33, coursePercent(completedLessons = 1, totalLessons = 3))
    }

    @Test
    fun streak_countsBackFromToday() {
        val today = LocalDate.of(2026, 11, 7)
        val days = setOf(today, today.minusDays(1), today.minusDays(2))
        assertEquals(3, currentStreak(days, today))
    }

    @Test
    fun streak_keepsCountingFromYesterdayIfTodayNotStudiedYet() {
        val today = LocalDate.of(2026, 11, 7)
        val days = setOf(today.minusDays(1), today.minusDays(2))
        assertEquals(2, currentStreak(days, today))
    }

    @Test
    fun streak_breaksOnGap() {
        val today = LocalDate.of(2026, 11, 7)
        val days = setOf(today, today.minusDays(2))
        assertEquals(1, currentStreak(days, today))
    }

    @Test
    fun streak_isZeroWithoutRecentStudy() {
        val today = LocalDate.of(2026, 11, 7)
        assertEquals(0, currentStreak(setOf(today.minusDays(5)), today))
    }
}
