package com.example.data.local

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class UserProfile(
    val username: String = "Deivi Medina",
    val handle: String = "@deivi_audiophile",
    val bio: String = "Apasionado por el audio en alta resolución (Hi-Res FLAC & 320k), vinilos y producción musical.",
    val favoriteGenre: String = "Rock & Electronic",
    val audioSetup: String = "Sennheiser HD 600 + DAC Fiio BTR5",
    val avatarId: Int = 1 // 1: Headphones, 2: MusicNote, 3: Equalizer, 4: Vinyl
)

class UserProfileManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("audiophiles_profile_prefs", Context.MODE_PRIVATE)

    private val _profile = MutableStateFlow(loadProfile())
    val profile: StateFlow<UserProfile> = _profile.asStateFlow()

    private val _recentSearches = MutableStateFlow(loadRecentSearches())
    val recentSearches: StateFlow<List<String>> = _recentSearches.asStateFlow()

    private fun loadProfile(): UserProfile {
        return UserProfile(
            username = prefs.getString("username", "Deivi Medina") ?: "Deivi Medina",
            handle = prefs.getString("handle", "@deivi_audiophile") ?: "@deivi_audiophile",
            bio = prefs.getString("bio", "Apasionado por el audio en alta resolución (Hi-Res FLAC & 320k), vinilos y producción musical.") ?: "",
            favoriteGenre = prefs.getString("favorite_genre", "Rock & Electronic") ?: "Rock & Electronic",
            audioSetup = prefs.getString("audio_setup", "Sennheiser HD 600 + DAC Fiio BTR5") ?: "Sennheiser HD 600",
            avatarId = prefs.getInt("avatar_id", 1)
        )
    }

    fun saveProfile(newProfile: UserProfile) {
        prefs.edit()
            .putString("username", newProfile.username)
            .putString("handle", newProfile.handle)
            .putString("bio", newProfile.bio)
            .putString("favorite_genre", newProfile.favoriteGenre)
            .putString("audio_setup", newProfile.audioSetup)
            .putInt("avatar_id", newProfile.avatarId)
            .apply()
        _profile.value = newProfile
    }

    fun addSearchQuery(query: String) {
        val clean = query.trim()
        if (clean.isBlank()) return
        val current = _recentSearches.value.toMutableList()
        current.removeAll { it.equals(clean, ignoreCase = true) }
        current.add(0, clean)
        val trimmed = current.take(15)
        prefs.edit().putString("recent_searches", trimmed.joinToString("|||")).apply()
        _recentSearches.value = trimmed
    }

    private fun loadRecentSearches(): List<String> {
        val raw = prefs.getString("recent_searches", null) ?: return listOf("Queen", "Daft Punk", "Pink Floyd", "Canserbero", "Gorillaz")
        return raw.split("|||").filter { it.isNotBlank() }
    }
}
