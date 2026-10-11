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
 * Contador real que decide si un logro se desbloquea. Cada valor sale de datos que la app
 * ya tiene (no se inventa nada ni hace falta backend):
 *
 *  - [DOWNLOADS]        canciones descargadas que siguen en el teléfono
 *  - [REVIEWS]          reseñas guardadas en la base de datos
 *  - [PLAYLISTS]        playlists creadas
 *  - [FULL_LISTENS]     canciones escuchadas hasta el final (contador persistido, se suma
 *                       cada vez que suena el evento de escucha completa)
 *  - [LEVEL]            nivel de XP alcanzado ([LevelCurve])
 *  - [PINNED_FAVORITES] huecos de "Mis 5 favoritas" que el dueño ha fijado
 */
enum class AchievementMetric {
    DOWNLOADS,
    REVIEWS,
    PLAYLISTS,
    FULL_LISTENS,
    LEVEL,
    PINNED_FAVORITES
}

/**
 * Logro desbloqueable. [metric] + [goal] son la condición real: el logro se desbloquea
 * cuando el contador de [metric] llega a [goal] (y una vez desbloqueado no se pierde,
 * porque el desbloqueo se guarda por [id] en las prefs del perfil:
 * `UserProfileManager.unlockAchievement`).
 */
data class Achievement(
    val id: String,
    val title: String,
    val description: String,
    val emoji: String = "🏆",
    val metric: AchievementMetric = AchievementMetric.DOWNLOADS,
    val goal: Int = 1
)

object AchievementsCatalog {
    /**
     * ═══════════════════════════════════════════════════════════════════════
     *  PUNTO ÚNICO PARA AÑADIR/CAMBIAR LOGROS
     *
     *  ⚠️ PROPUESTA DEL EQUIPO (Faro Labs): es una lista base para que la sección
     *  funcione de verdad mientras el dueño no ha pasado la suya. Cuando llegue su
     *  lista, se sustituye AQUÍ y en ningún otro sitio: la app no guarda títulos,
     *  descripciones ni metas, solo los `id` ya desbloqueados en las prefs.
     *
     *  Reglas al editar:
     *   1. `id` nuevo y estable (si se cambia un `id`, ese logro vuelve a salir con candado).
     *   2. `metric` + `goal` deben poder medirse con datos que la app ya tiene
     *      (ver [AchievementMetric]); no hay backend ni Firebase todavía.
     *   3. La UI de la pestaña 1 (Perfil) pinta la lista, el candado y el progreso
     *      "3/5" sola: no hay que tocar la pantalla.
     * ═══════════════════════════════════════════════════════════════════════
     */
    val all: List<Achievement> = listOf(
        // ── Descargas ────────────────────────────────────────────────────────
        Achievement(
            id = "first_download",
            title = "Primera descarga",
            description = "Descargaste tu primera canción",
            emoji = "⬇️",
            metric = AchievementMetric.DOWNLOADS,
            goal = 1
        ),
        Achievement(
            id = "downloads_5",
            title = "Coleccionista",
            description = "5 canciones descargadas",
            emoji = "📥",
            metric = AchievementMetric.DOWNLOADS,
            goal = 5
        ),
        Achievement(
            id = "downloads_25",
            title = "Biblioteca Hi-Fi",
            description = "25 canciones descargadas",
            emoji = "💾",
            metric = AchievementMetric.DOWNLOADS,
            goal = 25
        ),

        // ── Reseñas ──────────────────────────────────────────────────────────
        Achievement(
            id = "first_review",
            title = "Primera reseña",
            description = "Escribiste tu primera reseña",
            emoji = "✍️",
            metric = AchievementMetric.REVIEWS,
            goal = 1
        ),
        Achievement(
            id = "reviews_10",
            title = "Crítico musical",
            description = "10 canciones reseñadas",
            emoji = "📝",
            metric = AchievementMetric.REVIEWS,
            goal = 10
        ),

        // ── Playlists ────────────────────────────────────────────────────────
        Achievement(
            id = "first_playlist",
            title = "Primera playlist",
            description = "Creaste tu primera playlist",
            emoji = "🎼",
            metric = AchievementMetric.PLAYLISTS,
            goal = 1
        ),
        Achievement(
            id = "playlists_5",
            title = "Curador",
            description = "5 playlists creadas",
            emoji = "🎧",
            metric = AchievementMetric.PLAYLISTS,
            goal = 5
        ),

        // ── Escuchas ─────────────────────────────────────────────────────────
        Achievement(
            id = "listens_100",
            title = "Cien escuchas",
            description = "100 canciones escuchadas completas",
            emoji = "🔊",
            metric = AchievementMetric.FULL_LISTENS,
            goal = 100
        ),

        // ── Nivel y favoritas ────────────────────────────────────────────────
        Achievement(
            id = "level_5",
            title = "Audiófilo",
            description = "Alcanzaste el nivel 5",
            emoji = "⭐",
            metric = AchievementMetric.LEVEL,
            goal = 5
        ),
        Achievement(
            id = "pinned_5",
            title = "Escaparate completo",
            description = "Fijaste tus 5 favoritas",
            emoji = "📌",
            metric = AchievementMetric.PINNED_FAVORITES,
            goal = 5
        )
    )

    /** Cuántos logros del catálogo ya cumplen su meta con [counters]. */
    fun reachedIds(counters: Map<AchievementMetric, Int>): List<String> =
        all.filter { (counters[it.metric] ?: 0) >= it.goal }.map { it.id }

    /** Valor actual del contador de un logro (0 si todavía no se ha medido). */
    fun progressOf(achievement: Achievement, counters: Map<AchievementMetric, Int>): Int =
        counters[achievement.metric] ?: 0
}
