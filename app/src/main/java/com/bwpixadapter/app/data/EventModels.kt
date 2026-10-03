package com.bwpixadapter.app.data

data class EventItem(
    val index: Int,
    val type: Int,
    val timestamp: Long,
    val picturePath: String?,
)

data class RecordItem(
    val name: String,
    val size: Long,
    val startTime: Long,
    val endTime: Long,
)
