package com.srijeesolution.rojgaarwaala

import com.srijeesolution.rojgaarwaala.data.remote.model.ImageData
import com.srijeesolution.rojgaarwaala.data.remote.model.ImageSubItem
import com.srijeesolution.rojgaarwaala.data.remote.model.UserData
import com.srijeesolution.rojgaarwaala.utils.FreeJobFeed
import com.srijeesolution.rojgaarwaala.utils.FreeJobItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FreeJobFeedTest {

    @Test
    fun `jobs with place text show as cards even without coordinates`() {
        val withPin = ImageData(id = 1, title = "Guard", latitude = 21.25, longitude = 81.63)
        val withCity = ImageData(id = 3, title = "Office Assistant", location = "Raipur, Chhattisgarh")
        val poster = ImageData(id = 2, title = "Paper cut")
        val categories = listOf(
            ImageSubItem(id = 1, title = "Hospital", images = listOf(withPin, withCity, poster)),
        )
        val items = FreeJobFeed.flatten(categories)
        assertEquals(listOf(1, 3), FreeJobFeed.withLocation(items).map { it.job.id })
        assertEquals(listOf(2), FreeJobFeed.withoutLocation(items).map { it.job.id })
        assertEquals(listOf(2), FreeJobFeed.categoriesWithoutLocation(categories).flatMap { it.images.orEmpty() }.map { it.id })
        assertEquals(
            listOf(1, 3, 2),
            FreeJobFeed.sortedCategories(categories, FreeJobFeed.Sort.NEWEST)
                .flatMap { it.images.orEmpty() }
                .map { it.id },
        )
    }

    @Test
    fun `company falls back to category then app name`() {
        val named = FreeJobFeed.flatten(
            listOf(
                ImageSubItem(
                    title = "Hospital",
                    images = listOf(ImageData(id = 1, title = "Nurse", user = UserData(name = "Reliance"))),
                ),
            ),
        ).first()
        assertEquals("Reliance", FreeJobFeed.companyName(named))
        val noUser = FreeJobFeed.flatten(
            listOf(ImageSubItem(title = "Driver", images = listOf(ImageData(id = 2, title = "Helper")))),
        ).first()
        assertEquals("Driver", FreeJobFeed.companyName(noUser))
    }

    @Test
    fun `salary uses offer when salary_text is blank`() {
        val job = ImageData(id = 4, title = "Helper", offer = "₹11,000 - ₹16,000 / Month")
        assertEquals("₹11,000 - ₹16,000 / Month", FreeJobFeed.salaryLine(job))
        assertTrue(FreeJobFeed.placeLine(job).isEmpty())
    }

    @Test
    fun `place prefers area name over long geocoded location`() {
        val job = ImageData(
            id = 5,
            title = "Nurse",
            areaName = "Raipur, Chhattisgarh",
            location = "Yashwant Hospital, Raipur, G.E. Road, India",
        )
        assertEquals("Raipur, Chhattisgarh", FreeJobFeed.placeLine(job))
        assertTrue(FreeJobFeed.hasLocation(job))
    }

    @Test
    fun `map uri uses coordinates when present otherwise searches the place`() {
        val pinned = ImageData(id = 6, title = "Nurse", latitude = 21.25, longitude = 81.63)
        assertEquals("geo:21.25,81.63?q=21.25,81.63", FreeJobFeed.mapGeoUri(pinned))
        val cityOnly = ImageData(id = 7, title = "Helper", location = "Raipur, Chhattisgarh")
        assertEquals("geo:0,0?q=Raipur%2C%20Chhattisgarh", FreeJobFeed.mapGeoUri(cityOnly))
        val poster = ImageData(id = 8, title = "Paper cut")
        assertEquals(null, FreeJobFeed.mapGeoUri(poster))
    }

    @Test
    fun `merge categories appends new jobs without dropping the first page`() {
        val first = listOf(
            ImageSubItem(id = 1, title = "Hospital", images = listOf(ImageData(id = 10, title = "Nurse"))),
        )
        val second = listOf(
            ImageSubItem(id = 1, title = "Hospital", images = listOf(ImageData(id = 11, title = "Helper"))),
            ImageSubItem(id = 2, title = "Driver", images = listOf(ImageData(id = 12, title = "Driver"))),
        )
        val merged = FreeJobFeed.mergeCategories(first, second)
        assertEquals(listOf(1, 2), merged.map { it.id })
        assertEquals(listOf(10, 11), merged.first().images.orEmpty().map { it.id })
        assertEquals(listOf(12), merged.last().images.orEmpty().map { it.id })
    }

    @Test
    fun `append keeps already loaded jobs in place`() {
        val first = listOf(FreeJobItem(ImageData(id = 10, title = "Nurse"), "Hospital"))
        val extra = listOf(
            FreeJobItem(ImageData(id = 11, title = "Helper"), "Hospital"),
            FreeJobItem(ImageData(id = 10, title = "Nurse"), "Hospital"),
        )
        val merged = FreeJobFeed.appendItems(first, extra)
        assertEquals(listOf(10, 11), merged.map { it.job.id })
    }

    fun `newest first puts later dates at the top`() {
        val older = FreeJobItem(ImageData(id = 1, title = "Old", createdAt = "2026-01-01 10:00:00"), "Hospital")
        val newer = FreeJobItem(ImageData(id = 2, title = "New", createdAt = "2026-09-20 10:00:00"), "Hospital")
        assertEquals(listOf(2, 1), FreeJobFeed.sortedItems(listOf(older, newer), FreeJobFeed.Sort.NEWEST).map { it.job.id })
        assertEquals(listOf(1, 2), FreeJobFeed.sortedItems(listOf(older, newer), FreeJobFeed.Sort.OLDEST).map { it.job.id })
    }
}
