package com.xiaocydx.spananimation

import android.app.Activity
import android.content.Intent
import android.widget.ImageView
import androidx.core.app.ActivityOptionsCompat
import com.xiaocydx.spananimation.mulitype.SpanItem
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * @author xcc
 * @date 2023/2/5
 */
@Suppress("ObjectPropertyName")
object ContentPreviewHelper {
    /**
     * 示例代码主要展示过渡动画的效果，因此通过单例简单的处理Binder传输上限问题，
     * [ContentPreviewActivity]重建后，缺失集合数据会调用[Activity.finish]退出。
     */
    private var pendingParams: Params? = null
    private val _contentIdEvent = MutableSharedFlow<String>(
        extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val contentIdEvent = _contentIdEvent.asSharedFlow()

    fun start(
        activity: Activity,
        imageView: ImageView,
        current: SpanItem.Content,
        content: List<SpanItem.Content>
    ) {
        val position = content.indexOfFirst { it.id == current.id }
        pendingParams = Params(position, content)
        val intent = Intent(activity, ContentPreviewActivity::class.java)
        val options = ActivityOptionsCompat.makeSceneTransitionAnimation(
            activity, imageView, imageView.transitionName
        )
        activity.startActivity(intent, options.toBundle())
    }

    fun consumeParams(): Params? {
        return pendingParams.also { pendingParams = null }
    }

    fun selectContentId(id: String) {
        _contentIdEvent.tryEmit(id)
    }

    data class Params(val position: Int, val content: List<SpanItem.Content>)
}