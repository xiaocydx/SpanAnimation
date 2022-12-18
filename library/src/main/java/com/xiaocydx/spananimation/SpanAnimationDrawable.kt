package com.xiaocydx.spananimation

import android.animation.ValueAnimator
import android.graphics.*
import android.graphics.drawable.Drawable
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
    private var progress = 0f
    private val dstRectF = RectF()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val animator = createAnimator()
    val isRunning: Boolean
        get() = animator.isRunning

    fun startAnimation(startInfo: SpanAnimationInfo, endInto: SpanAnimationInfo) {
        animator.end()
        this.startInfo = startInfo
        this.endInto = endInto
        animator.start()
    }

    override fun draw(canvas: Canvas) {
        val startValues = startInfo?.values ?: return
        val endValues = endInto?.values ?: return

        val boundsWidth = bounds.width()
        val boundsHeight = bounds.height()
        for (index in startValues.indices) {
            val start = startValues[index]
            val end = endValues[index]
            val bitmap = (start.bitmap ?: end.bitmap) ?: continue

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
        duration = 1000
        addUpdateListener {
            progress = it.animatedValue as Float
            invalidateSelf()
        }
        addListener(
            onStart = { setChildVisible(false) },
            onCancel = { setChildVisible(true).release() },
            onEnd = { setChildVisible(true).release() }
        )
    }

    private fun setChildVisible(isVisible: Boolean) = apply {
        val endValues = endInto?.values ?: return@apply
        for (index in endValues.indices) {
            endValues[index].child?.isVisible = isVisible
        }
    }

    private fun release() {
        startInfo?.release()
        endInto?.release()
        startInfo = null
        endInto = null
        progress = 0f
        dstRectF.setEmpty()
    }

    override fun setAlpha(alpha: Int) = Unit

    override fun setColorFilter(colorFilter: ColorFilter?) = Unit

    @Suppress("OVERRIDE_DEPRECATION")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
}