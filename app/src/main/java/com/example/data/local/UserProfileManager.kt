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

    // ─── Progreso (XP / logros) ─────────────────────────────────────────────
    // Se guarda en las mismas prefs del perfil para que sobreviva al reinicio.
    private val _xp = MutableStateFlow(prefs.getInt(KEY_XP, 0))
    val xp: StateFlow<Int> = _xp.asStateFlow()

    private val _unlockedAchievements = MutableStateFlow(loadUnlockedAchievements())
    val unlockedAchievements: StateFlow<Set<String>> = _unlockedAchievements.asStateFlow()

    /**
     * Cuántas veces ha ocurrido cada evento real de la app, persistido.
     * Solo se usa para los contadores que no se pueden sacar de la base de datos
     * (hoy: escuchas completas). Descargas, reseñas, playlists y favoritas se cuentan
     * desde los datos reales del perfil.
     */
    private val _eventCounters = MutableStateFlow(loadEventCounters())
    val eventCounters: StateFlow<Map<XpSource, Int>> = _eventCounters.asStateFlow()

    /**
     * Racha de días seguidos escuchando al menos una canción completa.
     *
     * La métrica vive aquí (y no en el ViewModel) porque necesita persistir entre
     * reinicios: dos enteros y un día en las prefs, sin red ni base de datos. El día se
     * cuenta en UTC (`millis / 86.400.000`) para no depender de la zona horaria del
     * teléfono: el día cambia a las 00:00 UTC, no a medianoche local.
     */
    private val _currentStreak = MutableStateFlow(prefs.getInt(KEY_STREAK_CURRENT, 0))
    val currentStreak: StateFlow<Int> = _currentStreak.asStateFlow()

    /** Mejor racha histórica: es la que usan los logros, así no baja al fallar un día. */
    private val _bestStreak = MutableStateFlow(prefs.getInt(KEY_STREAK_BEST, 0))
    val bestStreak: StateFlow<Int> = _bestStreak.asStateFlow()

    /** Suma la XP de un evento real de la app (ver [XpSource]) y lo cuenta. */
    fun awardXp(source: XpSource): Int {
        recordEvent(source)
        if (source == XpSource.FULL_LISTEN) registerListeningDay()
        return addXp(source.xp)
    }

    /**
     * Anota que hoy se ha escuchado una canción completa y devuelve la racha actual.
     * Idempotente dentro del mismo día. Si el día anterior también contó, suma 1; si
     * hubo un hueco, la racha vuelve a 1 (la mejor racha nunca baja).
     */
    fun registerListeningDay(nowMillis: Long = System.currentTimeMillis()): Int {
        val today = nowMillis / DAY_MILLIS
        val lastDay = prefs.getLong(KEY_STREAK_LAST_DAY, NO_DAY)
        if (today == lastDay) return _currentStreak.value

        val current = if (lastDay != NO_DAY && today - lastDay == 1L) _currentStreak.value + 1 else 1
        val best = maxOf(current, _bestStreak.value)
        prefs.edit()
            .putLong(KEY_STREAK_LAST_DAY, today)
            .putInt(KEY_STREAK_CURRENT, current)
            .putInt(KEY_STREAK_BEST, best)
            .apply()
        _currentStreak.value = current
        _bestStreak.value = best
        return current
    }

    /** Anota que [source] ha ocurrido una vez más. Devuelve el total de ese evento. */
    fun recordEvent(source: XpSource): Int {
        val updated = (_eventCounters.value[source] ?: 0) + 1
        prefs.edit().putInt(counterKey(source), updated).apply()
        _eventCounters.value = _eventCounters.value + (source to updated)
        return updated
    }

    /**
     * Desbloquea los logros del catálogo cuya meta ya se cumple con [counters]
     * (contadores reales del perfil). Idempotente: lo ya desbloqueado no se toca,
     * así que un logro conseguido no se pierde si el contador baja luego.
     */
    fun syncAchievements(counters: Map<AchievementMetric, Int>) {
        AchievementsCatalog.reachedIds(counters).forEach { unlockAchievement(it) }
    }

    private fun loadEventCounters(): Map<XpSource, Int> =
        XpSource.values().associateWith { prefs.getInt(counterKey(it), 0) }

    /** Suma XP arbitraria (por ejemplo al desbloquear un logro). Devuelve el total. */
    fun addXp(amount: Int): Int {
        if (amount <= 0) return _xp.value
        val total = _xp.value + amount
        prefs.edit().putInt(KEY_XP, total).apply()
        _xp.value = total
        return total
    }

    /** Marca un logro como desbloqueado (idempotente). */
    fun unlockAchievement(id: String) {
        if (id.isBlank() || _unlockedAchievements.value.contains(id)) return
        val updated = _unlockedAchievements.value + id
        prefs.edit().putString(KEY_ACHIEVEMENTS, updated.joinToString("|||")).apply()
        _unlockedAchievements.value = updated
    }

    /**
     * Lee los logros guardados y aplica, si hace falta, la migración al catálogo nuevo
     * ([AchievementsCatalog.migrateLegacyUnlocks]). Es idempotente: con los ids ya
     * migrados no reescribe nada.
     */
    private fun loadUnlockedAchievements(): Set<String> {
        val raw = prefs.getString(KEY_ACHIEVEMENTS, null) ?: return emptySet()
        val stored = raw.split("|||").filter { it.isNotBlank() }.toSet()
        val migrated = AchievementsCatalog.migrateLegacyUnlocks(stored)
        if (migrated != stored) {
            prefs.edit().putString(KEY_ACHIEVEMENTS, migrated.joinToString("|||")).apply()
        }
        return migrated
    }

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

    private companion object {
        const val KEY_XP = "profile_xp"
        const val KEY_ACHIEVEMENTS = "profile_unlocked_achievements"

        /** Racha de días (ver [registerListeningDay]). */
        const val KEY_STREAK_CURRENT = "profile_streak_current"
        const val KEY_STREAK_BEST = "profile_streak_best"
        const val KEY_STREAK_LAST_DAY = "profile_streak_last_day"
        const val NO_DAY = -1L
        const val DAY_MILLIS = 86_400_000L

        /** Clave de prefs del contador de un evento (una por [XpSource]). */
        fun counterKey(source: XpSource) = "profile_event_count_${source.name}"
    }
}
