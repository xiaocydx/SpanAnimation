package com.xiaocydx.spananimation

import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.getSpanTransition
import androidx.recyclerview.widget.start
import com.bumptech.glide.Glide
import com.xiaocydx.cxrv.divider.spacing
import com.xiaocydx.cxrv.list.adapter
import com.xiaocydx.cxrv.list.grid
import com.xiaocydx.cxrv.list.submitList
import com.xiaocydx.cxrv.multitype.listAdapter
import com.xiaocydx.cxrv.multitype.register
import com.xiaocydx.sample.dp
import com.xiaocydx.sample.onClick
import com.xiaocydx.spananimation.mulitype.*

/**
 * @author xcc
 * @date 2022/12/18
 */
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val rvAnimation = findViewById<RecyclerView>(R.id.rvAnimation)
        val btnIncrease = findViewById<Button>(R.id.btnIncrease)
        val btnDecrease = findViewById<Button>(R.id.btnDecrease)

        val adapter = createListAdapter()
        rvAnimation
            .grid(spanCount = 3).adapter(adapter)
            .spacing(width = 5.dp, height = 5.dp)

        val lm = rvAnimation.layoutManager as GridLayoutManager
        val transition = lm.getSpanTransition()
        val capturedView: RecyclerView.ViewHolder.() -> View = {
            (this as? SpanContentHolder)?.view?.imageView ?: itemView
        }
        btnIncrease.onClick {
            if (transition.isRunning) return@onClick
            transition.start(spanCount = lm.spanCount + 1, adapter, capturedView)
        }
        btnDecrease.onClick {
            if (transition.isRunning) return@onClick
            transition.start(spanCount = lm.spanCount - 1, adapter, capturedView)
        }
    }

    private fun createListAdapter() = listAdapter<AnimationItem> {
        val requestManager = Glide.with(this@MainActivity)
        register(SpanCategoryDelegate())
        register(SpanContentDelegate(requestManager))

        val items = mutableListOf<AnimationItem>()
        val categoryCount = 10
        val contentCount = 20
        (1..categoryCount).map {
            AnimationItem.Category("目录-$it", "目录-$it")
        }.forEach { category ->
            items.add(category)
            (1..contentCount).map { num ->
                val id = "${category.id}-$num"
                AnimationItem.Content(id, urls[num % urls.size], num)
            }.let(items::addAll)
        }
        listAdapter.submitList(items)
    }
}