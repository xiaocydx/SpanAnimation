package com.xiaocydx.spananimation

import android.app.SharedElementCallback
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.xiaocydx.cxrv.divider.spacing
import com.xiaocydx.cxrv.itemclick.doOnItemClick
import com.xiaocydx.cxrv.list.ListAdapter
import com.xiaocydx.cxrv.list.adapter
import com.xiaocydx.cxrv.list.grid
import com.xiaocydx.cxrv.list.submitList
import com.xiaocydx.cxrv.multitype.listAdapter
import com.xiaocydx.cxrv.multitype.register
import com.xiaocydx.sample.dp
import com.xiaocydx.sample.onClick
import com.xiaocydx.spananimation.databinding.ActivitySpanAnimationBinding
import com.xiaocydx.spananimation.mulitype.*
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * [SpanAnimationController]的示例代码
 *
 * @author xcc
 * @date 2022/12/18
 */
class SpanAnimationActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivitySpanAnimationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.rvAnimation
            .grid(spanCount = 3)
            .spacing(width = 3.dp, height = 3.dp)
            .adapter(createListAdapter()
                .also(::submitSpanList)
                .also(::setExitSharedElementCallback)
            )

        val controller = SpanAnimationController(binding.rvAnimation).apply {
            // setSpanCounts(2, 3, 4, 8)
            setScaleGestureEnabled(true)
            setImageViewProvider { (this as? SpanContentHolder)?.view?.imageView }
        }

        binding.btnIncrease.onClick { controller.takeIf { !it.isRunning }?.increase() }
        binding.btnDecrease.onClick { controller.takeIf { !it.isRunning }?.decrease() }
    }

    private fun createListAdapter() = listAdapter<SpanItem> {
        val activity = this@SpanAnimationActivity
        register(SpanCategoryDelegate())
        register(SpanContentDelegate(Glide.with(activity)).apply {
            setMaxScrap(20)
            doOnItemClick { holder, item ->
                ContentPreviewHelper.start(
                    activity = activity,
                    imageView = holder.view.imageView,
                    current = item,
                    content = adapter.currentList.filterIsInstance<SpanItem.Content>()
                )
            }
        })
    }

    private fun submitSpanList(adapter: ListAdapter<SpanItem, *>) {
        val list = mutableListOf<SpanItem>()
        val categoryCount = 10
        val contentCount = 20
        (1..categoryCount).map {
            SpanItem.Category("目录-$it", "目录-$it")
        }.forEach { category ->
            list.add(category)
            (1..contentCount).map { num ->
                val id = "${category.id}-$num"
                SpanItem.Content(id, urls[num % urls.size], num)
            }.let(list::addAll)
        }
        adapter.submitList(list)
    }

    private fun setExitSharedElementCallback(adapter: ListAdapter<SpanItem, *>) {
        var pendingPosition = -1
        ContentPreviewHelper.contentIdEvent
            .onEach { contentId ->
                pendingPosition = adapter.currentList.indexOfFirst {
                    it is SpanItem.Content && it.id == contentId
                }
                adapter.recyclerView?.scrollToPosition(pendingPosition)
            }
            .launchIn(lifecycleScope)

        setExitSharedElementCallback(object : SharedElementCallback() {
            override fun onMapSharedElements(
                names: MutableList<String>,
                sharedElements: MutableMap<String, View>
            ) {
                adapter.recyclerView?.takeIf { pendingPosition != -1 }
                    ?.findViewHolderForAdapterPosition(pendingPosition)
                    ?.let { it as? SpanContentHolder }
                    ?.let { holder ->
                        val name = names.first()
                        val view = holder.view.imageView
                        sharedElements[name] = view
                    }
                pendingPosition = -1
            }
        })
    }
}