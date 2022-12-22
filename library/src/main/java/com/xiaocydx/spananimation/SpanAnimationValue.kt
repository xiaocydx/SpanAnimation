package com.xiaocydx.spananimation

import android.graphics.Bitmap
import android.view.View

/**
 * [SpanAnimationDrawable]的动画值
 *
 * @author xcc
 * @date 2022/12/16
 */
internal class SpanAnimationValue(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int,
    val spanSize: Int,
    val spanIndex: Int,
    val spanGroupIndex: Int,
    val child: View?,
    val bitmap: Bitmap?,
    val viewType: Int,
    val canRecycle: Boolean,
    val layoutPosition: Int
) {
    val width = right - left
    val height = bottom - top

    /**
     * 是否为计算值
     *
     * 若[child]和[bitmap]为null，则表示[left]、[top]、[right]、[bottom]是计算值，
     * 而不是捕获值，跟当前值对称的[SpanAnimationValue]，其`child`和`bitmap`不为`null`。
     */
    val isCalculated = child == null && bitmap == null

    fun recycle() {
        if (canRecycle) bitmap?.recycle()
    }
}