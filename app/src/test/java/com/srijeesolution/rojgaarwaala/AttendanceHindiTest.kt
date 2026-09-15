package com.srijeesolution.rojgaarwaala

import com.srijeesolution.rojgaarwaala.utils.AttendanceHindi
import org.junit.Assert.assertEquals
import org.junit.Test

class AttendanceHindiTest {

    @Test
    fun `maps greetings the dashboard shows`() {
        assertEquals("सुप्रभात", AttendanceHindi.greeting("Good Morning"))
        assertEquals("नमस्कार", AttendanceHindi.greeting("Good Afternoon"))
        assertEquals("शुभ संध्या", AttendanceHindi.greeting("Good Evening"))
        assertEquals("स्वागत है", AttendanceHindi.greeting(null))
    }

    @Test
    fun `maps attendance status chips`() {
        assertEquals("हाजिर", AttendanceHindi.status("Present"))
        assertEquals("गैरहाजिर", AttendanceHindi.status("absent"))
        assertEquals("आधा दिन", AttendanceHindi.status("half_day"))
        assertEquals("हाजिरी नहीं लगी", AttendanceHindi.status("Not Marked"))
        assertEquals("0 घंटे", AttendanceHindi.status("0 Hours"))
        assertEquals("लग गई", AttendanceHindi.status("Marked"))
        assertEquals("पेंडिंग", AttendanceHindi.status("pending"))
        assertEquals("अप्रूव्ड", AttendanceHindi.status("approved"))
        assertEquals("रिजेक्ट", AttendanceHindi.status("rejected"))
    }

    @Test
    fun `maps punch types labour and staff see`() {
        assertEquals("पंच इन", AttendanceHindi.punchType("punch_in"))
        assertEquals("पंच आउट", AttendanceHindi.punchType("Punch Out"))
        assertEquals("-", AttendanceHindi.punchType(null))
    }
}
