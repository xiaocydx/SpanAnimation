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
import android.util.SparseArray
import androidx.core.util.forEach
import androidx.core.util.isNotEmpty

/**
 * [SpanAnimationDrawable]的动画值集合
 *
 * @author xcc
 * @date 2022/12/16
 */
internal class SpanAnimationValues(initialCapacity: Int) {
    private var values: SparseArray<SpanAnimationValue>? = SparseArray(initialCapacity)

    val size: Int
        get() = values?.size() ?: 0

    operator fun set(layoutPosition: Int, animationValue: SpanAnimationValue) {
        values?.put(layoutPosition, animationValue)
    }

    operator fun get(layoutPosition: Int): SpanAnimationValue? = values?.get(layoutPosition)

    private inline fun forEach(action: (layoutPosition: Int, animationValue: SpanAnimationValue) -> Unit) {
        values?.forEach(action)
    }

    fun clear() {
        forEach { _, animationValue -> animationValue.recycle() }
        values = null
    }

    fun matchAnimationValues(startSpanCount: Int, endSpanCount: Int, endValues: SpanAnimationValues): Boolean {
        val state = SpanMatchState(startSpanCount, endSpanCount, endValues, SparseArray(2))
        matchAnimationValuesStep1(state)
        matchAnimationValuesStep2(state)
        matchAnimationValuesStep3(state)
        return state.step == Step.COMPLETED
    }

    /**
     * 匹配[SpanAnimationValues]的第一步：
     * 1. 计算遍历的起始和结束layoutPosition
     * 2. 计算endValues的最大和最小layoutPosition
     * 3. 计算startValues的childDimensions
     * 4. 计算decoratedDimensions，尝试startValues和endValues两轮计算
     */
    private fun matchAnimationValuesStep1(state: SpanMatchState): Unit = with(state) {
        if (step != Step.STEP1) return
        val startValues = this@SpanAnimationValues
        startValues.calculateMinMaxLayoutPositions().also {
            firstLayoutPosition = it[0] - 1
            lastLayoutPosition = it[1] + 1
        }
        endValues.calculateMinMaxLayoutPositions().also {
            minLayoutPosition = it[0]
            maxLayoutPosition = it[1]
        }

        startValues.calculateChildDimensions(state)
        startValues.calculateDecoratedDimensions(state)
        endValues.calculateDecoratedDimensions(state)

        if (checkMinMaxLayoutPositions()
                && checkChildDimensions()
                && checkDecoratedDimensions()) {
            step = Step.STEP2
        }
    }

    /**
     * 匹配[SpanAnimationValues]的第二步：
     * 1. 匹配startValues的前部分[SpanAnimationValue]
     * 2. 匹配startValues的后部分[SpanAnimationValue]
     */
    @Suppress("KotlinConstantConditions")
    private fun matchAnimationValuesStep2(state: SpanMatchState): Unit = with(state) {
        if (step != Step.STEP2) return
        var left: Int
        var top: Int
        var right: Int
        var bottom: Int
        var spanIndex: Int
        var spanGroupIndex: Int
        val bitmap: Bitmap? = null
        val canRecycle = false

        while (firstLayoutPosition >= minLayoutPosition) {
            val layoutPosition = firstLayoutPosition
            firstLayoutPosition -= 1
            if (get(layoutPosition) != null) continue
            val endValue = endValues[layoutPosition]
            val nextValue = get(layoutPosition + 1)
            requireNotNull(endValue) { "firstLayoutPosition计算错误" }
            requireNotNull(nextValue) { "firstLayoutPosition计算错误" }

            val viewType = endValue.viewType
            val childWidth = getChildWidth(viewType, endValue.width)
            val childHeight = getChildHeight(viewType, endValue.height)

            val spanSize = endValue.spanSize
            if (endValue.spanSize <= nextValue.spanIndex) {
                spanIndex = nextValue.spanIndex - spanSize
                spanGroupIndex = nextValue.spanGroupIndex
                left = nextValue.left - nextValue.width - decoratedWidth
                top = nextValue.top
                right = left + childWidth
                bottom = top + childHeight
            } else {
                spanIndex = startSpanCount - spanSize
                spanGroupIndex = nextValue.spanGroupIndex - 1
                left = spanIndex * (decoratedWidth + childWidth)
                top = nextValue.top - nextValue.height - decoratedHeight
                right = left + childWidth
                bottom = top + childHeight
            }

            set(layoutPosition, SpanAnimationValue(
                left, top, right, bottom,
                spanSize, spanIndex, spanGroupIndex,
                bitmap, viewType, canRecycle, layoutPosition))
        }

        while (lastLayoutPosition <= maxLayoutPosition) {
            val layoutPosition = lastLayoutPosition
            lastLayoutPosition += 1
            if (get(layoutPosition) != null) continue
            val endValue = endValues[layoutPosition]
            val prevValue = get(layoutPosition - 1)
            requireNotNull(endValue) { "lastLayoutPosition计算错误" }
            requireNotNull(prevValue) { "lastLayoutPosition计算错误" }

            val viewType = endValue.viewType
            val childWidth = getChildWidth(viewType, endValue.width)
            val childHeight = getChildHeight(viewType, endValue.height)

            val spanSize = endValue.spanSize
            val prevCount = prevValue.spanIndex + prevValue.spanSize
            if ((prevCount + endValue.spanSize) <= startSpanCount) {
                spanIndex = prevValue.spanIndex + spanSize
                spanGroupIndex = prevValue.spanGroupIndex
                left = prevValue.left + prevValue.width + decoratedWidth
                top = prevValue.top
                right = left + childWidth
                bottom = top + childHeight
            } else {
                spanIndex = 0
                spanGroupIndex = prevValue.spanGroupIndex + 1
                left = 0
                top = prevValue.top + prevValue.height + decoratedHeight
                right = left + childWidth
                bottom = top + childHeight
            }

            set(layoutPosition, SpanAnimationValue(
                left, top, right, bottom,
                spanSize, spanIndex, spanGroupIndex,
                bitmap, viewType, canRecycle, layoutPosition))
        }

        step = Step.STEP3
    }

    /**
     * 匹配[SpanAnimationValues]的第三步：
     * 若startValues.size大于endValues.size，则反转参数重新匹配，让endValues完整
     */
    private fun matchAnimationValuesStep3(state: SpanMatchState): Unit = with(state) {
        if (step != Step.STEP3) return
        val startValues = this@SpanAnimationValues
        when {
            startValues.size < endValues.size -> return
            startValues.size > endValues.size -> {
                if (endValues.matchAnimationValues(endSpanCount, startSpanCount, startValues)) {
                    step = Step.COMPLETED
                }
            }
            else -> step = Step.COMPLETED
        }
    }

    fun calculateMinMaxLayoutPositions(): IntArray {
        var minLayoutPosition = Int.MAX_VALUE
        var maxLayoutPosition = Int.MIN_VALUE
        forEach { layoutPosition, _ ->
            minLayoutPosition = minLayoutPosition.coerceAtMost(layoutPosition)
            maxLayoutPosition = maxLayoutPosition.coerceAtLeast(layoutPosition)
        }
        return intArrayOf(minLayoutPosition, maxLayoutPosition)
    }

    private fun calculateChildDimensions(state: SpanMatchState) = with(state) {
        if (!checkMinMaxLayoutPositions() || checkChildDimensions()) return
        for (layoutPosition in minLayoutPosition..maxLayoutPosition) {
            val startValue = get(layoutPosition)
            if (startValue == null || startValue.isCalculated) continue
            putChildDimensions(startValue.viewType, startValue.width, startValue.height)
        }
    }

    private fun calculateDecoratedDimensions(state: SpanMatchState) = with(state) {
        if (!checkMinMaxLayoutPositions() || checkDecoratedDimensions()) return
        var prevValue: SpanAnimationValue? = null
        for (layoutPosition in minLayoutPosition..maxLayoutPosition) {
            val startValue = get(layoutPosition)
            if (startValue == null || startValue.isCalculated) continue
            if (decoratedWidth == NO_VALUE && prevValue != null
                    && prevValue.spanSize == 1 && startValue.spanSize == 1
                    && prevValue.spanGroupIndex == startValue.spanGroupIndex) {
                decoratedWidth = startValue.left - prevValue.right
            }
            if (decoratedHeight == NO_VALUE && prevValue != null
                    && (startValue.spanGroupIndex - prevValue.spanGroupIndex) == 1) {
                decoratedHeight = startValue.top - prevValue.bottom
            }
            if (decoratedWidth != NO_VALUE && decoratedHeight != NO_VALUE) break
            prevValue = startValue
        }
    }

    private class SpanMatchState(
        val startSpanCount: Int,
        val endSpanCount: Int,
        val endValues: SpanAnimationValues,
        val childDimensions: SparseArray<IntArray?>,
        var minLayoutPosition: Int = NO_VALUE,
        var maxLayoutPosition: Int = NO_VALUE,
        var firstLayoutPosition: Int = NO_VALUE,
        var lastLayoutPosition: Int = NO_VALUE,
        var decoratedWidth: Int = NO_VALUE,
        var decoratedHeight: Int = NO_VALUE,
        var step: Step = Step.STEP1
    ) {
        fun checkMinMaxLayoutPositions(): Boolean {
            return minLayoutPosition != NO_VALUE
                    && maxLayoutPosition != NO_VALUE
                    && minLayoutPosition <= maxLayoutPosition
        }

        fun checkChildDimensions(): Boolean {
            return childDimensions.isNotEmpty()
        }

        fun checkDecoratedDimensions(): Boolean {
            return decoratedWidth != NO_VALUE && decoratedHeight != NO_VALUE
        }

        fun putChildDimensions(viewType: Int, childWidth: Int, childHeight: Int) {
            if (childDimensions[viewType] != null) return
            childDimensions[viewType] = intArrayOf(childWidth, childHeight)
        }

        fun getChildWidth(viewType: Int, defaultValue: Int): Int {
            return childDimensions[viewType]?.get(0) ?: defaultValue
        }

        fun getChildHeight(viewType: Int, defaultValue: Int): Int {
            return childDimensions[viewType]?.get(1) ?: defaultValue
        }
    }

    private enum class Step {
        STEP1, STEP2, STEP3, COMPLETED
    }

    private companion object {
        const val NO_VALUE = -1
    }
}