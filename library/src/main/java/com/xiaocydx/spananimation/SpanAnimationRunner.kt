package com.xiaocydx.spananimation

import android.graphics.Bitmap
import android.view.View
import androidx.core.view.doOnNextLayout
import androidx.core.view.doOnPreDraw
import androidx.core.view.forEach
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder

/**
 * 负责调度动画整体流程
 *
 * @author xcc
 * @date 2022/12/16
 */
internal class SpanAnimationRunner(
    private val start: Boolean,
    private val spanCount: Int,
    private val rv: RecyclerView,
    private val layoutManager: GridLayoutManager,
    private val capturedResultProvider: ViewHolder.() -> CapturedResult
) {

    /**
     * 1. 捕获设置spanCount前的startValues
     * 2. 设置spanCount，申请下一帧重绘
     * 3. 在下一帧捕获设置spanCount后的endValues
     * 4. 匹配startValues和endValues，补齐缺少的值
     * 5. 设置startValues和endValues，若[start]为ture，则开始动画
     */
    fun begin() {
        if (spanCount < 1) return
        if (rv.childCount == 0) {
            requestSpanCount(spanCount)
            return
        }
        ensureSupportSpanAnimation()
        val startSpanCount = layoutManager.spanCount
        val startValues = rv.captureStartValues()

        requestSpanCount(spanCount)

        // 下一帧RecyclerView布局流程，会调用onBindViewHolder()，
        // 绑定ViewHolder可能通过图片加载框架（例如Glide）加载图片，
        // 图片尺寸未确定，一般通过doOnPreDraw()获取尺寸（例如Glide），
        // doOnPreDraw()获取到尺寸后，若有图片缓存，则使用图片缓存，
        // doOnNextLayout() -> doOnPreDraw()的调用顺序，
        // 是在获取尺寸的doOnPreDraw()之后捕获endValues，
        // 确保当有图片缓存时，能捕获到有内容的endValues。
        rv.doOnNextLayout {
            rv.doOnPreDraw {
                val endSpanCount = layoutManager.spanCount
                val endValues = rv.captureEndValues(startValues)

                if (!matchAnimationValues(startSpanCount, endSpanCount, startValues, endValues)) {
                    startValues.clear()
                    endValues.clear()
                    return@doOnPreDraw
                }

                val drawable = rv.spanAnimationDrawable
                drawable.setBounds(0, 0, rv.width, rv.height)
                drawable.begin(SpanAnimationInfo(startValues), SpanAnimationInfo(endValues), start)
            }
        }
    }

    private fun ensureSupportSpanAnimation() {
        require(!layoutManager.reverseLayout) { "暂时不支持反转布局" }
        require(layoutManager.orientation == RecyclerView.VERTICAL) { "暂时不支持水平方向布局" }
    }

    private fun requestSpanCount(spanCount: Int) {
        layoutManager.spanCount = spanCount
        rv.invalidateItemDecorations()
    }

    private fun RecyclerView.captureStartValues(): SpanAnimationValues {
        val values = SpanAnimationValues(initialCapacity = childCount)
        forEach { child ->
            val holder = getChildViewHolder(child) ?: return@forEach
            val layoutPosition = holder.layoutPosition
            val (bitmap, canRecycle) = capturedResultProvider(holder)
            values[layoutPosition] = createAnimationValue(child, bitmap, canRecycle, layoutPosition)
        }
        return values
    }

    private fun RecyclerView.captureEndValues(startValues: SpanAnimationValues): SpanAnimationValues {
        val values = SpanAnimationValues(initialCapacity = childCount)
        forEach { child ->
            val holder = getChildViewHolder(child) ?: return@forEach
            val layoutPosition = holder.layoutPosition
            val startValue = startValues[layoutPosition]
            var bitmap = startValue?.bitmap
            var canRecycle = startValue?.canRecycle ?: true
            if (bitmap == null) {
                val result = capturedResultProvider(holder)
                bitmap = result.bitmap
                canRecycle = result.canRecycle
            }
            values[layoutPosition] = createAnimationValue(child, bitmap, canRecycle, layoutPosition)
        }
        return values
    }

    private fun createAnimationValue(
        child: View,
        bitmap: Bitmap?,
        canRecycle: Boolean,
        layoutPosition: Int
    ): SpanAnimationValue {
        val params = child.layoutParams as GridLayoutManager.LayoutParams
        val spanSizeLookup = layoutManager.spanSizeLookup
        if (!spanSizeLookup.isSpanGroupIndexCacheEnabled) {
            spanSizeLookup.isSpanGroupIndexCacheEnabled = true
        }
        val spanGroupIndex = layoutManager.spanSizeLookup
            .getSpanGroupIndex(layoutPosition, layoutManager.spanCount)
        val viewType = rv.getChildViewHolder(child).itemViewType
        return SpanAnimationValue(
            child.left, child.top, child.right, child.bottom,
            params.spanSize, params.spanIndex, spanGroupIndex,
            child, bitmap, viewType, canRecycle, layoutPosition,
        )
    }

    private fun matchAnimationValues(
        startSpanCount: Int,
        endSpanCount: Int,
        startValues: SpanAnimationValues,
        endValues: SpanAnimationValues
    ): Boolean = when {
        startValues.size < endValues.size -> {
            startValues.matchAnimationValues(startSpanCount, endSpanCount, endValues)
        }
        startValues.size > endValues.size -> {
            endValues.matchAnimationValues(endSpanCount, startSpanCount, startValues)
        }
        else -> true
    }
}