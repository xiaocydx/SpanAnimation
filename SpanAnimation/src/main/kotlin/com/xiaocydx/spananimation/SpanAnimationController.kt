/*
 * Copyright 2022 xiaocydx
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.xiaocydx.spananimation

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.view.animation.Interpolator
import android.widget.ImageView
import androidx.core.view.drawToBitmap
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder

/**
 * Span动画控制器，帮助设置属性和开始动画
 *
 * **注意**：一个RecyclerView应当只创建一个[SpanAnimationController]
 *
 * @author xcc
 * @date 2022/12/18
 */
class SpanAnimationController(rv: RecyclerView) {
    private var rv: RecyclerView? = rv
    private var spanCounts = emptyList<Int>()
    private var handler: SpanScaleGestureHandler? = null
    private var provider: (ViewHolder.() -> CapturedResult) = defaultProvider
    private val drawable = SpanAnimationDrawable()
    private val layoutManager: GridLayoutManager?
        get() = rv?.layoutManager as? GridLayoutManager

    init {
        rv.resetSpanAnimationDrawable(drawable)
    }

    /**
     * 动画是否运行中
     */
    val isRunning: Boolean
        get() = drawable.isRunning

    /**
     * 设置捕获结果提供者，默认使用[defaultProvider]
     */
    fun setCapturedProvider(provider: ViewHolder.() -> CapturedResult) {
        this.provider = provider
    }

    /**
     * 设置绘制中Bitmap提供者，用于替换已有的捕获结果
     */
    fun setDrawingProvider(provider: ViewHolder.() -> Bitmap?) {
        val rv = rv ?: return
        drawable.drawingProvider = { provider(rv.getChildViewHolder(it)) }
    }

    /**
     * 设置过渡到指定spanCount的动画时长，单位：ms
     */
    fun setAnimationDuration(duration: Long) {
        drawable.animationDuration = duration
    }

    /**
     * 设置过渡到指定spanCount的动画插值器
     */
    fun setAnimationInterpolator(interpolator: Interpolator) {
        drawable.animationInterpolator = interpolator
    }

    /**
     * 设置缩放手势结束后的完成动画插值器
     */
    fun setScaleCompleteInterpolator(interpolator: Interpolator) {
        drawable.completeInterpolator = interpolator
    }

    /**
     * 设置是否启用缩放手势
     */
    fun setScaleGestureEnabled(isEnabled: Boolean) {
        val rv = rv ?: return
        if (isEnabled && handler == null) {
            handler = SpanScaleGestureHandler(rv, drawable, ::capture, ::getSpanCount)
            rv.addOnItemTouchListener(handler!!)
        }
        handler?.isEnabled = isEnabled
    }

    /**
     * 设置参与过渡的spanCount组，[spanCounts]应当包含初始化spanCount
     */
    fun setSpanCounts(vararg spanCounts: Int) {
        spanCounts.sort()
        this.spanCounts = spanCounts.filter { it > 0 }.distinct()
    }

    /**
     * 从当前spanCount过渡到[spanCount]
     */
    fun go(spanCount: Int) {
        val rv = rv
        val lm = layoutManager
        if (rv == null || lm == null) return
        if (lm.spanCount == spanCount || spanCount < 1) return
        SpanAnimationRunner(spanCount, rv, lm, drawable, provider).start()
    }

    fun increase() {
        layoutManager?.let { go(getSpanCount(it.spanCount, sign = 1)) }
    }

    fun decrease() {
        layoutManager?.let { go(getSpanCount(it.spanCount, sign = -1)) }
    }

    fun dispose() {
        val rv = rv ?: return
        handler?.let(rv::removeOnItemTouchListener)
        drawable.end()
        rv.clearSpanAnimationDrawable(drawable)
        handler = null
        this.rv = null
    }

    private fun getSpanCount(spanCount: Int, sign: Int): Int {
        require(sign == 1 || sign == -1)
        var index = spanCounts.indexOf(spanCount)
        if (index == -1) return (spanCount + sign)
        index += sign
        return spanCounts.getOrNull(index) ?: spanCount
    }

    private fun capture(spanCount: Int) {
        val rv = rv
        val lm = layoutManager
        if (rv == null || lm == null) return
        if (lm.spanCount == spanCount || spanCount < 1) return
        SpanAnimationRunner(spanCount, rv, lm, drawable, provider).capture()
    }

    private companion object {
        val defaultProvider: ViewHolder.() -> CapturedResult = {
            CapturedResult(this.itemView.drawToBitmap(), canRecycle = true)
        }
    }
}

data class CapturedResult(val bitmap: Bitmap, val canRecycle: Boolean)

/**
 * 设置ImageView提供者
 *
 * 该函数用于图片加载场景，做了以下处理：
 * 1. 复用图片加载框架提供的Bitmap作为捕获结果。
 * 2. 图片加载完成的Bitmap替换无内容的捕获结果。
 */
inline fun SpanAnimationController.setImageViewProvider(crossinline provider: ViewHolder.() -> ImageView?) {
    setCapturedProvider {
        val imageView = provider(this)
        var bitmap = (imageView?.drawable as? BitmapDrawable)?.bitmap
        var canRecycle = bitmap == null
        if (bitmap == null) {
            bitmap = (imageView ?: itemView).drawToBitmap()
            canRecycle = true
        }
        CapturedResult(bitmap, canRecycle)
    }
    setDrawingProvider { (provider(this)?.drawable as? BitmapDrawable)?.bitmap }
}