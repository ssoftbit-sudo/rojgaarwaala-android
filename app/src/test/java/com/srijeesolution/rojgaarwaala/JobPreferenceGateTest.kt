package com.srijeesolution.rojgaarwaala

import com.srijeesolution.rojgaarwaala.utils.JobPreferenceGate
import com.srijeesolution.rojgaarwaala.utils.JobPreferenceGate.Field
import com.srijeesolution.rojgaarwaala.utils.JobPreferenceGate.State
import com.srijeesolution.rojgaarwaala.utils.MainTabs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class JobPreferenceGateTest {

    private fun missing(
        categories: List<String>? = listOf("Helper"),
        legacyCategory: String? = null,
        address: String? = "Shankar Nagar, Raipur",
        latitude: Double? = 21.25,
        longitude: Double? = 81.63,
        district: String? = "Raipur",
    ) = JobPreferenceGate.missingFields(categories, legacyCategory, address, latitude, longitude, district)

    @Test
    fun `a profile with work, address and district misses nothing`() {
        assertEquals(emptyList<Field>(), missing())
        assertEquals(State.PRESENT, JobPreferenceGate.stateFor(missing()))
    }

    @Test
    fun `preferred work is read from the list or the older single value`() {
        assertEquals(emptyList<Field>(), missing(categories = emptyList(), legacyCategory = "Helper, Driver"))
        assertEquals(listOf(Field.PREFERRED_WORK), missing(categories = null, legacyCategory = null))
        assertEquals(listOf(Field.PREFERRED_WORK), missing(categories = listOf(" ", ""), legacyCategory = " , "))
    }

    @Test
    fun `a saved map pin counts as an address even without text`() {
        assertEquals(emptyList<Field>(), missing(address = null))
        assertEquals(emptyList<Field>(), missing(address = "Raipur", latitude = null, longitude = null))
        assertEquals(listOf(Field.ADDRESS), missing(address = " ", latitude = null, longitude = null))
        assertEquals(listOf(Field.ADDRESS), missing(address = null, latitude = 21.25, longitude = null))
    }

    @Test
    fun `district is required`() {
        assertEquals(listOf(Field.DISTRICT), missing(district = null))
        assertEquals(listOf(Field.DISTRICT), missing(district = "  "))
    }

    @Test
    fun `everything missing is listed in profile order`() {
        val all = missing(categories = null, address = null, latitude = null, longitude = null, district = null)

        assertEquals(listOf(Field.PREFERRED_WORK, Field.ADDRESS, Field.DISTRICT), all)
        assertEquals(State.MISSING, JobPreferenceGate.stateFor(all))
    }

    @Test
    fun `the prompt names only what is missing`() {
        val text = JobPreferenceGate.message(listOf(Field.DISTRICT))

        assertTrue(text.contains("किस जिले में काम ढूँढ रहे हैं"))
        assertFalse(text.contains("पसंदीदा काम"))
    }

    @Test
    fun `home and free job are locked when something is missing`() {
        assertTrue(JobPreferenceGate.blocks(State.MISSING, MainTabs.HOME))
        assertTrue(JobPreferenceGate.blocks(State.MISSING, MainTabs.FREE_JOB))
    }

    @Test
    fun `profile and the other tabs stay open so the user can fix it`() {
        assertFalse(JobPreferenceGate.blocks(State.MISSING, MainTabs.PROFILE))
        assertFalse(JobPreferenceGate.blocks(State.MISSING, MainTabs.CATEGORIES))
        assertFalse(JobPreferenceGate.blocks(State.MISSING, MainTabs.STORIES))
    }

    @Test
    fun `nothing is locked before the profile has loaded or once it is complete`() {
        assertFalse(JobPreferenceGate.blocks(State.UNKNOWN, MainTabs.HOME))
        assertFalse(JobPreferenceGate.blocks(State.UNKNOWN, MainTabs.FREE_JOB))
        assertFalse(JobPreferenceGate.blocks(State.PRESENT, MainTabs.HOME))
        assertFalse(JobPreferenceGate.blocks(State.PRESENT, MainTabs.FREE_JOB))
    }
}
