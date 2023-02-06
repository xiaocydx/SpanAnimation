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
    private val dstRectF = RectF()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var startInfo: SpanAnimationInfo? = null
    private var endInto: SpanAnimationInfo? = null
    private var allowClear = true
    private var animator: ValueAnimator? = null

    var fraction = -1f
        private set
    val isRunning: Boolean
        get() = animator?.isRunning == true
    val isInitialized: Boolean
        get() = startInfo != null && endInto != null

    var animationDuration = 500L
    var animationInterpolator: Interpolator = AccelerateDecelerateInterpolator()
    var completeInterpolator: Interpolator = DecelerateInterpolator()
    var drawingProvider: ((child: View) -> Bitmap?)? = null

    fun begin(startInfo: SpanAnimationInfo, endInto: SpanAnimationInfo, start: Boolean) {
        endAnimation(allowClear = true)
        this.startInfo = startInfo
        this.endInto = endInto
        setChildVisible(isVisible = false)
        if (!start) return
        runAnimation {
            duration = animationDuration
            interpolator = animationInterpolator
            setFloatValues(0f, 1f)
        }
    }

    fun setFraction(fraction: Float) {
        endAnimation(allowClear = false)
        val safeFraction = fraction
            .coerceAtLeast(0f)
            .coerceAtMost(1f)
        when {
            this.fraction == safeFraction -> return
            safeFraction == 1f -> {
                setChildVisible(isVisible = true)
                clear()
            }
            else -> {
                this.fraction = safeFraction
                invalidateSelf()
            }
        }
    }

    fun completeToStart(action: (() -> Unit)? = null) {
        complete(toEnd = false, action)
    }

    fun completeToEnd(action: (() -> Unit)? = null) {
        complete(toEnd = true, action)
    }

    private fun complete(toEnd: Boolean, action: (() -> Unit)? = null) {
        if (!isInitialized) return
        val startFraction = fraction
        endAnimation(allowClear = false)
        runAnimation {
            interpolator = completeInterpolator
            duration = if (toEnd) {
                (animationDuration * (1f - startFraction)).toLong()
            } else {
                (animationDuration * startFraction).toLong()
            }
            if (action != null) {
                addListener(
                    onCancel = { action() },
                    onEnd = { action() }
                )
            }
            setFloatValues(startFraction, if (toEnd) 1f else 0f)
        }
    }

    override fun draw(canvas: Canvas) {
        val startValues = startInfo?.values ?: return
        val endValues = endInto?.values ?: return
        val fraction = fraction
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

            val left = start.left + (end.left - start.left) * fraction
            val top = start.top + (end.top - start.top) * fraction
            val width = start.width + (end.width - start.width) * fraction
            val height = start.height + (end.height - start.height) * fraction
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

    private inline fun runAnimation(block: ValueAnimator.() -> Unit) {
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            addUpdateListener {
                fraction = it.animatedValue as Float
                invalidateSelf()
            }
            addListener(
                onStart = { setChildVisible(false) },
                onCancel = { setChildVisible(true).clear() },
                onEnd = { setChildVisible(true).clear() }
            )
            block(this)
        }
        animator!!.start()
    }

    private fun endAnimation(allowClear: Boolean) {
        if (!isRunning) return
        this.allowClear = allowClear
        animator?.end()
        animator = null
        this.allowClear = true
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
        fraction = -1f
        dstRectF.setEmpty()
        animator = null
    }

    override fun setAlpha(alpha: Int) = Unit

    override fun setColorFilter(colorFilter: ColorFilter?) = Unit

    @Suppress("OVERRIDE_DEPRECATION")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
}