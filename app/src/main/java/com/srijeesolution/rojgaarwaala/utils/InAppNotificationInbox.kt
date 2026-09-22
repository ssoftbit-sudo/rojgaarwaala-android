package com.srijeesolution.rojgaarwaala.utils

data class InAppNotification(
    val type: String,
    val title: String,
    val body: String,
    val targetId: String,
    val receivedAt: Long,
    val read: Boolean = false,
) {
    val dedupeKey: String
        get() = if (targetId.isNotBlank()) "$type|$targetId" else "$type|$title|$body"
}

object InAppNotificationInbox {
    const val MAX_ITEMS = 50

    fun resolveTargetId(
        id: String? = null,
        scheduledImageId: String? = null,
        videoId: String? = null,
        applicationId: String? = null,
        categoryId: String? = null,
    ): String = listOf(id, scheduledImageId, videoId, applicationId, categoryId)
        .firstOrNull { !it.isNullOrBlank() }
        .orEmpty()

    fun upsert(
        existing: List<InAppNotification>,
        incoming: InAppNotification,
    ): List<InAppNotification> {
        val previous = existing.firstOrNull { it.dedupeKey == incoming.dedupeKey }
        val merged = if (previous == null) {
            incoming
        } else {
            incoming.copy(
                title = incoming.title.takeIf { it.isNotBlank() && !it.equals("Rojgaarwaala", true) }
                    ?: previous.title,
                body = incoming.body.ifBlank { previous.body },
            )
        }
        val rest = existing.filterNot { it.dedupeKey == incoming.dedupeKey }
        return (listOf(merged) + rest).take(MAX_ITEMS)
    }

    fun unreadCount(items: List<InAppNotification>): Int = items.count { !it.read }

    fun markAllRead(items: List<InAppNotification>): List<InAppNotification> =
        items.map { it.copy(read = true) }
}
