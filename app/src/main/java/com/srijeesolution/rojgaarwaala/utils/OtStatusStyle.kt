package com.srijeesolution.rojgaarwaala.utils

import android.widget.TextView
import com.srijeesolution.rojgaarwaala.R

object OtStatusStyle {
    fun apply(view: TextView, status: String?) {
        val color = when (status?.trim()?.lowercase()) {
            "approved" -> R.color.color_green
            "rejected" -> R.color.new_tag_red
            else -> R.color.accent
        }
        view.setTextColor(view.context.getColor(color))
    }
}
