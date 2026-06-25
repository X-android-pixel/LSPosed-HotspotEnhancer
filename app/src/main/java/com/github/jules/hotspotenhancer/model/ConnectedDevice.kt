package com.github.jules.hotspotenhancer.model

data class ConnectedDevice(
    val macAddress: String,
    val ipAddress: String?,
    val hostname: String?,
    val connectionTime: Long = System.currentTimeMillis()
)
