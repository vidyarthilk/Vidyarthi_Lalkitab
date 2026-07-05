package com.vidyarthi.lalkitab.ui.widget

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.widget.FrameLayout

/**
 * Pinch to zoom (and drag when zoomed) for chart / reading screens.
 * Single-finger scroll passes through when scale is 1x.
 */
class PinchZoomFrameLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr) {

    var isZoomEnabled: Boolean = true

    private var scaleFactor = 1f
    private var translateX = 0f
    private var translateY = 0f
    private var lastTouchX = 0f
    private var lastTouchY = 0f
    private var activePointerId = INVALID_POINTER_ID

    private val minScale = 1f
    private val maxScale = 3f

    private val scaleDetector = ScaleGestureDetector(
        context,
        object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                if (!isZoomEnabled) return false
                scaleFactor *= detector.scaleFactor
                scaleFactor = scaleFactor.coerceIn(minScale, maxScale)
                if (scaleFactor <= minScale + 0.01f) {
                    resetZoom()
                } else {
                    applyTransform()
                }
                return true
            }
        }
    )

    private fun contentView() = if (childCount > 0) getChildAt(0) else null

    private fun applyTransform() {
        val child = contentView() ?: return
        child.pivotX = width / 2f
        child.pivotY = height / 2f
        child.scaleX = scaleFactor
        child.scaleY = scaleFactor
        child.translationX = translateX
        child.translationY = translateY
    }

    fun resetZoom() {
        scaleFactor = 1f
        translateX = 0f
        translateY = 0f
        applyTransform()
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        applyTransform()
    }

    override fun onInterceptTouchEvent(event: MotionEvent): Boolean {
        if (!isZoomEnabled) return false

        scaleDetector.onTouchEvent(event)

        if (event.pointerCount >= 2 || scaleDetector.isInProgress) {
            parent?.requestDisallowInterceptTouchEvent(true)
            return true
        }

        return scaleFactor > 1.05f
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isZoomEnabled) return super.onTouchEvent(event)

        scaleDetector.onTouchEvent(event)

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                lastTouchX = event.x
                lastTouchY = event.y
                activePointerId = event.getPointerId(0)
            }

            MotionEvent.ACTION_POINTER_DOWN -> {
                if (event.pointerCount >= 2) {
                    parent?.requestDisallowInterceptTouchEvent(true)
                }
            }

            MotionEvent.ACTION_MOVE -> {
                if (scaleDetector.isInProgress || event.pointerCount >= 2) {
                    return true
                }
                if (scaleFactor > 1.05f && event.pointerCount == 1) {
                    val index = event.findPointerIndex(activePointerId)
                    if (index >= 0) {
                        val x = event.getX(index)
                        val y = event.getY(index)
                        translateX += x - lastTouchX
                        translateY += y - lastTouchY
                        lastTouchX = x
                        lastTouchY = y
                        applyTransform()
                    }
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                activePointerId = INVALID_POINTER_ID
                if (scaleFactor <= minScale + 0.01f) {
                    resetZoom()
                }
            }

            MotionEvent.ACTION_POINTER_UP -> {
                val pointerIndex = event.actionIndex
                val pointerId = event.getPointerId(pointerIndex)
                if (pointerId == activePointerId) {
                    val newIndex = if (pointerIndex == 0) 1 else 0
                    if (event.pointerCount > 1) {
                        lastTouchX = event.getX(newIndex)
                        lastTouchY = event.getY(newIndex)
                        activePointerId = event.getPointerId(newIndex)
                    }
                }
            }
        }

        return scaleDetector.isInProgress || scaleFactor > 1.05f || event.pointerCount >= 2
    }

    companion object {
        private const val INVALID_POINTER_ID = -1
    }
}
