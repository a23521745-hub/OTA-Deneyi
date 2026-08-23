package com.example.otadashboard.ota_updater

data class UpdateInfo(
    val versionCode: Int,
    val currentVersionCode: Int,
    val apkUrl: String,
    val signature: String,
    val changelog: String,
    val fileSizeFormatted: String,
    val publishedAt: String,
    val hasUpdate: Boolean
)