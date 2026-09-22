package com.srijeesolution.rojgaarwaala

import com.srijeesolution.rojgaarwaala.utils.InAppNotification
import com.srijeesolution.rojgaarwaala.utils.InAppNotificationInbox
import com.srijeesolution.rojgaarwaala.utils.JobAlertNavigation
import com.srijeesolution.rojgaarwaala.utils.MainTabs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MainTabsTest {

    @Test
    fun `bottom bar is profile, category, home, free job, stories`() {
        assertEquals(
            listOf(
                MainTabs.PROFILE,
                MainTabs.CATEGORIES,
                MainTabs.HOME,
                MainTabs.FREE_JOB,
                MainTabs.STORIES,
            ),
            MainTabs.bottomOrder,
        )
        assertEquals(0, MainTabs.PROFILE)
        assertEquals(1, MainTabs.CATEGORIES)
        assertEquals(2, MainTabs.HOME)
        assertEquals(3, MainTabs.FREE_JOB)
        assertEquals(4, MainTabs.STORIES)
        assertEquals(5, MainTabs.ADD_JOB)
        assertEquals(MainTabs.FREE_JOB, JobAlertNavigation.TAB_FREE_JOB)
    }
}

class InAppNotificationInboxTest {

    @Test
    fun `new push is stored first and phone details stay in the teaser as given`() {
        val first = notice("paper_cut_job", "1", "Nurse", "यशवंत · रायपुर")
        val items = InAppNotificationInbox.upsert(emptyList(), first)
        assertEquals(listOf("1"), items.map { it.targetId })
        assertEquals(1, InAppNotificationInbox.unreadCount(items))
    }

    @Test
    fun `same job push updates in place instead of duplicating`() {
        val older = notice("paper_cut_job", "1", "Old", "old body", receivedAt = 1L)
        val newer = notice("paper_cut_job", "1", "New", "new body", receivedAt = 2L)
        val items = InAppNotificationInbox.upsert(listOf(older), newer)
        assertEquals(1, items.size)
        assertEquals("New", items.first().title)
        assertEquals("new body", items.first().body)
    }

    @Test
    fun `job status is not mixed into unread when all items are marked read`() {
        val items = InAppNotificationInbox.markAllRead(
            listOf(notice("job_application_status", "9", "Status", "Shortlisted")),
        )
        assertEquals(0, InAppNotificationInbox.unreadCount(items))
        assertTrue(items.all { it.read })
    }

    @Test
    fun `bell keeps every push type the user received, not only paper-cut`() {
        var items = emptyList<InAppNotification>()
        items = InAppNotificationInbox.upsert(items, notice("paper_cut_job", "1", "Nurse", "Raipur"))
        items = InAppNotificationInbox.upsert(items, notice("video_published", "88", "New Video", "Helper"))
        items = InAppNotificationInbox.upsert(items, notice("job_application_status", "9", "Shortlisted", "Cook"))
        items = InAppNotificationInbox.upsert(
            items,
            notice("attendance_punch_out_reminder", "3", "Punch out", "Shift ending"),
        )
        assertEquals(4, items.size)
        assertEquals(
            listOf(
                "attendance_punch_out_reminder",
                "job_application_status",
                "video_published",
                "paper_cut_job",
            ),
            items.map { it.type },
        )
    }

    @Test
    fun `video id is taken from video_id when data id is missing`() {
        assertEquals(
            "88",
            InAppNotificationInbox.resolveTargetId(videoId = "88", scheduledImageId = ""),
        )
        assertEquals(
            "9",
            InAppNotificationInbox.resolveTargetId(applicationId = "9"),
        )
    }

    private fun notice(
        type: String,
        id: String,
        title: String,
        body: String,
        receivedAt: Long = 10L,
    ) = InAppNotification(
        type = type,
        title = title,
        body = body,
        targetId = id,
        receivedAt = receivedAt,
        read = false,
    )
}
