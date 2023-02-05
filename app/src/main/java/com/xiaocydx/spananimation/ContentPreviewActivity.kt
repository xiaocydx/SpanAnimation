package com.xiaocydx.spananimation

import android.app.Instrumentation
import android.app.SharedElementCallback
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.transition.*
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.transition.doOnEnd
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.MarginPageTransformer
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestManager
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.xiaocydx.cxrv.itemclick.doOnSimpleItemClick
import com.xiaocydx.cxrv.list.ListAdapter
import com.xiaocydx.cxrv.list.getItem
import com.xiaocydx.cxrv.list.submitList
import com.xiaocydx.sample.dp
import com.xiaocydx.sample.matchParent
import com.xiaocydx.sample.withLayoutParams
import com.xiaocydx.spananimation.mulitype.SpanItem

/**
 * **注意**：[ContentPreviewActivity]主题的`windowIsTranslucent = true`，
 * 作用是让[SpanAnimationActivity]的视图树能measure、layout，同步当前位置，
 * [SpanAnimationActivity]的生命周期状态只会回退到[Lifecycle.State.STARTED]，
 * 实际场景可能需要将活跃状态范围限制为[Lifecycle.State.RESUMED]。
 *
 * @author xcc
 * @date 2023/2/4
 */
class ContentPreviewActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val params = ContentPreviewHelper.consumeParams() ?: return finish()

        window.requestFeature(Window.FEATURE_ACTIVITY_TRANSITIONS)
        window.sharedElementEnterTransition = TransitionSet().apply {
            duration = 250
            interpolator = FastOutSlowInInterpolator()
            addTransition(ChangeBounds())
            addTransition(ChangeImageTransform())
        }

        val requestManager = Glide.with(this)
        val viewPager = ViewPager2(this).apply {
            adapter = ContentPreviewAdapter(requestManager).apply {
                submitList(params.content)
                doOnSimpleItemClick { finishAfterTransition() }
            }
            setBackgroundColor(Color.BLACK)
            withLayoutParams(matchParent, matchParent)
            setPageTransformer(MarginPageTransformer(5.dp))
            setCurrentItem(params.position, false)
        }
        setEnterSharedElementCallback(viewPager)
        postponeEnterTransition(requestManager, viewPager)
        setContentView(viewPager)
    }

    @Suppress("UNCHECKED_CAST")
    private fun setEnterSharedElementCallback(viewPager: ViewPager2) {
        val adapter = requireNotNull(viewPager.adapter as? ListAdapter<SpanItem.Content, *>)

        // 发送事件同步当前位置的contentId
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                ContentPreviewHelper.selectContentId(adapter.getItem(position).id)
            }
        })

        // 退出时调整为当前位置的itemView
        setEnterSharedElementCallback(object : SharedElementCallback() {
            override fun onMapSharedElements(
                names: MutableList<String>,
                sharedElements: MutableMap<String, View>
            ) {
                val holder = adapter.recyclerView
                    ?.findViewHolderForAdapterPosition(viewPager.currentItem) ?: return
                sharedElements[names.first()] = holder.itemView
            }
        })
    }

    private fun postponeEnterTransition(requestManager: RequestManager, viewPager: ViewPager2) {
        // 推迟过渡动画，直至图片加载结束
        postponeEnterTransition()
        requestManager.addDefaultRequestListener(object : RequestListener<Any> {
            override fun onResourceReady(
                resource: Any?, model: Any?,
                target: Target<Any>?, dataSource: DataSource?, isFirstResource: Boolean
            ): Boolean {
                startPostponedEnterTransition()
                return false
            }

            override fun onLoadFailed(
                e: GlideException?, model: Any?,
                target: Target<Any>?, isFirstResource: Boolean
            ): Boolean {
                startPostponedEnterTransition()
                return false
            }
        })

        // 过渡动画结束时，才将viewPager.offscreenPageLimit修改为1，
        // 确保startPostponedEnterTransition()不受两侧加载图片影响。
        window.sharedElementEnterTransition.doOnEnd { viewPager.offscreenPageLimit = 1 }
    }

    override fun onPause() {
        super.onPause()
        if (!isFinishing && Build.VERSION.SDK_INT >= 29) {
            // 兼容Android 10及以上，执行onStop()后再恢复，退出时无过渡动画的问题
            Instrumentation().callActivityOnSaveInstanceState(this, Bundle())
        }
    }
}

private class ContentPreviewAdapter(
    private val requestManager: RequestManager
) : ListAdapter<SpanItem.Content, RecyclerView.ViewHolder>() {
    override fun areItemsTheSame(
        oldItem: SpanItem.Content,
        newItem: SpanItem.Content
    ): Boolean = oldItem.id == newItem.id

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val imageView = AppCompatImageView(parent.context).apply {
            scaleType = ImageView.ScaleType.FIT_CENTER
            withLayoutParams(matchParent, matchParent)
        }
        return object : RecyclerView.ViewHolder(imageView) {}
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, item: SpanItem.Content) {
        val imageView = holder.itemView as ImageView
        imageView.transitionName = item.id
        requestManager.load(item.url).into(imageView)
    }
}