@file:Suppress("PackageDirectoryMismatch")

package androidx.recyclerview.widget

import android.view.View
import androidx.recyclerview.widget.RecyclerView.*
import com.xiaocydx.spananimation.SpanAnimationDrawable
import com.xiaocydx.spananimation.spanAnimationDrawable

fun GridLayoutManager.getSpanTransition() = SpanTransition(this)

class SpanTransition internal constructor(private val layoutManager: GridLayoutManager) {
    private val drawable: SpanAnimationDrawable?
        get() = layoutManager.mRecyclerView?.spanAnimationDrawable

    val isRunning: Boolean
        get() = drawable?.isRunning ?: false

    fun start(spanCount: Int, capturedView: (ViewHolder.() -> View) = defaultCapturedView) {
        ensureSupportSpanAnimation()
        SpanAnimationStarter(spanCount, layoutManager, capturedView).start()
    }

    private fun ensureSupportSpanAnimation() {
        require(!layoutManager.reverseLayout) { "暂时不支持反转布局" }
        require(layoutManager.orientation == VERTICAL) { "暂时不支持水平方向布局" }
    }

    private companion object {
        val defaultCapturedView: ViewHolder.() -> View = { itemView }
    }
}

@Suppress("UNCHECKED_CAST")
inline fun <VH : ViewHolder> SpanTransition.start(
    spanCount: Int,
    adapter: Adapter<out VH>,
    crossinline capturedView: (VH.() -> View)
) = start(spanCount) {
    if (bindingAdapter === adapter) capturedView(this as VH) else itemView
}