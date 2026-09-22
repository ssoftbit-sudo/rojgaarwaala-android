package com.srijeesolution.rojgaarwaala.utils

import com.srijeesolution.rojgaarwaala.utils.sp.SharedPrefs
import com.srijeesolution.rojgaarwaala.utils.sp.SharedPrefsConstant
import org.json.JSONArray
import org.json.JSONObject

object InAppNotificationStore {

    fun add(prefs: SharedPrefs, item: InAppNotification) {
        persist(prefs, InAppNotificationInbox.upsert(list(prefs), item))
    }

    fun list(prefs: SharedPrefs): List<InAppNotification> {
        val raw = prefs.getPrefs(SharedPrefsConstant.IN_APP_NOTIFICATIONS, "").orEmpty()
        if (raw.isBlank()) return emptyList()
        return try {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    val obj = array.optJSONObject(i) ?: continue
                    add(
                        InAppNotification(
                            type = obj.optString("type"),
                            title = obj.optString("title"),
                            body = obj.optString("body"),
                            targetId = obj.optString("targetId"),
                            receivedAt = obj.optLong("receivedAt"),
                            read = obj.optBoolean("read"),
                        ),
                    )
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun unreadCount(prefs: SharedPrefs): Int = InAppNotificationInbox.unreadCount(list(prefs))

    fun markAllRead(prefs: SharedPrefs) {
        persist(prefs, InAppNotificationInbox.markAllRead(list(prefs)))
    }

    private fun persist(prefs: SharedPrefs, items: List<InAppNotification>) {
        val array = JSONArray()
        items.forEach { item ->
            array.put(
                JSONObject()
                    .put("type", item.type)
                    .put("title", item.title)
                    .put("body", item.body)
                    .put("targetId", item.targetId)
                    .put("receivedAt", item.receivedAt)
                    .put("read", item.read),
            )
        }
        prefs.setPrefsData(Pair(SharedPrefsConstant.IN_APP_NOTIFICATIONS, array.toString()))
        prefs.setPrefsData(
            Pair(
                SharedPrefsConstant.NOTIFICATION_BADGE_PENDING,
                InAppNotificationInbox.unreadCount(items) > 0,
            ),
        )
    }
}
