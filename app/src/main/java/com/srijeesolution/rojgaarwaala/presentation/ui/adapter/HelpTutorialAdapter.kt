package com.srijeesolution.rojgaarwaala.presentation.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.srijeesolution.rojgaarwaala.R
import com.srijeesolution.rojgaarwaala.data.remote.model.HelpDeskTutorialItem

class HelpTutorialAdapter(
    private val onOpenVideo: (String) -> Unit,
    private val onOpenAudio: (String) -> Unit,
) : RecyclerView.Adapter<HelpTutorialAdapter.TutorialViewHolder>() {

    private val items = ArrayList<HelpDeskTutorialItem>()
    private val expandedIds = HashSet<Int>()

    fun submitList(list: List<HelpDeskTutorialItem>) {
        items.clear()
        items.addAll(list)
        expandedIds.clear()
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TutorialViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_help_tutorial, parent, false)
        return TutorialViewHolder(view)
    }

    override fun onBindViewHolder(holder: TutorialViewHolder, position: Int) {
        holder.bind(items[position], expandedIds.contains(items[position].id)) { id ->
            if (expandedIds.contains(id)) expandedIds.remove(id) else expandedIds.add(id)
            notifyItemChanged(position)
        }
    }

    override fun getItemCount(): Int = items.size

    inner class TutorialViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val titleView: TextView = itemView.findViewById(R.id.tutorialTitle)
        private val descriptionView: TextView = itemView.findViewById(R.id.tutorialDescription)
        private val textContentView: TextView = itemView.findViewById(R.id.tutorialTextContent)
        private val watchVideoButton: TextView = itemView.findViewById(R.id.tutorialWatchVideo)
        private val listenAudioButton: TextView = itemView.findViewById(R.id.tutorialListenAudio)

        fun bind(item: HelpDeskTutorialItem, expanded: Boolean, onToggle: (Int) -> Unit) {
            val id = item.id ?: 0
            titleView.text = item.title
            descriptionView.text = item.description.orEmpty()
            textContentView.text = item.textContent.orEmpty()
            textContentView.visibility = if (expanded && item.textContent?.isNotBlank() == true) View.VISIBLE else View.GONE

            val videoUrl = item.videoUrl.orEmpty()
            watchVideoButton.visibility = if (videoUrl.isNotBlank()) View.VISIBLE else View.GONE
            watchVideoButton.setOnClickListener { onOpenVideo(videoUrl) }

            val audioUrl = item.audioUrl.orEmpty()
            listenAudioButton.visibility = if (audioUrl.isNotBlank()) View.VISIBLE else View.GONE
            listenAudioButton.setOnClickListener { onOpenAudio(audioUrl) }

            itemView.setOnClickListener { if (id > 0) onToggle(id) }
        }
    }
}
