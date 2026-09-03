package com.srijeesolution.rojgaarwaala.presentation.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.srijeesolution.rojgaarwaala.R
import com.srijeesolution.rojgaarwaala.data.remote.model.HelpDeskFaqSuggestion

class HelpSuggestionAdapter : RecyclerView.Adapter<HelpSuggestionAdapter.SuggestionViewHolder>() {

    private val items = ArrayList<HelpDeskFaqSuggestion>()
    private val expandedIds = HashSet<Int>()

    fun submitList(list: List<HelpDeskFaqSuggestion>) {
        items.clear()
        items.addAll(list)
        expandedIds.clear()
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SuggestionViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_help_suggestion, parent, false)
        return SuggestionViewHolder(view)
    }

    override fun onBindViewHolder(holder: SuggestionViewHolder, position: Int) {
        holder.bind(items[position], expandedIds.contains(items[position].id)) { id ->
            if (expandedIds.contains(id)) expandedIds.remove(id) else expandedIds.add(id)
            notifyItemChanged(position)
        }
    }

    override fun getItemCount(): Int = items.size

    class SuggestionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val questionView: TextView = itemView.findViewById(R.id.suggestionQuestion)
        private val answerView: TextView = itemView.findViewById(R.id.suggestionAnswer)

        fun bind(item: HelpDeskFaqSuggestion, expanded: Boolean, onToggle: (Int) -> Unit) {
            questionView.text = item.question
            answerView.text = item.answer
            answerView.visibility = if (expanded) View.VISIBLE else View.GONE
            itemView.setOnClickListener {
                item.id?.let(onToggle)
            }
        }
    }
}
