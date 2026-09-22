package com.srijeesolution.rojgaarwaala

import com.srijeesolution.rojgaarwaala.data.remote.model.ImageData
import com.srijeesolution.rojgaarwaala.data.remote.model.ImageSubItem
import com.srijeesolution.rojgaarwaala.utils.JobMapPins
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class JobMapPinsTest {

    @Test
    fun `only jobs with lat long become map pins`() {
        val pinned = ImageData(id = 1, title = "Nurse", latitude = 21.25, longitude = 81.63)
        val remote = ImageData(id = 2, title = "Driver")
        val categories = listOf(
            ImageSubItem(id = 1, title = "Hospital", images = listOf(pinned, remote)),
        )
        val pins = JobMapPins.withCoordinates(categories)
        assertEquals(listOf(1), pins.map { it.id })
    }

    @Test
    fun `distance from current location is labelled in km`() {
        val jobs = listOf(ImageData(id = 1, title = "Nurse", latitude = 21.2514, longitude = 81.6296))
        val withKm = JobMapPins.withDistanceFrom(jobs, 21.2514, 81.6396)
        val km = withKm.first().distanceKm
        assertTrue(km != null && km > 0)
        assertTrue(JobMapPins.distanceLabel(km).startsWith("आपसे"))
    }

    @Test
    fun `jobs without user location stay on the map without a distance`() {
        val job = ImageData(id = 3, title = "Cook", latitude = 21.2, longitude = 81.6)
        val pins = JobMapPins.withDistanceFrom(listOf(job), null, null)
        assertEquals(null, pins.first().distanceKm)
        assertEquals("", JobMapPins.distanceLabel(null))
    }
}
