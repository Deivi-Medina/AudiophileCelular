package com.example.data.model

sealed class DownloadState {
    data object Idle : DownloadState()
    data object Queued : DownloadState()
    data class Downloading(val progress: Float) : DownloadState()
    data object Completed : DownloadState()
    data class Error(val message: String) : DownloadState()
}
