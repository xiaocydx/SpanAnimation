package com.xiaocydx.spananimation.mulitype

import java.io.Serializable

/**
 * @author xcc
 * @date 2022/12/17
 */
sealed class SpanItem : Serializable {
    data class Category(val id: String, val text: String) : SpanItem()
    data class Content(val id: String, val url: String, val num: Int) : SpanItem()
}

val urls = listOf(
    "https://cdn.pixabay.com/photo/2019/10/31/06/24/lake-4591082_1280.jpg",
    "https://cdn.pixabay.com/photo/2024/01/31/19/25/sunset-8544672_1280.jpg",
    "https://cdn.pixabay.com/photo/2016/11/08/05/18/hot-air-balloons-1807521_960_720.jpg",
    "https://cdn.pixabay.com/photo/2021/02/22/10/08/night-sky-6039591_1280.jpg",
    "https://cdn.pixabay.com/photo/2019/07/16/11/14/lake-4341560_1280.jpg",
    "https://cdn.pixabay.com/photo/2013/07/25/01/33/boat-166738_1280.jpg",
    "https://cdn.pixabay.com/photo/2019/11/18/18/29/mountain-4635428_1280.jpg",
    "https://cdn.pixabay.com/photo/2020/07/11/22/13/tree-5395485_1280.jpg",
    "https://cdn.pixabay.com/photo/2020/10/20/13/24/sunrise-5670440_1280.jpg",
    "https://cdn.pixabay.com/photo/2021/03/16/01/19/sunset-6098406_960_720.jpg",
)