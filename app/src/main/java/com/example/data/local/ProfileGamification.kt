package com.example.data.local

/**
 * Progreso del perfil: XP, niveles y logros.
 *
 * Los valores de XP son una decisión de producto (no hay backend todavía) y viven aquí
 * para que se puedan ajustar en un solo sitio. Un evento real de la app = un [XpSource]:
 *
 *  - Descargar una canción            -> 10 XP
 *  - Escribir una reseña              -> 15 XP
 *  - Escuchar una canción completa    ->  2 XP
 *  - Crear una playlist               -> 20 XP
 *
 * Una descarga cuenta una sola vez por canción (se deduplica en el ViewModel);
 * escuchar cuenta cada vez que la canción termina, que es justo lo que se premia.
 */
enum class XpSource(val xp: Int, val label: String) {
    DOWNLOAD(10, "Descargar canción"),
    REVIEW(15, "Escribir reseña"),
    FULL_LISTEN(2, "Escuchar canción completa"),
    CREATE_PLAYLIST(20, "Crear playlist")
}

/**
 * Curva de niveles: XP acumulada para alcanzar el nivel L = [XP_BASE] * L * (L - 1).
 *
 *   Nivel 1 ->     0 XP   (Oyente curioso)
 *   Nivel 2 ->    50 XP
 *   Nivel 3 ->   150 XP
 *   Nivel 4 ->   300 XP
 *   Nivel 5 ->   500 XP
 *   Nivel 6 ->   750 XP
 *   Nivel 7 ->  1050 XP   (Maestro Hi-Fi)
 *   Nivel 8 ->  1400 XP
 *
 * El coste del siguiente nivel sube de forma lineal para que al principio se suba rápido
 * (los eventos valen 2-20 XP) y luego haga falta constancia.
 */
object LevelCurve {
    /** XP que aporta el "paso" base de la curva. Subirlo hace los niveles más caros. */
    const val XP_BASE = 25

    /** XP acumulada necesaria para tener [level]. El nivel 1 empieza en 0 XP. */
    fun totalXpForLevel(level: Int): Int {
        val safe = level.coerceAtLeast(1)
        return XP_BASE * safe * (safe - 1)
    }

    /** Nivel correspondiente a [xp] acumulada (1 = recién empezado). */
    fun levelForXp(xp: Int): Int {
        val safe = xp.coerceAtLeast(0)
        var level = 1
        while (totalXpForLevel(level + 1) <= safe) level++
        return level
    }

    /** Nombre del nivel, para pintarlo junto al número. */
    fun titleForLevel(level: Int): String = when (level.coerceAtLeast(1)) {
        1 -> "Oyente Curioso"
        2 -> "Explorador"
        3 -> "Coleccionista"
        4 -> "Melómano"
        5 -> "Audiófilo"
        6 -> "Audiófilo Senior"
        7 -> "Maestro Hi-Fi"
        8 -> "Ingeniero de Mezcla"
        9 -> "Productor Cósmico"
        else -> "Leyenda del Sonido"
    }

    /** XP ya ganada dentro del nivel actual. */
    fun xpIntoLevel(xp: Int): Int {
        val safe = xp.coerceAtLeast(0)
        return safe - totalXpForLevel(levelForXp(safe))
    }

    /** XP que hay que ganar en total dentro del nivel actual para pasar al siguiente. */
    fun xpForNextLevel(level: Int): Int {
        val safe = level.coerceAtLeast(1)
        return totalXpForLevel(safe + 1) - totalXpForLevel(safe)
    }

    /** Progreso 0f..1f hacia el siguiente nivel, para la barra. */
    fun levelProgress(xp: Int): Float {
        val level = levelForXp(xp)
        val span = xpForNextLevel(level).coerceAtLeast(1)
        return (xpIntoLevel(xp).toFloat() / span).coerceIn(0f, 1f)
    }
}

/**
 * Logro desbloqueable. El desbloqueo se guarda por [id] en las prefs del perfil
 * (`UserProfileManager.unlockAchievement`), así que la lista se puede llenar más tarde
 * sin migrar nada.
 */
data class Achievement(
    val id: String,
    val title: String,
    val description: String,
    val emoji: String = "🏆"
)

object AchievementsCatalog {
    /**
     * ─────────────────────────────────────────────────────────────
     *  PUNTO ÚNICO PARA AÑADIR LOGROS
     *  El dueño todavía no ha pasado su lista: hoy está vacía a propósito.
     *  Cuando la traiga, añadir aquí cada logro, por ejemplo:
     *
     *    Achievement("first_download", "Primera descarga", "Bajaste tu primera canción"),
     *    Achievement("ten_reviews", "Crítico", "Escribiste 10 reseñas", "✍️"),
     *
     *  y desbloquearlo desde la lógica correspondiente con
     *  `userProfileManager.unlockAchievement("first_download")`.
     *  La UI de la pestaña 1 (Perfil) ya pinta la lista con candado/desbloqueado:
     *  no hay que tocar la pantalla.
     * ─────────────────────────────────────────────────────────────
     */
    val all: List<Achievement> = emptyList()
}
