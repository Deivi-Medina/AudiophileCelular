package com.example.data.network

import com.example.data.model.YouTubeTrackResult
import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Streaming

interface AudiophilesApiService {
    @GET("api/search")
    suspend fun searchTracks(
        @Query("q") query: String
    ): List<YouTubeTrackResult>

    @Streaming
    @GET("api/download")
    suspend fun downloadTrack(
        @Query("videoId") videoId: String,
        @Query("bitrate") bitrate: String
    ): ResponseBody
}
