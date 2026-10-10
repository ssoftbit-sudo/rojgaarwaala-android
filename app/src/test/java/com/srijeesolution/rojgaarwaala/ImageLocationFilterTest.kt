package com.srijeesolution.rojgaarwaala

import com.srijeesolution.rojgaarwaala.data.remote.model.ImageData
import com.srijeesolution.rojgaarwaala.utils.FreeJobFeed
import com.srijeesolution.rojgaarwaala.utils.ImageLocationFilter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ImageLocationFilterTest {

    @Test
    fun `a job matches the district the admin filed it under`() {
        // The Google address never says Durg, which is why the old text match missed it.
        val job = ImageData(id = 1, location = "Sector 6, Civic Centre", districts = listOf("Durg"))

        assertTrue(ImageLocationFilter.matches(job, "Durg"))
        assertTrue(ImageLocationFilter.matches(job, " durg "))
        assertFalse(ImageLocationFilter.matches(job, "Raipur"))
    }

    @Test
    fun `a job filed under several districts shows in each of them`() {
        val job = ImageData(id = 2, districts = listOf("Raipur", "Durg"))

        assertTrue(ImageLocationFilter.matches(job, "Raipur"))
        assertTrue(ImageLocationFilter.matches(job, "Durg"))
        assertFalse(ImageLocationFilter.matches(job, "Korba"))
    }

    @Test
    fun `filed districts win over what the address text says`() {
        val job = ImageData(id = 3, location = "Station Road, Korba", districts = listOf("Raipur"))

        assertTrue(ImageLocationFilter.matches(job, "Raipur"))
        assertFalse(ImageLocationFilter.matches(job, "Korba"))
    }

    @Test
    fun `short and joined district names are the same district`() {
        assertTrue(ImageLocationFilter.matches(ImageData(id = 4, districts = listOf("Janjgir-Champa")), "Janjgir"))
        assertTrue(ImageLocationFilter.matches(ImageData(id = 5, districts = listOf("Janjgir")), "Janjgir-Champa"))
    }

    @Test
    fun `Balod and Baloda Bazar stay apart`() {
        assertFalse(ImageLocationFilter.matches(ImageData(id = 6, districts = listOf("Baloda Bazar")), "Balod"))
        assertFalse(ImageLocationFilter.matches(ImageData(id = 7, districts = listOf("Balod")), "Baloda Bazar"))
    }

    @Test
    fun `a job with no districts still matches on its address text`() {
        val fromOldBackend = ImageData(id = 8, location = "Samta Colony, Raipur")

        assertTrue(ImageLocationFilter.matches(fromOldBackend, "Raipur"))
        assertFalse(ImageLocationFilter.matches(fromOldBackend, "Durg"))
        assertTrue(ImageLocationFilter.matches(ImageData(id = 9, location = "Raipur", districts = emptyList()), "Raipur"))
    }

    @Test
    fun `all Chhattisgarh shows every job`() {
        val job = ImageData(id = 10, districts = listOf("Durg"))

        assertTrue(ImageLocationFilter.matches(job, "All Chhattisgarh"))
        assertTrue(ImageLocationFilter.matches(job, ""))
    }

    @Test
    fun `tile view previews the first five jobs of a category`() {
        val jobs = (1..8).map { ImageData(id = it) }

        assertEquals(listOf(1, 2, 3, 4, 5), FreeJobFeed.tilePreview(jobs).map { it.id })
        assertEquals(3, FreeJobFeed.tilePreview(jobs.take(3)).size)
    }
}
