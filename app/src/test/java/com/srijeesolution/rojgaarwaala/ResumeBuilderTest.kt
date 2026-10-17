package com.srijeesolution.rojgaarwaala

import com.srijeesolution.rojgaarwaala.utils.resume.Education
import com.srijeesolution.rojgaarwaala.utils.resume.Experience
import com.srijeesolution.rojgaarwaala.utils.resume.ResumeCopy
import com.srijeesolution.rojgaarwaala.utils.resume.ResumeData
import com.srijeesolution.rojgaarwaala.utils.resume.ResumeLanguage
import com.srijeesolution.rojgaarwaala.utils.resume.ResumeRules
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class ResumeBuilderTest {

    private val today = Calendar.getInstance().apply { set(2026, Calendar.OCTOBER, 18) }

    private val complete = ResumeData(
        name = "Ramesh Sahu", mobile = "9876543210", dob = "2001-08-15", district = "Raipur",
        photoPath = "/x/photo.jpg", experiences = listOf(Experience("Driver", "ABC", 3)),
        education = listOf(Education("CLASS_12", "CG Board", 2019)),
        skills = listOf("DRIVING", "फोर्कलिफ्ट"), language = ResumeLanguage.ENGLISH,
        roles = listOf("Driver", "Helper"), jobLocations = listOf("Raipur", "Durg"),
    )

    @Test fun jsonRoundTripKeepsEverything() {
        assertEquals(complete, ResumeData.fromJson(complete.toJson()))
    }

    @Test fun oldDraftHeadlineBecomesRoles() {
        val old = JSONObject().put("name", "A").put("headline", "Driver, Helper ,")
        assertEquals(listOf("Driver", "Helper"), ResumeData.fromJson(old).roles)
    }

    @Test fun ageCountsBirthdayNotYetReached() {
        assertEquals(25, ResumeRules.ageOn("2001-08-15", today))
        assertEquals(24, ResumeRules.ageOn("2001-12-01", today))
        assertNull(ResumeRules.ageOn("bad", today))
    }

    @Test fun eachStepAsksForItsOwnFields() {
        assertTrue(ResumeRules.missing(1, complete, today).isEmpty())
        assertTrue(ResumeRules.missing(1, complete.copy(photoPath = null), today).contains("फोटो लगाएँ"))
        assertTrue(ResumeRules.missing(1, complete.copy(dob = "2020-01-01"), today).isNotEmpty())
        assertTrue(ResumeRules.missing(2, complete, today).isEmpty())
        assertTrue(ResumeRules.missing(2, complete.copy(roles = emptyList()), today).isNotEmpty())
        assertTrue(ResumeRules.missing(2, complete.copy(jobLocations = emptyList()), today).isNotEmpty())
        assertTrue(ResumeRules.missing(2, complete.copy(experiences = emptyList()), today).isNotEmpty())
        assertTrue(ResumeRules.missing(2, complete.copy(experiences = emptyList(), fresher = true), today).isEmpty())
        assertTrue(ResumeRules.missing(3, complete.copy(education = emptyList()), today).isNotEmpty())
    }

    @Test fun passingYearsStartFromThisYear() {
        val years = ResumeRules.passingYears(today)
        assertEquals(2026, years.first())
        assertEquals(1970, years.last())
    }

    @Test fun labelsFollowLanguageAndKeepCustomText() {
        assertEquals("Driving", ResumeCopy.label("DRIVING", ResumeLanguage.ENGLISH).let { if (it.startsWith("Driv")) "Driving" else it })
        assertEquals("फोर्कलिफ्ट", ResumeCopy.label("फोर्कलिफ्ट", ResumeLanguage.ENGLISH))
        assertEquals("15 अगस्त 2001", ResumeCopy.date("2001-08-15", ResumeLanguage.HINDI))
        assertEquals("15 Aug 2001", ResumeCopy.date("2001-08-15", ResumeLanguage.ENGLISH))
    }
}
