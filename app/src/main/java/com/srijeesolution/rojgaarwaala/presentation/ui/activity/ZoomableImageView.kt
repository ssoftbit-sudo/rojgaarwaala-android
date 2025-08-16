package com.srijeesolution.rojgaarwaala.presentation.ui.activity

import android.content.Context
import android.graphics.Matrix
import android.graphics.PointF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.widget.ImageView
import kotlin.math.max
import kotlin.math.min

class ZoomableImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : androidx.appcompat.widget.AppCompatImageView(context, attrs, defStyleAttr) {

    private var scaleGestureDetector: ScaleGestureDetector
    private var matrix: Matrix = Matrix()
    private var mode = NONE
    private var oldDist = 1f
    private var d = 0f
    private var newRot = 0f
    private var last = PointF()
    private var start = PointF()
    private var minScale = 0.5f
    private var maxScale = 3.0f
    private var initialScale = 1f
    private var saveScale = 1f
    private var viewWidth = 0
    private var viewHeight = 0
    private var origWidth = 0f
    private var origHeight = 0f
    private var oldMatrix = Matrix()
    private var matrixValues = FloatArray(9)
    private var onImageClickListener: (() -> Unit)? = null
    private var lastClickTime = 0L

    companion object {
        private const val NONE = 0
        private const val DRAG = 1
        private const val ZOOM = 2
        private const val CLICK = 3
        private const val CLICK_TIME_SPAN = 200L
    }

    init {
        super.setClickable(true)
        scaleGestureDetector = ScaleGestureDetector(context, ScaleListener())
        matrix.setTranslate(1f, 1f)
        imageMatrix = matrix
        scaleType = ScaleType.MATRIX
    }

    fun setOnImageClickListener(listener: () -> Unit) {
        onImageClickListener = listener
    }

    fun resetZoom() {
        matrix.reset()
        setImageMatrix()
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        viewWidth = w
        viewHeight = h
    }

    private fun fixTrans() {
        matrix.getValues(matrixValues)
        val transX = matrixValues[Matrix.MTRANS_X]
        val transY = matrixValues[Matrix.MTRANS_Y]
        val fixTransX = getFixTrans(transX, viewWidth.toFloat(), origWidth * saveScale)
        val fixTransY = getFixTrans(transY, viewHeight.toFloat(), origHeight * saveScale)
        if (fixTransX != 0f || fixTransY != 0f) matrix.postTranslate(fixTransX, fixTransY)
    }

    private fun getFixTrans(trans: Float, viewSize: Float, contentSize: Float): Float {
        val minTrans: Float
        val maxTrans: Float
        if (contentSize <= viewSize) {
            minTrans = 0f
            maxTrans = viewSize - contentSize
        } else {
            minTrans = viewSize - contentSize
            maxTrans = 0f
        }
        return when {
            trans < minTrans -> minTrans - trans
            trans > maxTrans -> maxTrans - trans
            else -> 0f
        }
    }

    private fun getFixDragTrans(delta: Float, viewSize: Float, contentSize: Float): Float {
        return if (contentSize <= viewSize) {
            0f
        } else {
            delta
        }
    }

    private fun setImageMatrix() {
        val drawable = drawable ?: return
        val drawableWidth = drawable.intrinsicWidth
        val drawableHeight = drawable.intrinsicHeight
        
        if (drawableWidth <= 0 || drawableHeight <= 0 || viewWidth <= 0 || viewHeight <= 0) {
            return
        }
        
        // Calculate scale to fit the image within the view
        val scaleX = viewWidth.toFloat() / drawableWidth
        val scaleY = viewHeight.toFloat() / drawableHeight
        val scale = min(scaleX, scaleY)
        
        // Reset matrix and set the initial scale
        matrix.reset()
        matrix.setScale(scale, scale)
        
        // Center the image
        val redundantYSpace = viewHeight - scale * drawableHeight
        val redundantXSpace = viewWidth - scale * drawableWidth
        matrix.postTranslate(redundantXSpace / 2, redundantYSpace / 2)
        
        // Store original dimensions
        origWidth = drawableWidth.toFloat()
        origHeight = drawableHeight.toFloat()
        
        // Set initial scale factor
        initialScale = scale
        saveScale = scale
        
        // Apply the matrix
        imageMatrix = matrix
        invalidate()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleGestureDetector.onTouchEvent(event)
        val curr = PointF(event.x, event.y)
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                last.set(curr)
                start.set(last)
                mode = DRAG
            }
            MotionEvent.ACTION_MOVE -> {
                if (mode == DRAG) {
                    val deltaX = curr.x - last.x
                    val deltaY = curr.y - last.y
                    val fixTransX = getFixDragTrans(deltaX, viewWidth.toFloat(), origWidth * saveScale)
                    val fixTransY = getFixDragTrans(deltaY, viewHeight.toFloat(), origHeight * saveScale)
                    matrix.postTranslate(fixTransX, fixTransY)
                    fixTrans()
                    last.set(curr.x, curr.y)
                }
            }
            MotionEvent.ACTION_UP -> {
                mode = NONE
                val xDiff = (curr.x - start.x).toInt()
                val yDiff = (curr.y - start.y).toInt()
                if (xDiff < CLICK_TIME_SPAN && yDiff < CLICK_TIME_SPAN) {
                    val currentTime = System.currentTimeMillis()
                    if (currentTime - lastClickTime < 300) {
                        // Double tap - reset zoom
                        resetZoom()
                    } else {
                        // Single tap
                        performClick()
                        onImageClickListener?.invoke()
                    }
                    lastClickTime = currentTime
                }
            }
            MotionEvent.ACTION_POINTER_UP -> mode = NONE
        }
        imageMatrix = matrix
        invalidate()
        return true
    }

    override fun performClick(): Boolean {
        return super.performClick()
    }

    private inner class ScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
            mode = ZOOM
            return true
        }

        override fun onScale(detector: ScaleGestureDetector): Boolean {
            val mScaleFactor = detector.scaleFactor
            val origScale = saveScale
            saveScale *= mScaleFactor
            if (saveScale > initialScale * maxScale) {
                saveScale = initialScale * maxScale
            } else if (saveScale < initialScale * minScale) {
                saveScale = initialScale * minScale
            }
            val scaleFactor = saveScale / origScale
            matrix.postScale(scaleFactor, scaleFactor, detector.focusX, detector.focusY)
            fixTrans()
            return true
        }
    }

    override fun setImageResource(resId: Int) {
        super.setImageResource(resId)
        setImageMatrix()
    }
    
    override fun setImageDrawable(drawable: android.graphics.drawable.Drawable?) {
        super.setImageDrawable(drawable)
        if (drawable != null) {
            post {
                setImageMatrix()
            }
        }
    }
} 