package com.yucsan.musica2.modelo

/**
 * Modelo de datos para una canción
 */
data class Song(
    val id: String,
    val title: String,
    val artist: String,
    val duration: Long = 0L, // en milisegundos
    val thumbnailUrl: String? = null,
    val youtubeUrl: String? = null,
    val audioUrl: String? = null // URL directa del audio (Jamendo, etc.)
) {
    companion object {
        /**
         * Crear un Song de ejemplo para testing
         */
        fun createTestSong(
            title: String = "Test Song",
            artist: String = "Test Artist",
            isDemo: Boolean = true
        ): Song {
            return Song(
                id = if (isDemo) "demo_test_${System.currentTimeMillis()}" else "jamendo_test_${System.currentTimeMillis()}",
                title = title,
                artist = artist,
                duration = 180_000, // 3 minutos
                thumbnailUrl = if (isDemo) null else "https://usercontent.jamendo.com/default/track.png",
                youtubeUrl = "https://example.com/test",
                audioUrl = if (isDemo)
                    "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3"
                else
                    "https://mp3l.jamendo.com/download/track/test/mp3"
            )
        }
    }
}

// ============================================================================
// EXTENSIONES
// ============================================================================

/**
 * Obtener la URI para reproducción
 * Prioriza audioUrl (Jamendo) sobre youtubeUrl
 */
val Song.uri: String?
    get() = when {
        // Jamendo URLs (directas de audio)
        !audioUrl.isNullOrBlank() -> audioUrl
        // Fallback a YouTube URL si está disponible
        !youtubeUrl.isNullOrBlank() -> youtubeUrl
        else -> null
    }

/**
 * Verificar si la canción es de una fuente real (no demo)
 */
val Song.isRealSource: Boolean
    get() = !id.startsWith("demo")

/**
 * Obtener el tipo de fuente de la canción
 */
val Song.sourceType: String
    get() = when {
        id.startsWith("demo") -> "Demo"
        id.startsWith("jamendo") -> "Jamendo"
        youtubeUrl?.contains("youtube.com") == true -> "YouTube"
        else -> "Unknown"
    }

/**
 * Verificar si la canción es de Jamendo
 */
val Song.isFromJamendo: Boolean
    get() = id.startsWith("jamendo_") || audioUrl?.contains("jamendo.com") == true

/**
 * Obtener duración formateada (mm:ss)
 */
fun Song.getFormattedDuration(): String {
    if (duration <= 0) return "--:--"

    val totalSeconds = duration / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60

    return "%d:%02d".format(minutes, seconds)
}

/**
 * Obtener información de debug para logging
 */
fun Song.getDebugInfo(): String {
    return buildString {
        appendLine("🎵 Song Debug Info:")
        appendLine("  ID: $id")
        appendLine("  Title: $title")
        appendLine("  Artist: $artist")
        appendLine("  Duration: ${getFormattedDuration()}")
        appendLine("  Source: $sourceType")

        // Usar substring en lugar de take() para evitar errores
        val audioUrlDisplay = audioUrl?.let { url ->
            if (url.length > 50) "${url.substring(0, 50)}..." else url
        } ?: "null"

        val youtubeUrlDisplay = youtubeUrl?.let { url ->
            if (url.length > 50) "${url.substring(0, 50)}..." else url
        } ?: "null"

        val thumbnailUrlDisplay = thumbnailUrl?.let { url ->
            if (url.length > 50) "${url.substring(0, 50)}..." else url
        } ?: "null"

        val uriDisplay = uri?.let { url ->
            if (url.length > 50) "${url.substring(0, 50)}..." else url
        } ?: "null"

        appendLine("  AudioURL: $audioUrlDisplay")
        appendLine("  YouTubeURL: $youtubeUrlDisplay")
        appendLine("  ThumbnailURL: $thumbnailUrlDisplay")
        appendLine("  PlaybackURI: $uriDisplay")
    }
}

/**
 * Crear una copia con URL de audio actualizada (útil para streams de YouTube)
 */
fun Song.withAudioUrl(newAudioUrl: String): Song {
    return this.copy(audioUrl = newAudioUrl)
}

/**
 * Verificar si la canción tiene URLs válidas para reproducir
 */
val Song.hasPlayableUrl: Boolean
    get() = !audioUrl.isNullOrBlank() || !youtubeUrl.isNullOrBlank()

/**
 * Obtener texto descriptivo de la fuente
 */
val Song.sourceDescription: String
    get() = when {
        isFromJamendo -> "Música libre de Jamendo"
        id.startsWith("demo") -> "Canción de demostración"
        youtubeUrl?.contains("youtube.com") == true -> "Video de YouTube"
        else -> "Fuente desconocida"
    }

/**
 * Validar que la canción tenga datos mínimos necesarios
 */
fun Song.isValid(): Boolean {
    return title.isNotBlank() &&
            artist.isNotBlank() &&
            hasPlayableUrl
}

/**
 * Obtener calidad estimada basada en la fuente
 */
val Song.estimatedQuality: String
    get() = when {
        isFromJamendo -> "Alta (Jamendo)"
        id.startsWith("demo") -> "Media (Demo)"
        else -> "Variable"
    }