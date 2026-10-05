package com.example.data.model

data class Artist(
    val id: String,
    val name: String,
    val imageUrl: String,
    val isFollowing: Boolean = false,
    val followersText: String = "1.2M oyentes"
)
