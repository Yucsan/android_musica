package com.yucsan.musica2.config

import com.yucsan.musica2.BuildConfig

/**
 * Configuración centralizada para Jamendo API - VERSIÓN SEGURA
 */
object JamendoConfig {

    // ============================================================================
    // API CONFIGURATION - AHORA SEGURA
    // ============================================================================

    const val BASE_URL = "https://api.jamendo.com/v3.0/"

    // ✅ API KEY AHORA VIENE DE BuildConfig (definida en build.gradle)
    val API_KEY: String = BuildConfig.JAMENDO_API_KEY

    init {
        // Debug logging para verificar que se lea correctamente
        android.util.Log.d("JamendoConfig", "🔧 INIT - API Key desde BuildConfig: '$API_KEY'")
        android.util.Log.d("JamendoConfig", "🔧 INIT - API Key length: ${API_KEY.length}")
        android.util.Log.d("JamendoConfig", "🔧 INIT - BuildConfig class: ${BuildConfig.JAMENDO_API_KEY}")
    }

    // Límites de resultados
    const val DEFAULT_SEARCH_LIMIT = 20
    const val DEFAULT_GENRE_LIMIT = 15
    const val DEFAULT_FEATURED_LIMIT = 15

    // Timeouts de red
    const val CONNECT_TIMEOUT_SECONDS = 30L
    const val READ_TIMEOUT_SECONDS = 30L

    // ============================================================================
    // GÉNEROS POPULARES
    // ============================================================================

    val POPULAR_GENRES = listOf(
        "rock",
        "pop",
        "electronic",
        "jazz",
        "classical",
        "blues",
        "folk",
        "ambient",
        "experimental",
        "world",
        "indie",
        "metal",
        "acoustic",
        "instrumental",
        "chill",
        "funk",
        "reggae",
        "house",
        "techno",
        "dnb" // drum and bass
    )

    // ============================================================================
    // ENDPOINTS
    // ============================================================================

    object Endpoints {
        const val TRACKS = "tracks/"
        const val ALBUMS = "albums/"
        const val ARTISTS = "artists/"
        const val PLAYLISTS = "playlists/"
        const val RADIO = "radios/"
    }

    // ============================================================================
    // PARÁMETROS COMUNES
    // ============================================================================

    object Params {
        const val CLIENT_ID = "client_id"
        const val FORMAT = "format"
        const val LIMIT = "limit"
        const val SEARCH = "search"
        const val INCLUDE = "include"
        const val ORDER = "order"
        const val FUZZYTAGS = "fuzzytags"
        const val FEATURED = "featured"
        const val GROUPBY = "groupby"
    }

    // ============================================================================
    // VALORES POR DEFECTO
    // ============================================================================

    object Defaults {
        const val FORMAT_JSON = "json"
        const val INCLUDE_MUSICINFO = "musicinfo"
        const val ORDER_POPULARITY = "popularity_total"
        const val ORDER_RELEVANCE = "relevance"
        const val GROUPBY_ARTIST = "artist_id"
    }

    // ============================================================================
    // URLS DE FALLBACK PARA DEMOS
    // ============================================================================

    val DEMO_AUDIO_URLS = listOf(
        "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3",
        "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3",
        "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-3.mp3",
        "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-4.mp3",
        "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-5.mp3"
    )

    // ============================================================================
    // FUNCIONES UTILITARIAS
    // ============================================================================

    /**
     * Construir URL de búsqueda completa
     */
    fun buildSearchUrl(
        query: String,
        limit: Int = DEFAULT_SEARCH_LIMIT,
        includeInfo: Boolean = true
    ): String {
        return buildString {
            append(BASE_URL)
            append(Endpoints.TRACKS)
            append("?${Params.CLIENT_ID}=$API_KEY")
            append("&${Params.FORMAT}=${Defaults.FORMAT_JSON}")
            append("&${Params.LIMIT}=$limit")
            append("&${Params.SEARCH}=$query")
            if (includeInfo) {
                append("&${Params.INCLUDE}=${Defaults.INCLUDE_MUSICINFO}")
            }
            append("&${Params.GROUPBY}=${Defaults.GROUPBY_ARTIST}")
        }
    }

    /**
     * Construir URL de búsqueda por género
     */
    fun buildGenreUrl(
        genre: String,
        limit: Int = DEFAULT_GENRE_LIMIT,
        includeInfo: Boolean = true
    ): String {
        return buildString {
            append(BASE_URL)
            append(Endpoints.TRACKS)
            append("?${Params.CLIENT_ID}=$API_KEY")
            append("&${Params.FORMAT}=${Defaults.FORMAT_JSON}")
            append("&${Params.LIMIT}=$limit")
            append("&${Params.FUZZYTAGS}=$genre")
            if (includeInfo) {
                append("&${Params.INCLUDE}=${Defaults.INCLUDE_MUSICINFO}")
            }
            append("&${Params.ORDER}=${Defaults.ORDER_POPULARITY}")
        }
    }

    /**
     * Construir URL de música destacada
     */
    fun buildFeaturedUrl(
        limit: Int = DEFAULT_FEATURED_LIMIT,
        includeInfo: Boolean = true
    ): String {
        return buildString {
            append(BASE_URL)
            append(Endpoints.TRACKS)
            append("?${Params.CLIENT_ID}=$API_KEY")
            append("&${Params.FORMAT}=${Defaults.FORMAT_JSON}")
            append("&${Params.LIMIT}=$limit")
            append("&${Params.FEATURED}=1")
            if (includeInfo) {
                append("&${Params.INCLUDE}=${Defaults.INCLUDE_MUSICINFO}")
            }
            append("&${Params.ORDER}=${Defaults.ORDER_POPULARITY}")
        }
    }

    /**
     * Verificar si la API key está configurada
     */
    fun isApiKeyConfigured(): Boolean {
        return API_KEY.isNotBlank() &&
                API_KEY != "YOUR_API_KEY_HERE" &&
                API_KEY != "TU_API_KEY_AQUI" &&
                API_KEY.length > 5 // Una API key real debería ser más larga
    }

    /**
     * Obtener URL de imagen mejorada
     */
    fun getImprovedImageUrl(originalUrl: String?): String {
        return originalUrl
            ?.replace("1.100.jpg", "1.500.jpg") // Imagen más grande
            ?: "https://usercontent.jamendo.com/default/track.png"
    }

    /**
     * Validar respuesta de API
     */
    fun isValidApiResponse(responseBody: String?): Boolean {
        return !responseBody.isNullOrBlank() &&
                responseBody.contains("\"status\":\"success\"")
    }

    /**
     * Generar ID único para demos
     */
    fun generateDemoId(): String {
        return "demo_${System.currentTimeMillis()}_${(1000..9999).random()}"
    }

    /**
     * Formatear duración desde segundos a milisegundos
     */
    fun formatDurationToMillis(durationSeconds: Int): Long {
        return durationSeconds * 1000L
    }

    /**
     * Limpiar texto de búsqueda
     */
    fun cleanSearchQuery(query: String): String {
        return query.trim()
            .replace(Regex("[^a-zA-Z0-9\\s]"), "") // Remover caracteres especiales
            .replace(Regex("\\s+"), " ") // Normalizar espacios
            .take(100) // Limitar longitud
    }

    /**
     * Obtener mensaje de error amigable
     */
    fun getFriendlyErrorMessage(error: Throwable): String {
        return when {
            error.message?.contains("timeout", ignoreCase = true) == true ->
                "Conexión lenta. Intenta de nuevo."
            error.message?.contains("network", ignoreCase = true) == true ->
                "Sin conexión a internet"
            error.message?.contains("404", ignoreCase = true) == true ->
                "Servicio no disponible temporalmente"
            error.message?.contains("401", ignoreCase = true) == true ->
                "API key inválida"
            else -> "Error cargando música: ${error.message ?: "Desconocido"}"
        }
    }

    /**
     * Log seguro de la API key (solo muestra los primeros caracteres)
     */
    fun getApiKeyForLogging(): String {
        return if (API_KEY.length > 4) {
            "${API_KEY.take(4)}***"
        } else {
            "***"
        }
    }
}

/**
 * Clase para estadísticas de uso de Jamendo
 */
data class JamendoStats(
    val totalSearches: Int = 0,
    val totalSongsLoaded: Int = 0,
    val popularGenres: List<String> = emptyList(),
    val averageResponseTime: Long = 0L,
    val lastSuccessfulConnection: Long = 0L,
    val apiKeyStatus: Boolean = false
) {
    fun getFormattedStats(): String {
        return buildString {
            appendLine("📊 Estadísticas Jamendo:")
            appendLine("• Búsquedas realizadas: $totalSearches")
            appendLine("• Canciones cargadas: $totalSongsLoaded")
            appendLine("• Tiempo respuesta promedio: ${averageResponseTime}ms")
            appendLine("• API Key configurada: ${if (apiKeyStatus) "✅" else "❌"}")
            appendLine("• API Key: ${JamendoConfig.getApiKeyForLogging()}")
            if (popularGenres.isNotEmpty()) {
                appendLine("• Géneros populares: ${popularGenres.take(3).joinToString(", ")}")
            }
        }
    }
}