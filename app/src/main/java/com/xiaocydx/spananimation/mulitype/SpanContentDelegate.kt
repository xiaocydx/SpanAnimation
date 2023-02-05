package com.xiaocydx.spananimation.mulitype

import android.content.Context
import android.graphics.Color
import android.util.TypedValue
import android.view.Gravity
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.bumptech.glide.RequestManager
import com.xiaocydx.cxrv.multitype.ViewTypeDelegate
import com.xiaocydx.sample.matchParent
import com.xiaocydx.spananimation.R
import com.xiaocydx.spananimation.custom.CustomLayout

/**
 * @author xcc
 * @date 2022/12/17
 */
class SpanContentDelegate(
    private val requestManager: RequestManager
) : ViewTypeDelegate<SpanItem.Content, SpanContentHolder>() {

    override fun areItemsTheSame(
        oldItem: SpanItem.Content,
        newItem: SpanItem.Content
    ): Boolean = oldItem.id == newItem.id

    override fun getSpanSize(position: Int, spanCount: Int): Int = 1

    override fun onCreateViewHolder(parent: ViewGroup): SpanContentHolder {
        return SpanContentHolder(SpanContentItemView(parent.context))
    }

    override fun onBindViewHolder(
        holder: SpanContentHolder,
        item: SpanItem.Content
    ): Unit = with(holder.view) {
        imageView.transitionName = item.id
        requestManager.load(item.url)
            .placeholder(R.color.placeholder_color)
            .into(imageView)
    }
}

class SpanContentHolder(val view: SpanContentItemView) : ViewHolder(view)

class SpanContentItemView(context: Context) : CustomLayout(context) {
    val imageView = AppCompatImageView(context).apply {
        scaleType = ImageView.ScaleType.CENTER_CROP
        addView(this, matchParent, 0.dp)
    }

    val textView = AppCompatTextView(context).apply {
        gravity = Gravity.CENTER
        setTextColor(Color.WHITE)
        setTextSize(TypedValue.COMPLEX_UNIT_PX, 16.dp.toFloat())
        addView(this, matchParent, wrapContent)
    }

    override fun onMeasureChildren(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val size = measuredWidth
        val measureSpec = size.toExactlyMeasureSpec()
        imageView.measureWith(measureSpec, measureSpec)
        textView.measureWith(measureSpec, measureSpec)
        setMeasuredDimension(size, size)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        imageView.layout(x = 0, y = 0)
        textView.layoutToCenter(imageView)
    }
}