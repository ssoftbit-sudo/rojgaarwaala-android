package com.srijeesolution.rojgaarwaala.presentation.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.srijeesolution.rojgaarwaala.R

sealed class HelpDeskListItem {
    data class CategoryHeader(val title: String) : HelpDeskListItem()
    data class FaqRow(val id: Int, val question: String, val answer: String) : HelpDeskListItem()
}

class HelpFaqAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val items = ArrayList<HelpDeskListItem>()
    private val expandedIds = HashSet<Int>()

    fun submitCategories(categories: List<Pair<String, List<Triple<Int, String, String>>>>) {
        items.clear()
        categories.forEach { (category, faqs) ->
            items.add(HelpDeskListItem.CategoryHeader(category))
            faqs.forEach { (id, question, answer) ->
                items.add(HelpDeskListItem.FaqRow(id, question, answer))
            }
        }
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is HelpDeskListItem.CategoryHeader -> VIEW_TYPE_CATEGORY
            is HelpDeskListItem.FaqRow -> VIEW_TYPE_FAQ
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == VIEW_TYPE_CATEGORY) {
            CategoryViewHolder(inflater.inflate(R.layout.item_help_faq_category, parent, false))
        } else {
            FaqViewHolder(inflater.inflate(R.layout.item_help_faq, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is HelpDeskListItem.CategoryHeader -> {
                (holder as CategoryViewHolder).bind(item.title)
            }
            is HelpDeskListItem.FaqRow -> {
                (holder as FaqViewHolder).bind(item, expandedIds.contains(item.id)) {
                    if (expandedIds.contains(item.id)) {
                        expandedIds.remove(item.id)
                    } else {
                        expandedIds.add(item.id)
                    }
                    notifyItemChanged(position)
                }
            }
        }
    }

    override fun getItemCount(): Int = items.size

    private class CategoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val titleView: TextView = itemView.findViewById(R.id.categoryTitle)

        fun bind(title: String) {
            titleView.text = title
        }
    }

    private class FaqViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val questionView: TextView = itemView.findViewById(R.id.faqQuestion)
        private val answerView: TextView = itemView.findViewById(R.id.faqAnswer)
        private val chevronView: TextView = itemView.findViewById(R.id.faqChevron)

        fun bind(item: HelpDeskListItem.FaqRow, expanded: Boolean, onToggle: () -> Unit) {
            questionView.text = item.question
            answerView.text = item.answer
            answerView.visibility = if (expanded) View.VISIBLE else View.GONE
            chevronView.text = if (expanded) "−" else "+"
            itemView.setOnClickListener { onToggle() }
        }
    }

    companion object {
        private const val VIEW_TYPE_CATEGORY = 0
        private const val VIEW_TYPE_FAQ = 1
    }
}
