package com.xiaocydx.spananimation

import android.view.MotionEvent
import android.view.ScaleGestureDetector
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

/**
 * @author xcc
 * @date 2022/12/18
 */
internal class SpanScaleGestureHandler(
    private val controller: SpanAnimationController
) : RecyclerView.OnItemTouchListener {
    private val rv = controller.rv
    private val drawable = rv.spanAnimationDrawable
    private val listener = OnScaleGestureListenerImpl()
    private val detector = ScaleGestureDetector(rv.context, listener)
    var isEnabled = false

    override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
        if (!isEnabled || e.pointerCount == 1 || drawable.isRunning) return false
        return detector.onTouchEvent(e)
    }

    override fun onTouchEvent(rv: RecyclerView, e: MotionEvent) {
        detector.onTouchEvent(e)
        if (e.action == MotionEvent.ACTION_CANCEL
                || e.action == MotionEvent.ACTION_UP) {
            listener.reset()
            drawable.complete()
        }
    }

    override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) = Unit

    private inner class OnScaleGestureListenerImpl : ScaleGestureDetector.OnScaleGestureListener {
        private var scale = MIN_SCALE
        private var captured = false
        private var minToMax = false

        fun reset() {
            scale = MIN_SCALE
            captured = false
            minToMax = false
        }

        override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
            return !drawable.isRunning
        }

        override fun onScale(detector: ScaleGestureDetector): Boolean {
            val lm = rv.layoutManager as? GridLayoutManager
            if (lm != null && !captured && detector.scaleFactor != 1f) {
                val sign = if (detector.scaleFactor > 1f) -1 else 1
                val spanCount = controller.getSpanCount(lm.spanCount, sign)
                if (spanCount != lm.spanCount && spanCount >= 1) {
                    minToMax = detector.scaleFactor > 1f
                    scale = if (minToMax) MIN_SCALE else MAX_SCALE
                    controller.capture(spanCount)
                }
                captured = true
            }
            if (drawable.isInitialized) {
                scale *= detector.scaleFactor
                scale = scale.coerceAtLeast(MIN_SCALE).coerceAtMost(MAX_SCALE)
                val progress = if (minToMax) {
                    (scale - MIN_SCALE) / (MAX_SCALE - MIN_SCALE)
                } else {
                    (MAX_SCALE - scale) / (MAX_SCALE - MIN_SCALE)
                }
                drawable.setProgress(progress)
            }
            return true
        }

        override fun onScaleEnd(detector: ScaleGestureDetector) = Unit
    }

    private companion object {
        const val MIN_SCALE = 1f
        const val MAX_SCALE = 2f
    }
}