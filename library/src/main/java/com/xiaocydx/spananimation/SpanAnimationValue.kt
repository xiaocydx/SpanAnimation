package com.xiaocydx.spananimation

import android.graphics.Bitmap
import android.view.View

/**
 * [SpanAnimationDrawable]的动画值
 *
 * @author xcc
 * @date 2022/12/16
 */
internal class SpanAnimationValue constructor(
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
    val layoutPosition: Int
) {
    val width = right - left
    val height = bottom - top

    /**
     * 是否为计算出的值
     *
     * 若[child]和[bitmap]为null，则表示[left]、[top]、[right]、[bottom]是计算出的值，
     * 而不是捕获出的值，跟当前值对称的[SpanAnimationValue]，其`child`和`bitmap`不为`null`。
     */
    val isCalculatedValue: Boolean
        get() = child == null && bitmap == null
}