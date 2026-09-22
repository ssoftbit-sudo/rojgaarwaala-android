package com.srijeesolution.rojgaarwaala.presentation.ui.activity

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.srijeesolution.rojgaarwaala.R
import com.srijeesolution.rojgaarwaala.utils.InAppNotification
import com.srijeesolution.rojgaarwaala.utils.InAppNotificationStore
import com.srijeesolution.rojgaarwaala.utils.sp.SharedPrefs
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class NotificationsActivity : AppCompatActivity() {

    @Inject
    lateinit var sharedPrefs: SharedPrefs

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notifications)

        findViewById<View>(R.id.backButton).setOnClickListener { finish() }

        InAppNotificationStore.markAllRead(sharedPrefs)
        val items = InAppNotificationStore.list(sharedPrefs)
        val empty = findViewById<TextView>(R.id.emptyStateText)
        val list = findViewById<RecyclerView>(R.id.notificationsRecyclerView)
        if (items.isEmpty()) {
            empty.visibility = View.VISIBLE
            list.visibility = View.GONE
        } else {
            empty.visibility = View.GONE
            list.visibility = View.VISIBLE
            list.layoutManager = LinearLayoutManager(this)
            list.adapter = NotificationsAdapter(items) { openNotification(it) }
        }
    }

    private fun openNotification(item: InAppNotification) {
        if (item.type.isBlank()) return
        startActivity(
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra("type", item.type)
                putExtra("notification_type", item.type)
                putExtra("id", item.targetId)
                putExtra("notification_id", item.targetId)
                putExtra("scheduled_image_id", item.targetId)
                putExtra("application_id", item.targetId)
                putExtra("video_id", item.targetId)
                putExtra("from_inbox", true)
            },
        )
    }
}

private class NotificationsAdapter(
    private val items: List<InAppNotification>,
    private val onClick: (InAppNotification) -> Unit,
) : RecyclerView.Adapter<NotificationsAdapter.Holder>() {

    private val timeFormat = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_in_app_notification, parent, false)
        return Holder(view)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val item = items[position]
        holder.title.text = item.title.ifBlank { holder.itemView.context.getString(R.string.app_name) }
        holder.body.text = item.body
        holder.body.visibility = if (item.body.isBlank()) View.GONE else View.VISIBLE
        holder.time.text = if (item.receivedAt > 0) timeFormat.format(Date(item.receivedAt)) else ""
        holder.unread.visibility = if (item.read) View.GONE else View.VISIBLE
        holder.itemView.setOnClickListener { onClick(item) }
    }

    override fun getItemCount(): Int = items.size

    class Holder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.notificationTitle)
        val body: TextView = view.findViewById(R.id.notificationBody)
        val time: TextView = view.findViewById(R.id.notificationTime)
        val unread: View = view.findViewById(R.id.unreadDot)
    }
}
