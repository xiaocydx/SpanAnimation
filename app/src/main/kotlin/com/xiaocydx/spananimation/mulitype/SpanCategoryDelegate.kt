package com.xiaocydx.spananimation.mulitype

import android.util.TypedValue
import android.view.Gravity
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.xiaocydx.cxrv.multitype.ViewTypeDelegate
import com.xiaocydx.sample.dp
import com.xiaocydx.sample.matchParent
import com.xiaocydx.sample.withLayoutParams

/**
 * @author xcc
 * @date 2022/12/17
 */
class SpanCategoryDelegate : ViewTypeDelegate<SpanItem.Category, ViewHolder>() {

    override fun areItemsTheSame(
        oldItem: SpanItem.Category,
        newItem: SpanItem.Category
    ): Boolean = oldItem.id == newItem.id

    override fun getSpanSize(position: Int, spanCount: Int): Int = spanCount

    override fun onCreateViewHolder(parent: ViewGroup): ViewHolder {
        val view = AppCompatTextView(parent.context).apply {
            gravity = Gravity.CENTER_VERTICAL
            setTextSize(TypedValue.COMPLEX_UNIT_PX, 16.dp.toFloat())
            updatePadding(left = 16.dp)
            withLayoutParams(matchParent, 40.dp)
        }
        return object : ViewHolder(view) {}
    }

    override fun onBindViewHolder(holder: ViewHolder, item: SpanItem.Category) {
        (holder.itemView as TextView).text = item.text
    }
}