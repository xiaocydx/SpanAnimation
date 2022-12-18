package com.xiaocydx.spananimation.mulitype

import android.content.Context
import android.graphics.Color
import android.util.TypedValue
import android.view.Gravity
import android.view.ViewGroup
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
) : ViewTypeDelegate<AnimationItem.Content, SpanContentHolder>() {
    override fun areItemsTheSame(
        oldItem: AnimationItem.Content,
        newItem: AnimationItem.Content
    ): Boolean = oldItem.id == newItem.id

    override fun getSpanSize(position: Int, spanCount: Int): Int = 1

    override fun onCreateViewHolder(parent: ViewGroup): SpanContentHolder {
        return SpanContentHolder(SpanContentLayout(parent.context))
    }

    override fun onBindViewHolder(
        holder: SpanContentHolder,
        item: AnimationItem.Content
    ): Unit = with(holder.view) {
        // requestManager.load(item.url).centerCrop()
        //     .placeholder(R.color.placeholder_color)
        //     .into(imageView)
        imageView.setBackgroundColor(
            if (item.num % 2 != 0) 0xFF72AAA2.toInt() else 0xFFAA766B.toInt()
        )
        textView.text = holder.layoutPosition.toString()
    }
}

class SpanContentHolder(val view: SpanContentLayout) : ViewHolder(view)

class SpanContentLayout(context: Context) : CustomLayout(context) {
    val imageView = AppCompatImageView(context).apply {
        addView(this, matchParent, wrapContent)
    }

    val textView = AppCompatTextView(context).apply {
        gravity = Gravity.CENTER
        setTextColor(Color.WHITE)
        setTextSize(TypedValue.COMPLEX_UNIT_PX, 16.dp.toFloat())
        addView(this, matchParent, wrapContent)
    }

    override fun onMeasureChildren(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val size = measuredWidth
        imageView.measureWith(size.toExactlyMeasureSpec(), size.toExactlyMeasureSpec())
        textView.measureWith(size.toExactlyMeasureSpec(), size.toExactlyMeasureSpec())
        setMeasuredDimension(measuredWidth, measuredWidth)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        imageView.layout(x = 0, y = 0)
        textView.layoutToCenter(imageView)
    }
}