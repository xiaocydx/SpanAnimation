package com.xiaocydx.spananimation

import androidx.recyclerview.widget.RecyclerView.NO_POSITION

/**
 * [SpanAnimationDrawable]的动画信息
 *
 * @author xcc
 * @date 2022/12/16
 */
internal class SpanAnimationInfo(private val source: SpanAnimationValues) {
    /**
     * [values]中最小的layoutPosition
     */
    var minLayoutPosition = Int.MAX_VALUE
        private set

    /**
     * [values]中最大的layoutPosition
     */
    var maxLayoutPosition = Int.MIN_VALUE
        private set

    /**
     * [SpanAnimationDrawable]的动画值集合
     *
     * 集合元素顺序是从[minLayoutPosition]到[maxLayoutPosition]，
     * 确保绘制顺序是从[minLayoutPosition]到[maxLayoutPosition]。
     */
    val values: List<SpanAnimationValue>

    init {
        source.calculateMinMaxLayoutPositions().also {
            minLayoutPosition = it[0]
            maxLayoutPosition = it[1]
        }
        val size = when {
            maxLayoutPosition < minLayoutPosition -> 0
            else -> maxLayoutPosition - minLayoutPosition + 1
        }
        values = if (size != 0) {
            ArrayList<SpanAnimationValue>(size).apply {
                for (layoutPosition in minLayoutPosition..maxLayoutPosition) {
                    source[layoutPosition]?.let(::add)
                }
            }
        } else {
            minLayoutPosition = NO_POSITION
            maxLayoutPosition = NO_POSITION
            emptyList()
        }
    }

    fun release() {
        source.release()
    }
}