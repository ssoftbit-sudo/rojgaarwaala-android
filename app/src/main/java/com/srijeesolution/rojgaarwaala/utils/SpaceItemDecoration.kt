package com.srijeesolution.rojgaarwaala.utils

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

class SpaceItemDecoration(
    private val spaceHorizontal: Int,
    private val spaceVertical: Int
) : RecyclerView.ItemDecoration() {

    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        outRect.apply {
            left = spaceHorizontal
            right = spaceHorizontal
            top = spaceVertical
            bottom = spaceVertical
        }
    }

}