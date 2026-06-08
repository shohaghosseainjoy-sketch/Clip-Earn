package com.example.ui.models

data class VideoItem(
    val id: String,
    val title: String,
    val creator: String,
    val thumbnailUrl: String,
    val durationString: String,
    val clapsCount: Int,
    val viewsCount: String,
    val authorAvatar: String,
    val isLiked: Boolean = false,
    val videoUrl: String = ""
)
