package com.example.buildlearn.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AchievementsTest {

    private val empty = LearnerSnapshot(
        completedLessons = 0,
        finishedCourses = 0,
        noteCount = 0,
        streakDays = 0,
        totalStudySeconds = 0
    )

    @Test
    fun noProgress_unlocksNothing() {
        assertEquals(emptySet<AchievementCode>(), evaluateAchievements(empty))
    }

    @Test
    fun firstCompletedLesson_unlocksFirstStep() {
        val result = evaluateAchievements(empty.copy(completedLessons = 1))
        assertEquals(setOf(AchievementCode.FIRST_LESSON), result)
    }

    @Test
    fun fiveNotes_unlocksNoteTaker_butFourDoesNot() {
        assertEquals(
            emptySet<AchievementCode>(),
            evaluateAchievements(empty.copy(noteCount = 4))
        )
        assertEquals(
            setOf(AchievementCode.NOTE_TAKER),
            evaluateAchievements(empty.copy(noteCount = 5))
        )
    }

    @Test
    fun oneHourOfStudy_unlocksStudy1h() {
        assertEquals(
            setOf(AchievementCode.STUDY_1H),
            evaluateAchievements(empty.copy(totalStudySeconds = 3600))
        )
    }

    @Test
    fun codeLookup_roundTrips() {
        assertEquals(AchievementCode.STREAK_3, AchievementCode.fromCode("STREAK_3"))
        assertNull(AchievementCode.fromCode("UNKNOWN"))
    }
}
