@file:Suppress("PackageDirectoryMismatch")

package androidx.recyclerview.widget

import android.graphics.Bitmap
import android.view.View
import androidx.core.view.doOnNextLayout
import androidx.core.view.doOnPreDraw
import androidx.core.view.drawToBitmap
import androidx.core.view.forEach
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.xiaocydx.spananimation.SpanAnimationInfo
import com.xiaocydx.spananimation.SpanAnimationValue
import com.xiaocydx.spananimation.SpanAnimationValues
import com.xiaocydx.spananimation.spanAnimationDrawable

/**
 * @author xcc
 * @date 2022/12/16
 */
internal class SpanAnimationStarter(
    private val spanCount: Int,
    private val layoutManager: GridLayoutManager,
    private val capturedView: ViewHolder.() -> View
) {

    fun start() {
        if (spanCount < 1) return
        val rv = layoutManager.mRecyclerView
        if (rv == null || rv.childCount == 0) {
            requestSpanCount(spanCount)
            return
        }
        // 1. 捕获设置spanCount前的StartValues
        // 2. 设置spanCount，申请下一帧重绘
        // 3. 在下一帧捕获设置spanCount后的EndValues
        // 4. 匹配StartValues和EndValues，补齐缺少的值
        // 5. 设置StartValues和EndValues，运行过渡动画

        val startSpanCount = layoutManager.spanCount
        val startValues = rv.captureStartValues()

        requestSpanCount(spanCount)

        // 下一帧RecyclerView布局流程，会调用onBindViewHolder()，
        // 绑定ViewHolder可能通过图片加载框架（例如Glide）加载图片，
        // 图片尺寸未确定，一般通过doOnPreDraw()获取尺寸（例如Glide），
        // doOnPreDraw()获取到尺寸后，若有图片缓存，则使用图片缓存，
        // doOnNextLayout() -> doOnPreDraw()的调用顺序，
        // 是在获取尺寸的doOnPreDraw()之后捕获EndValues，
        // 确保当有图片缓存时，能捕获到有内容的EndValues。
        rv.doOnNextLayout {
            rv.doOnPreDraw {
                val endSpanCount = layoutManager.spanCount
                val endValues = rv.captureEndValues(startValues)

                if (!matchAnimationValues(startSpanCount, endSpanCount, startValues, endValues)) {
                    startValues.release()
                    endValues.release()
                    return@doOnPreDraw
                }

                val drawable = rv.spanAnimationDrawable
                drawable.setBounds(0, 0, rv.width, rv.height)
                drawable.startAnimation(SpanAnimationInfo(startValues), SpanAnimationInfo(endValues))
            }
        }
    }

    private fun requestSpanCount(spanCount: Int) {
        layoutManager.spanCount = spanCount
        layoutManager.mRecyclerView?.invalidateItemDecorations()
    }

    private fun RecyclerView.captureStartValues(): SpanAnimationValues {
        val values = SpanAnimationValues(initialCapacity = childCount)
        forEach { child ->
            val holder = getChildViewHolder(child) ?: return@forEach
            val capturedView = capturedView(holder)
            ensureCapturedViewValid(child, capturedView)
            val layoutPosition = holder.layoutPosition
            val bitmap = capturedView.drawToBitmap()
            values[layoutPosition] = createAnimationValue(child, bitmap, layoutPosition)
        }
        return values
    }

    private fun RecyclerView.captureEndValues(startValues: SpanAnimationValues): SpanAnimationValues {
        val values = SpanAnimationValues(initialCapacity = childCount)
        forEach { child ->
            val holder = getChildViewHolder(child) ?: return@forEach
            val capturedView = capturedView(holder)
            ensureCapturedViewValid(child, capturedView)
            val layoutPosition = holder.layoutPosition
            val bitmap = startValues[layoutPosition]?.bitmap ?: capturedView.drawToBitmap()
            values[layoutPosition] = createAnimationValue(child, bitmap, layoutPosition)
        }
        return values
    }

    private fun ensureCapturedViewValid(child: View, capturedView: View) {
        if (!DEBUG || child === capturedView) return
        require(child.left == capturedView.left
                && child.top == capturedView.top
                && child.right == capturedView.right
                && child.bottom == capturedView.bottom) {
            "capturedView的left、top、right、bottom需要跟itemView的一致"
        }
    }

    private fun createAnimationValue(child: View, bitmap: Bitmap?, layoutPosition: Int): SpanAnimationValue {
        val params = child.layoutParams as GridLayoutManager.LayoutParams
        val spanGroupIndex = layoutManager.spanSizeLookup
            .getSpanGroupIndex(layoutPosition, layoutManager.spanCount)
        val viewType = params.mViewHolder.itemViewType
        return SpanAnimationValue(
            left = child.left, top = child.top, right = child.right, bottom = child.bottom,
            spanSize = params.spanSize, spanIndex = params.spanIndex, spanGroupIndex = spanGroupIndex,
            child = child, bitmap = bitmap, viewType = viewType, layoutPosition = layoutPosition,
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

    private companion object {
        const val DEBUG = false
    }
}