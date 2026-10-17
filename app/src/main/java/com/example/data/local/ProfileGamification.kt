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
 * Escala de niveles 0..100 con un TÍTULO cada 10 niveles.
 *
 * Curva (una sola fórmula, sin tablas a mano):
 *
 *     XP acumulada para el nivel L = 9 · L^1,7      (redondeada a entero)
 *     Nivel 0 = 0 XP  ·  Nivel 100 = 22.607 XP (tope)
 *
 * Para qué está calibrada: un usuario normal hace entre 10 y 20 acciones de XP al día
 * (~50-100 XP mezclando escuchas, descargas y alguna reseña). Con ese ritmo:
 *
 *   Nivel 10  ->    451 XP  ->  ~4-9 días          (engancha desde el principio)
 *   Nivel 25  ->  2.142 XP  ->  ~3 semanas
 *   Nivel 50  ->  6.958 XP  ->  ~2-5 meses         (constancia, no locura)
 *   Nivel 75  -> 13.863 XP  ->  ~5-9 meses
 *   Nivel 100 -> 22.607 XP  ->  ~8-15 meses        (meta larga pero real)
 *
 * Por qué exponente 1,7 y no algo lineal: el coste por nivel tiene que crecer, pero no
 * tanto como para que el nivel 100 sea inalcanzable (una exponencial pura lo volvería
 * imposible y una recta lo dejaría en semanas).
 *
 * La XP guardada NO se toca: el nivel siempre se deriva del total acumulado
 * ([levelForXp]), así que al cambiar la curva el nivel del dueño se recalcula solo.
 */
object LevelCurve {
    /** Nivel máximo de la escala. */
    const val MAX_LEVEL = 100

    /** Cada cuántos niveles se gana un título ([titleForLevel]). */
    const val TITLE_STEP = 10

    /** Constante de la curva: subirla hace todos los niveles más caros. */
    private const val XP_UNIT = 9.0

    /** Exponente de la curva: cuánto se encarece cada nivel respecto al anterior. */
    private const val CURVE_EXPONENT = 1.7

    /** Título neutro mientras no se llega al primer hito (nivel 10). */
    const val INITIAL_TITLE = "Oyente Nuevo"

    /**
     * Títulos, uno por cada hito de 10 niveles. Progresión musical/audiófila:
     * de recién llegado a leyenda del sonido.
     */
    private val TITLES: Map<Int, String> = mapOf(
        10 to "Oído Curioso",
        20 to "Coleccionista de Surcos",
        30 to "Melómano",
        40 to "Curador Sonoro",
        50 to "Audiófilo",
        60 to "Oído Absoluto",
        70 to "Maestro de Mezcla",
        80 to "Arquitecto del Sonido",
        90 to "Sabio de Frecuencias",
        100 to "Leyenda del Sonido"
    )

    /** XP acumulada de cada nivel, calculada una sola vez y en enteros. */
    private val thresholds: IntArray = IntArray(MAX_LEVEL + 1) { level ->
        if (level <= 0) 0
        else Math.round(XP_UNIT * Math.pow(level.toDouble(), CURVE_EXPONENT)).toInt()
    }

    /** XP acumulada necesaria para tener [level] (nivel 0 = 0 XP). */
    fun totalXpForLevel(level: Int): Int = thresholds[level.coerceIn(0, MAX_LEVEL)]

    /** Nivel correspondiente a [xp] acumulada (0 = recién empezado, 100 = tope). */
    fun levelForXp(xp: Int): Int {
        val safe = xp.coerceAtLeast(0)
        var low = 0
        var high = MAX_LEVEL
        while (low < high) {
            val mid = (low + high + 1) / 2
            if (thresholds[mid] <= safe) low = mid else high = mid - 1
        }
        return low
    }

    /** Título del nivel: el del último hito de 10 alcanzado. */
    fun titleForLevel(level: Int): String {
        val milestone = level.coerceIn(0, MAX_LEVEL) / TITLE_STEP * TITLE_STEP
        return TITLES[milestone] ?: INITIAL_TITLE
    }

    /** Nivel del siguiente título, o `null` si ya se está en el último (100). */
    fun nextTitleLevel(level: Int): Int? {
        val next = (level.coerceIn(0, MAX_LEVEL) / TITLE_STEP + 1) * TITLE_STEP
        return if (next <= MAX_LEVEL) next else null
    }

    /** Nombre del siguiente título a conseguir, o `null` si ya es Leyenda. */
    fun nextTitle(level: Int): String? = nextTitleLevel(level)?.let { TITLES[it] }

    /** XP ya ganada dentro del nivel actual. */
    fun xpIntoLevel(xp: Int): Int = xp.coerceAtLeast(0) - totalXpForLevel(levelForXp(xp))

    /** XP que hay que ganar dentro del nivel actual para pasar al siguiente (0 en el 100). */
    fun xpForNextLevel(level: Int): Int {
        val safe = level.coerceIn(0, MAX_LEVEL)
        if (safe >= MAX_LEVEL) return 0
        return thresholds[safe + 1] - thresholds[safe]
    }

    /** Progreso 0f..1f hacia el siguiente nivel, para la barra. */
    fun levelProgress(xp: Int): Float {
        val level = levelForXp(xp)
        if (level >= MAX_LEVEL) return 1f
        val span = xpForNextLevel(level).coerceAtLeast(1)
        return (xpIntoLevel(xp).toFloat() / span).coerceIn(0f, 1f)
    }

    /** `true` cuando ya se está en el tope de la escala. */
    fun isMaxLevel(level: Int): Boolean = level >= MAX_LEVEL
}

/**
 * Contador real que decide si un logro se desbloquea. Cada valor sale de datos que la app
 * ya tiene (no se inventa nada ni hace falta backend ni Firebase):
 *
 *  - [DOWNLOADS]         canciones descargadas desde la app que siguen en el teléfono
 *  - [FLAC_DOWNLOADS]    de esas, las descargadas en FLAC / lossless
 *  - [DISTINCT_ARTISTS]  artistas distintos entre las canciones descargadas
 *  - [REVIEWS]           reseñas guardadas en la base de datos
 *  - [FIVE_STAR_REVIEWS] reseñas con nota de 4,5★ o más
 *  - [PLAYLISTS]         playlists creadas
 *  - [FULL_LISTENS]      canciones escuchadas hasta el final (contador persistido)
 *  - [BEST_STREAK]       mejor racha de días seguidos escuchando algo completo
 *  - [LEVEL]             nivel de XP alcanzado ([LevelCurve])
 *  - [PINNED_FAVORITES]  huecos de "Mis 5 favoritas" que el dueño ha fijado
 *
 * Las tres últimas ([BEST_STREAK] es la única nueva de verdad) se añadieron con este
 * catálogo y se calculan sin coste: FLAC y artistas salen de la biblioteca ya descargada
 * (campo `qualityLabel` y `artist` de la canción) y la racha la cuenta
 * [UserProfileManager] con dos enteros en las prefs, sin tocar red ni base de datos.
 */
enum class AchievementMetric {
    DOWNLOADS,
    FLAC_DOWNLOADS,
    DISTINCT_ARTISTS,
    REVIEWS,
    FIVE_STAR_REVIEWS,
    PLAYLISTS,
    FULL_LISTENS,
    BEST_STREAK,
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

/**
 * Catálogo de logros diseñado por el equipo (Faro Labs) a petición del dueño.
 *
 * 16 logros, todos medibles con datos que la app ya registra. La regla al tocarlo:
 * el `id` es el contrato con lo ya guardado — si cambia, ese logro vuelve a salir con
 * candado (por eso [LEGACY_ID_ALIASES] existe).
 */
object AchievementsCatalog {
    /**
     * ═══════════════════════════════════════════════════════════════════════
     *  PUNTO ÚNICO PARA AÑADIR/CAMBIAR LOGROS
     *
     *  El dueño nos delegó el catálogo ("los logros ponlos tú"). Está ordenado por
     *  familia (descargas, reseñas, playlists, escuchas, nivel) y la UI de la pestaña 1
     *  pinta lista, candado y progreso "3/25" sola: no hay que tocar la pantalla.
     *
     *  Reglas al editar:
     *   1. `id` nuevo y estable. Para retirar un logro, añade su id a [LEGACY_ID_ALIASES]
     *      apuntando al logro que lo sustituye (o a `null` si no tiene sustituto).
     *   2. `metric` + `goal` deben poder medirse con datos que la app ya tiene
     *      (ver [AchievementMetric]); no hay backend ni Firebase todavía.
     * ═══════════════════════════════════════════════════════════════════════
     */
    val all: List<Achievement> = listOf(
        // ── Descargas ────────────────────────────────────────────────────────
        Achievement(
            id = "first_download",
            title = "Primera descarga",
            description = "Guardaste tu primera canción en el teléfono",
            emoji = "⬇️",
            metric = AchievementMetric.DOWNLOADS,
            goal = 1
        ),
        Achievement(
            id = "downloads_50",
            title = "Biblioteca Hi-Fi",
            description = "50 canciones descargadas",
            emoji = "📥",
            metric = AchievementMetric.DOWNLOADS,
            goal = 50
        ),
        Achievement(
            id = "flac_10",
            title = "Oído de estudio",
            description = "10 canciones descargadas en FLAC",
            emoji = "🎚️",
            metric = AchievementMetric.FLAC_DOWNLOADS,
            goal = 10
        ),
        Achievement(
            id = "artists_25",
            title = "Cazador de artistas",
            description = "25 artistas distintos en tu biblioteca",
            emoji = "🎯",
            metric = AchievementMetric.DISTINCT_ARTISTS,
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
        Achievement(
            id = "reviews_50",
            title = "Pluma de oro",
            description = "50 canciones reseñadas",
            emoji = "🖋️",
            metric = AchievementMetric.REVIEWS,
            goal = 50
        ),
        Achievement(
            id = "five_star_10",
            title = "Diez de diez",
            description = "10 reseñas de 4,5★ o más",
            emoji = "⭐",
            metric = AchievementMetric.FIVE_STAR_REVIEWS,
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
        Achievement(
            id = "listens_1000",
            title = "Maratón sonoro",
            description = "1.000 canciones escuchadas completas",
            emoji = "🔥",
            metric = AchievementMetric.FULL_LISTENS,
            goal = 1000
        ),
        Achievement(
            id = "streak_7",
            title = "Semana perfecta",
            description = "Escuchaste algo completo 7 días seguidos",
            emoji = "📆",
            metric = AchievementMetric.BEST_STREAK,
            goal = 7
        ),
        // ── Nivel y favoritas ────────────────────────────────────────────────
        Achievement(
            id = "pinned_5",
            title = "Escaparate completo",
            description = "Fijaste tus 5 favoritas",
            emoji = "📌",
            metric = AchievementMetric.PINNED_FAVORITES,
            goal = 5
        ),
        Achievement(
            id = "level_50",
            title = "Mitad del camino",
            description = "Alcanzaste el nivel 50",
            emoji = "🏅",
            metric = AchievementMetric.LEVEL,
            goal = 50
        ),
        Achievement(
            id = "level_100",
            title = "Leyenda viva",
            description = "Llegaste al nivel 100, el máximo",
            emoji = "👑",
            metric = AchievementMetric.LEVEL,
            goal = 100
        )
    )

    /** Ids del catálogo actual, para saber rápido si un id guardado sigue vivo. */
    private val currentIds: Set<String> = all.map { it.id }.toSet()

    /**
     * MIGRACIÓN de los logros del catálogo antiguo (el que llegó con la primera versión
     * de la pestaña de perfil). Solo se aplica una vez, al arrancar el perfil, y es
     * conservadora: **un id antiguo solo pasa al nuevo si el nuevo es igual de fácil o
     * más fácil de cumplir**, para no regalar logros que el dueño no ha ganado.
     *
     *   - ids que siguen existiendo en el catálogo nuevo -> se quedan tal cual
     *     (first_download, first_review, first_playlist, reviews_10, playlists_5,
     *      listens_100, pinned_5)
     *   - `downloads_5` / `downloads_25` -> `first_download` (cualquier descarga implica
     *     la primera; las metas de 5 y 25 ya no existen en el catálogo nuevo)
     *   - `level_5` -> `null` (el nivel 5 de la curva vieja eran 500 XP; en la escala
     *     nueva eso es ~nivel 12 y el primer logro de nivel es el 50: no se puede
     *     convertir sin inventar. Los contadores reales de XP lo vuelven a decidir solos.)
     *
     * Nada más se toca: el id antiguo sin equivalente deja de estar en el conjunto
     * guardado (no se puede volver a ganar, así que tampoco tiene sentido conservarlo),
     * pero la XP acumulada del dueño sí se mantiene entera.
     */
    val LEGACY_ID_ALIASES: Map<String, String?> = mapOf(
        "downloads_5" to "first_download",
        "downloads_25" to "first_download",
        "level_5" to null
    )

    /** Traduce los ids guardados a ids del catálogo nuevo (ver [LEGACY_ID_ALIASES]). */
    fun migrateLegacyUnlocks(stored: Set<String>): Set<String> =
        stored.mapNotNull { id ->
            LEGACY_ID_ALIASES[id] ?: id.takeIf { currentIds.contains(it) }
        }.toSet()

    /** Cuántos logros del catálogo ya cumplen su meta con [counters]. */
    fun reachedIds(counters: Map<AchievementMetric, Int>): List<String> =
        all.filter { (counters[it.metric] ?: 0) >= it.goal }.map { it.id }

    /** Valor actual del contador de un logro (0 si todavía no se ha medido). */
    fun progressOf(achievement: Achievement, counters: Map<AchievementMetric, Int>): Int =
        counters[achievement.metric] ?: 0
}
