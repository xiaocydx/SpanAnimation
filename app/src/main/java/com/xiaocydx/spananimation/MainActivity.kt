package com.xiaocydx.spananimation

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
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

        rvAnimation
            .grid(spanCount = 3)
            .spacing(width = 3.dp, height = 3.dp)
            .adapter(createAnimationAdapter())

        val controller = SpanAnimationController(rvAnimation).apply {
            setScaleGestureEnabled(true)
            setImageViewProvider { (this as? SpanContentHolder)?.view?.imageView }
        }

        btnIncrease.onClick {
            if (!controller.isRunning) controller.increase()
        }
        btnDecrease.onClick {
            if (!controller.isRunning) controller.decrease()
        }
    }

    private fun createAnimationAdapter() = listAdapter<AnimationItem> {
        val requestManager = Glide.with(this@MainActivity)
        register(SpanCategoryDelegate())
        register(SpanContentDelegate(requestManager))

        val list = mutableListOf<AnimationItem>()
        val categoryCount = 10
        val contentCount = 20
        (1..categoryCount).map {
            AnimationItem.Category("目录-$it", "目录-$it")
        }.forEach { category ->
            list.add(category)
            (1..contentCount).map { num ->
                val id = "${category.id}-$num"
                AnimationItem.Content(id, urls[num % urls.size], num)
            }.let(list::addAll)
        }
        listAdapter.submitList(list)
    }
}