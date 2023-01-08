package com.xiaocydx.spananimation

import android.animation.ValueAnimator
import android.graphics.*
import android.graphics.drawable.Drawable
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.Interpolator
import androidx.core.animation.addListener
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView

internal val RecyclerView.spanAnimationDrawable: SpanAnimationDrawable
    get() {
        var drawable = getTag(R.id.tag_span_animation_drawable) as? SpanAnimationDrawable
        if (drawable == null) {
            drawable = SpanAnimationDrawable()
            overlay.add(drawable)
            setTag(R.id.tag_span_animation_drawable, drawable)
        }
        return drawable
    }

internal class SpanAnimationDrawable : Drawable() {
    private var startInfo: SpanAnimationInfo? = null
    private var endInto: SpanAnimationInfo? = null
    private var allowClear = true
    private var progress = -1f
    private val dstRectF = RectF()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val animator = createAnimator()

    val isRunning: Boolean
        get() = animator.isRunning
    val isInitialized: Boolean
        get() = startInfo != null && endInto != null

    var animationDuration = 500L
    var animationInterpolator: Interpolator = AccelerateDecelerateInterpolator()
    var completeInterpolator: Interpolator = DecelerateInterpolator()
    var drawingProvider: ((child: View) -> Bitmap?)? = null

    fun begin(startInfo: SpanAnimationInfo, endInto: SpanAnimationInfo, start: Boolean) {
        if (isRunning) animator.end()
        this.startInfo = startInfo
        this.endInto = endInto
        setChildVisible(isVisible = false)
        if (!start) return
        animator.apply {
            duration = animationDuration
            interpolator = animationInterpolator
            setFloatValues(0f, 1f)
            start()
        }
    }

    fun setProgress(progress: Float) {
        if (isRunning) {
            allowClear = false
            animator.end()
            allowClear = true
        }
        val safeProgress = progress
            .coerceAtLeast(0f)
            .coerceAtMost(1f)
        when {
            this.progress == safeProgress -> return
            safeProgress == 1f -> {
                setChildVisible(isVisible = true)
                clear()
            }
            else -> {
                this.progress = safeProgress
                invalidateSelf()
            }
        }
    }

    fun complete() {
        if (!isInitialized) return
        val startProgress = progress
        if (isRunning) {
            allowClear = false
            animator.end()
            allowClear = true
        }
        animator.apply {
            duration = (animationDuration * (1f - startProgress)).toLong()
            interpolator = completeInterpolator
            setFloatValues(startProgress, 1f)
            start()
        }
    }

    override fun draw(canvas: Canvas) {
        val startValues = startInfo?.values ?: return
        val endValues = endInto?.values ?: return
        val progress = progress
            .coerceAtLeast(0f)
            .coerceAtMost(1f)

        val boundsWidth = bounds.width()
        val boundsHeight = bounds.height()
        for (index in startValues.indices) {
            val start = startValues[index]
            val end = endValues[index]
            var bitmap = (start.bitmap ?: end.bitmap) ?: continue
            val child = start.child ?: end.child
            if (child != null && drawingProvider != null) {
                bitmap = drawingProvider!!.invoke(child) ?: bitmap
            }
            if (bitmap.isRecycled) continue

            val left = start.left + (end.left - start.left) * progress
            val top = start.top + (end.top - start.top) * progress
            val width = start.width + (end.width - start.width) * progress
            val height = start.height + (end.height - start.height) * progress
            val right = left + width
            val bottom = top + height
            if (left > boundsWidth || top > boundsHeight || right < 0 || bottom < 0) {
                // 超出drawable的绘制范围
                continue
            }
            dstRectF.set(left, top, right, bottom)
            canvas.drawBitmap(bitmap, null, dstRectF, paint)
        }
    }

    private fun createAnimator() = ValueAnimator.ofFloat(0f, 1f).apply {
        addUpdateListener {
            progress = it.animatedValue as Float
            invalidateSelf()
        }
        addListener(
            onStart = { setChildVisible(false) },
            onCancel = { setChildVisible(true).clear() },
            onEnd = { setChildVisible(true).clear() }
        )
    }

    private fun setChildVisible(isVisible: Boolean) = apply {
        val endValues = endInto?.values ?: return@apply
        for (index in endValues.indices) {
            val child = endValues[index].child
            if (child != null && child.isVisible != isVisible) {
                child.isVisible = isVisible
            }
        }
    }

    private fun clear() {
        if (!allowClear) return
        startInfo?.clear()
        endInto?.clear()
        startInfo = null
        endInto = null
        progress = -1f
        dstRectF.setEmpty()
    }

    override fun setAlpha(alpha: Int) = Unit

    override fun setColorFilter(colorFilter: ColorFilter?) = Unit

    @Suppress("OVERRIDE_DEPRECATION")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
}