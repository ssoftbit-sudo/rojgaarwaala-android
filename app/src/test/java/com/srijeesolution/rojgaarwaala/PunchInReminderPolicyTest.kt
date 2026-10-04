package com.srijeesolution.rojgaarwaala

import com.srijeesolution.rojgaarwaala.utils.GeofenceEvaluator
import com.srijeesolution.rojgaarwaala.utils.PunchInReminderPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PunchInReminderPolicyTest {

    @Test
    fun `only a recorded attendance stops the reminders`() {
        assertTrue(PunchInReminderPolicy.attendanceRecorded("09:02", attendanceMarked = true, canPunchIn = false))
        // Leave or holiday already marked by the server, punching no longer offered.
        assertTrue(PunchInReminderPolicy.attendanceRecorded(null, attendanceMarked = true, canPunchIn = false))
        // At the factory with a GPS fix but no punch: keep reminding.
        assertFalse(PunchInReminderPolicy.attendanceRecorded(null, attendanceMarked = false, canPunchIn = true))
        assertFalse(PunchInReminderPolicy.attendanceRecorded("", attendanceMarked = null, canPunchIn = true))
    }

    @Test
    fun `tells the employee they have arrived or how far they are`() {
        assertEquals(
            "✅ आप फैक्ट्री पहुंच गए हैं।\nPunch In करें",
            PunchInReminderPolicy.arrivalMessage(GeofenceEvaluator.Status.INSIDE, 40.0),
        )
        assertEquals(
            "📍 आप फैक्ट्री से 850 मीटर दूर हैं।\nफैक्ट्री पहुंचने के बाद Punch In करें।",
            PunchInReminderPolicy.arrivalMessage(GeofenceEvaluator.Status.OUTSIDE, 849.6),
        )
        assertNull(PunchInReminderPolicy.arrivalMessage(GeofenceEvaluator.Status.LOCATING, null))
        assertEquals("2.4 किलोमीटर", PunchInReminderPolicy.hindiDistance(2_400.0))
        assertEquals("3 किलोमीटर", PunchInReminderPolicy.hindiDistance(3_000.0))
    }

    @Test
    fun `the reminder asks the employee to open the app and punch in`() {
        assertEquals(
            "आपके ऑफिस जाने का समय होने वाला है। ऑफिस/फैक्ट्री पहुंचकर ऐप खोलें और Punch In करें।",
            PunchInReminderPolicy.NOTIFICATION_BODY,
        )
    }
}
