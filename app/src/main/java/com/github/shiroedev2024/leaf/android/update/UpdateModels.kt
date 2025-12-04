package com.github.shiroedev2024.leaf.android.update

import androidx.annotation.Keep

@Keep data class ChangeLogEntry(val languageCode: String, val text: String)

@Keep data class DownloadSource(val type: String, val url: String, val variant: String? = null)

@Keep
data class UpdateResponse(
    val available: Boolean,
    val downloadName: String? = null,
    val downloadDescription: String? = null,
    val latestVersionName: String? = null,
    val publishedDate: String? = null,
    val downloadSources: List<DownloadSource> = emptyList(),
    val changeLog: List<ChangeLogEntry> = emptyList(),
)
