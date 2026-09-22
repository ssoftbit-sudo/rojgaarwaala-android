package com.srijeesolution.rojgaarwaala

import com.srijeesolution.rojgaarwaala.data.remote.model.ImageData
import com.srijeesolution.rojgaarwaala.data.remote.model.ImageSubItem
import com.srijeesolution.rojgaarwaala.utils.JobAlertFeed
import org.junit.Assert.assertEquals
import org.junit.Test

class JobAlertFeedTest {

    @Test
    fun `preferred category jobs are listed even when other categories exist`() {
        val hospital = ImageData(id = 1, title = "Nurse")
        val driver = ImageData(id = 2, title = "Driver")
        val categories = listOf(
            ImageSubItem(id = 1, title = "Hospital", images = listOf(hospital)),
            ImageSubItem(id = 2, title = "Driver", images = listOf(driver)),
        )

        val alerts = JobAlertFeed.imagesForPreferredCategory(categories, "Hospital")
        assertEquals(listOf(1), alerts.map { it.id })
    }

    @Test
    fun `when category is missing the published jobs still show`() {
        val job = ImageData(id = 9, title = "Helper")
        val categories = listOf(
            ImageSubItem(id = 3, title = "Helper/Labour", images = listOf(job)),
        )

        val alerts = JobAlertFeed.imagesForPreferredCategory(categories, "")
        assertEquals(listOf(9), alerts.map { it.id })
    }
}
