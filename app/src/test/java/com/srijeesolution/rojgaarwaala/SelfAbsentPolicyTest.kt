package com.srijeesolution.rojgaarwaala

import com.srijeesolution.rojgaarwaala.utils.SelfAbsentPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SelfAbsentPolicyTest {

    @Test
    fun `button is offered only when the server allows it`() {
        assertTrue(SelfAbsentPolicy.canOffer(canMarkAbsent = true, selfAbsent = false))
        assertFalse(SelfAbsentPolicy.canOffer(canMarkAbsent = false, selfAbsent = false))
    }

    @Test
    fun `button stays hidden against a backend that does not send the flag`() {
        assertFalse(SelfAbsentPolicy.canOffer(canMarkAbsent = null, selfAbsent = null))
    }

    @Test
    fun `button is not offered twice in a day`() {
        assertFalse(SelfAbsentPolicy.canOffer(canMarkAbsent = true, selfAbsent = true))
    }

    @Test
    fun `marking absent silences reminders`() {
        assertTrue(SelfAbsentPolicy.silencesReminders(selfAbsent = true, punchInAt = null))
        assertTrue(SelfAbsentPolicy.silencesReminders(selfAbsent = true, punchInAt = ""))
    }

    @Test
    fun `reminders are not silenced without an absent mark`() {
        assertFalse(SelfAbsentPolicy.silencesReminders(selfAbsent = false, punchInAt = null))
        assertFalse(SelfAbsentPolicy.silencesReminders(selfAbsent = null, punchInAt = null))
    }

    @Test
    fun `a punch in after the absent mark brings punch out reminders back`() {
        assertFalse(SelfAbsentPolicy.silencesReminders(selfAbsent = true, punchInAt = "09:40 AM"))
    }

    @Test
    fun `notice carries the reason when there is one`() {
        assertTrue(SelfAbsentPolicy.notice("बीमार हूँ").contains("कारण: बीमार हूँ"))
        assertFalse(SelfAbsentPolicy.notice(null).contains("कारण"))
        assertFalse(SelfAbsentPolicy.notice(" ").contains("कारण"))
    }

    @Test
    fun `team list marks a self reported absence with its reason`() {
        assertEquals("गैरहाजिर, खुद बताया: बीमार हूँ", SelfAbsentPolicy.teamStatus("गैरहाजिर", true, "बीमार हूँ"))
        assertEquals("गैरहाजिर, खुद बताया", SelfAbsentPolicy.teamStatus("गैरहाजिर", true, null))
    }

    @Test
    fun `team list leaves other statuses alone`() {
        assertEquals("गैरहाजिर", SelfAbsentPolicy.teamStatus("गैरहाजिर", false, "बीमार हूँ"))
        assertEquals("हाजिर", SelfAbsentPolicy.teamStatus("हाजिर", null, null))
    }

    @Test
    fun `last reason is the catch all`() {
        assertEquals(SelfAbsentPolicy.REASON_OTHER, SelfAbsentPolicy.REASONS.last())
    }
}
