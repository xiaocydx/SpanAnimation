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

val urls = arrayOf(
    "https://cdn.pixabay.com/photo/2022/07/01/14/29/wheat-7295718_960_720.jpg",
    "https://cdn.pixabay.com/photo/2022/07/10/18/21/allium-7313550_960_720.jpg",
    "https://cdn.pixabay.com/photo/2022/07/04/23/52/beach-7302072_960_720.jpg",
    "https://cdn.pixabay.com/photo/2022/07/05/11/06/mountains-7302806_960_720.jpg",
    "https://cdn.pixabay.com/photo/2022/07/12/11/30/morning-view-7317119_960_720.jpg",
    "https://cdn.pixabay.com/photo/2022/07/10/21/25/sky-7313775_960_720.jpg",
    "https://cdn.pixabay.com/photo/2022/07/13/07/10/branch-7318716_960_720.jpg",
    "https://cdn.pixabay.com/photo/2022/07/09/17/20/mushroom-7311371_960_720.jpg",
    "https://cdn.pixabay.com/photo/2022/07/06/12/11/spaceship-7304985_960_720.jpg",
    "https://cdn.pixabay.com/photo/2022/06/19/07/12/mount-kilimanjaro-7271184_960_720.jpg",
)