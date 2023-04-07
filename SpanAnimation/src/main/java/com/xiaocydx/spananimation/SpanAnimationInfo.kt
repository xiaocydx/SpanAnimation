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

import androidx.recyclerview.widget.RecyclerView.NO_POSITION

/**
 * [SpanAnimationDrawable]的动画信息
 *
 * @author xcc
 * @date 2022/12/16
 */
internal class SpanAnimationInfo(private val source: SpanAnimationValues) {
    /**
     * [values]中最小layoutPosition
     */
    var minLayoutPosition = Int.MAX_VALUE
        private set

    /**
     * [values]中最大layoutPosition
     */
    var maxLayoutPosition = Int.MIN_VALUE
        private set

    /**
     * [SpanAnimationDrawable]的动画值集合
     *
     * 集合元素顺序从[minLayoutPosition]到[maxLayoutPosition]，
     * 确保绘制顺序从[minLayoutPosition]到[maxLayoutPosition]。
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

    fun clear() {
        source.clear()
    }
}