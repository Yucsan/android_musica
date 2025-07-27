package com.yucsan.musica2.servicio

import android.content.Context
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import com.yucsan.musica2.modelo.Song
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import com.yausername.ffmpeg.FFmpeg
import java.io.File

/**
 * YtDlpWrapper paso a paso - Versión robusta
 * Manejo cuidadoso de errores y múltiples estrategias
 */
class YtDlpWrapper private constructor(private val context: Context) {

    companion object {
        private const val TAG = "YtDlpWrapper"

        @Volatile
        private var INSTANCE: YtDlpWrapper? = null

        fun getInstance(context: Context): YtDlpWrapper {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: YtDlpWrapper(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private var isInitialized = false
    private var initializationError: String? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * PASO 1: Inicialización paso a paso con diagnóstico detallado
     */
    suspend fun initialize(): Boolean = withContext(Dispatchers.IO) {
        try {
            if (isInitialized) {
                Log.d(TAG, "✅ Ya inicializado previamente")
                return@withContext true
            }

            Log.d(TAG, "🚀 PASO 1: Iniciando inicialización yt-dlp...")

            // Verificar arquitectura del dispositivo
            val arch = System.getProperty("os.arch") ?: "unknown"
            val abi = android.os.Build.SUPPORTED_ABIS?.joinToString(", ") ?: "unknown"
            Log.d(TAG, "📱 Arquitectura: $arch, ABIs: $abi")

            // Verificar espacio de almacenamiento
            val cacheDir = context.cacheDir
            val freeSpace = cacheDir.freeSpace / (1024 * 1024) // MB
            Log.d(TAG, "💾 Espacio libre: ${freeSpace}MB")

            if (freeSpace < 50) {
                val error = "Espacio insuficiente: ${freeSpace}MB (necesario: 50MB+)"
                Log.e(TAG, "❌ $error")
                initializationError = error
                return@withContext false
            }

            // PASO 1a: Inicializar YoutubeDL
            Log.d(TAG, "🔧 PASO 1a: Inicializando YoutubeDL...")
            try {
                YoutubeDL.getInstance().init(context)
                Log.d(TAG, "✅ YoutubeDL inicializado correctamente")
            } catch (e: Exception) {
                val error = "Error inicializando YoutubeDL: ${e.message}"
                Log.e(TAG, "❌ $error", e)
                initializationError = error

                // Intentar estrategia alternativa
                Log.d(TAG, "🔄 Probando estrategia alternativa...")
                try {
                    // Limpiar cache y reintentar
                    clearYtDlpCache()
                    delay(2000)
                    YoutubeDL.getInstance().init(context)
                    Log.d(TAG, "✅ Estrategia alternativa exitosa")
                } catch (e2: Exception) {
                    Log.e(TAG, "❌ Estrategia alternativa también falló", e2)
                    return@withContext false
                }
            }

            // PASO 1b: Inicializar FFmpeg (opcional)
            Log.d(TAG, "🔧 PASO 1b: Inicializando FFmpeg...")
            try {
                FFmpeg.getInstance().init(context)
                Log.d(TAG, "✅ FFmpeg inicializado")
            } catch (e: Exception) {
                Log.w(TAG, "⚠️ FFmpeg no disponible (no crítico): ${e.message}")
                // FFmpeg no es crítico, continuar
            }

            // PASO 1c: Test básico
            Log.d(TAG, "🧪 PASO 1c: Test básico...")
            val version = getYtDlpVersion()
            Log.d(TAG, "📱 Versión yt-dlp: $version")

            if (version.contains("error", ignoreCase = true)) {
                initializationError = "Test básico falló: $version"
                Log.e(TAG, "❌ $initializationError")
                return@withContext false
            }

            isInitialized = true
            initializationError = null
            Log.d(TAG, "✅ PASO 1 COMPLETADO: yt-dlp inicializado correctamente")

            return@withContext true

        } catch (e: Exception) {
            val error = "Error general en inicialización: ${e.message}"
            Log.e(TAG, "💥 $error", e)
            initializationError = error
            isInitialized = false
            return@withContext false
        }
    }

    /**
     * PASO 2: Búsqueda con múltiples estrategias
     */
    fun searchSongsFlow(query: String): Flow<List<Song>> = flow {
        emit(emptyList())

        try {
            Log.d(TAG, "🔍 PASO 2: Búsqueda iniciada para '$query'")

            if (!isInitialized) {
                Log.d(TAG, "🔄 No inicializado, inicializando...")
                if (!initialize()) {
                    Log.e(TAG, "❌ No se pudo inicializar: $initializationError")
                    emit(emptyList())
                    return@flow
                }
            }

            val songs = searchSongsStep2(query)

            if (songs.isNotEmpty()) {
                Log.d(TAG, "✅ PASO 2 COMPLETADO: ${songs.size} canciones encontradas")
                emit(songs)
            } else {
                Log.w(TAG, "⚠️ PASO 2: Sin resultados")
                emit(emptyList())
            }

        } catch (e: Exception) {
            Log.e(TAG, "💥 Error en PASO 2", e)
            emit(emptyList())
        }
    }

    /**
     * PASO 2 interno: Múltiples estrategias de búsqueda
     */
    private suspend fun searchSongsStep2(query: String): List<Song> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "🎯 Estrategia 1: Búsqueda estándar")

            // Estrategia 1: Búsqueda estándar
            var results = searchWithStrategy("ytsearch5:$query", "estándar")
            if (results.isNotEmpty()) return@withContext results

            // Estrategia 2: Búsqueda con menos resultados
            Log.d(TAG, "🎯 Estrategia 2: Búsqueda reducida")
            results = searchWithStrategy("ytsearch3:$query", "reducida")
            if (results.isNotEmpty()) return@withContext results

            // Estrategia 3: Búsqueda simple
            Log.d(TAG, "🎯 Estrategia 3: Búsqueda simple")
            results = searchWithStrategy("ytsearch1:$query", "simple")
            if (results.isNotEmpty()) return@withContext results

            Log.w(TAG, "❌ Todas las estrategias de búsqueda fallaron")
            return@withContext emptyList()

        } catch (e: Exception) {
            Log.e(TAG, "💥 Error en searchSongsStep2", e)
            return@withContext emptyList()
        }
    }

    /**
     * Búsqueda con estrategia específica
     */
    private suspend fun searchWithStrategy(searchQuery: String, strategyName: String): List<Song> {
        return try {
            Log.d(TAG, "🔍 Ejecutando estrategia $strategyName: $searchQuery")

            val request = YoutubeDLRequest(searchQuery).apply {
                addOption("--flat-playlist")
                addOption("--no-warnings")
                addOption("--ignore-errors")
                addOption("--extract-flat")
                addOption("--get-id")
                addOption("--get-title")
                addOption("--get-uploader")
            }

            val response = withTimeoutOrNull(15000) {
                YoutubeDL.getInstance().execute(request)
            }

            if (response == null) {
                Log.w(TAG, "⏰ Timeout en estrategia $strategyName")
                return emptyList()
            }

            Log.d(TAG, "📥 Respuesta estrategia $strategyName: ${response.out?.take(100)}...")

            val songs = parseSimpleResponse(response.out ?: "")

            if (songs.isNotEmpty()) {
                Log.d(TAG, "✅ Estrategia $strategyName exitosa: ${songs.size} resultados")
            } else {
                Log.w(TAG, "⚠️ Estrategia $strategyName sin resultados")
            }

            songs

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error en estrategia $strategyName", e)
            emptyList()
        }
    }

    /**
     * PASO 3: Obtener stream de audio
     */
    suspend fun getAudioStreamUrl(videoId: String): String? = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "🎵 PASO 3: Obteniendo stream para $videoId")

            if (!isInitialized) {
                Log.d(TAG, "🔄 No inicializado, inicializando...")
                if (!initialize()) {
                    Log.e(TAG, "❌ No se pudo inicializar: $initializationError")
                    return@withContext null
                }
            }

            val url = "https://www.youtube.com/watch?v=$videoId"

            // Estrategia 1: Formato específico
            Log.d(TAG, "🎯 Estrategia 1: Formato m4a")
            var streamUrl = getStreamWithFormat(url, "bestaudio[ext=m4a]")
            if (streamUrl != null) return@withContext streamUrl

            // Estrategia 2: Mejor audio disponible
            Log.d(TAG, "🎯 Estrategia 2: Mejor audio")
            streamUrl = getStreamWithFormat(url, "bestaudio")
            if (streamUrl != null) return@withContext streamUrl

            // Estrategia 3: Cualquier audio
            Log.d(TAG, "🎯 Estrategia 3: Cualquier audio")
            streamUrl = getStreamWithFormat(url, "worst")
            if (streamUrl != null) return@withContext streamUrl

            Log.w(TAG, "❌ PASO 3: No se pudo obtener stream para $videoId")
            return@withContext null

        } catch (e: Exception) {
            Log.e(TAG, "💥 Error en PASO 3", e)
            return@withContext null
        }
    }

    /**
     * Obtener stream con formato específico
     */
    private suspend fun getStreamWithFormat(url: String, format: String): String? {
        return try {
            Log.d(TAG, "🔧 Probando formato: $format")

            val request = YoutubeDLRequest(url).apply {
                addOption("--get-url")
                addOption("--format", format)
                addOption("--no-warnings")
                addOption("--ignore-errors")
            }

            val response = withTimeoutOrNull(10000) {
                YoutubeDL.getInstance().execute(request)
            }

            if (response?.out?.isNotBlank() == true) {
                val streamUrl = response.out.lines()
                    .firstOrNull { it.trim().startsWith("http") }
                    ?.trim()

                if (streamUrl != null) {
                    Log.d(TAG, "✅ Stream obtenido con formato $format")
                    return streamUrl
                }
            }

            Log.w(TAG, "⚠️ Formato $format no disponible")
            null

        } catch (e: Exception) {
            Log.w(TAG, "❌ Error con formato $format: ${e.message}")
            null
        }
    }

    /**
     * Parsear respuesta simple
     */
    private fun parseSimpleResponse(output: String): List<Song> {
        return try {
            val lines = output.lines().filter { it.trim().isNotEmpty() }
            val songs = mutableListOf<Song>()

            // Buscar IDs de video
            val videoIds = mutableSetOf<String>()

            for (line in lines) {
                // Buscar patrones de video ID
                val patterns = listOf(
                    "([a-zA-Z0-9_-]{11})".toRegex(), // YouTube video ID pattern
                    "\"id\":\\s*\"([^\"]+)\"".toRegex(),
                    "watch\\?v=([a-zA-Z0-9_-]{11})".toRegex()
                )

                for (pattern in patterns) {
                    pattern.findAll(line).forEach { match ->
                        val id = match.groupValues[1]
                        if (id.length == 11 && id.matches("[a-zA-Z0-9_-]+".toRegex())) {
                            videoIds.add(id)
                        }
                    }
                }
            }

            // Crear canciones básicas
            videoIds.take(5).forEach { id ->
                val song = Song(
                    id = id,
                    title = "Video $id",
                    artist = "YouTube",
                    duration = 0L,
                    thumbnailUrl = "https://img.youtube.com/vi/$id/hqdefault.jpg",
                    youtubeUrl = "https://www.youtube.com/watch?v=$id",
                    audioUrl = ""
                )
                songs.add(song)
            }

            Log.d(TAG, "📝 Parseados ${songs.size} resultados básicos")
            songs

        } catch (e: Exception) {
            Log.e(TAG, "Error parseando respuesta", e)
            emptyList()
        }
    }

    /**
     * Test de conectividad paso a paso
     */
    suspend fun testConnection(): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "🧪 PASO TEST: Iniciando test de conectividad")

            if (!isInitialized) {
                Log.d(TAG, "🔄 Inicializando para test...")
                if (!initialize()) {
                    Log.e(TAG, "❌ Test falló: No se pudo inicializar")
                    return@withContext false
                }
            }

            // Test simple: obtener versión
            val version = getYtDlpVersion()
            val versionOk = !version.contains("error", ignoreCase = true)

            Log.d(TAG, "🧪 Test versión: $versionOk ($version)")

            if (!versionOk) {
                return@withContext false
            }

            // Test avanzado: búsqueda simple
            Log.d(TAG, "🧪 Test búsqueda simple...")
            val searchResults = searchSongsStep2("test")
            val searchOk = searchResults.isNotEmpty()

            Log.d(TAG, "🧪 Test búsqueda: $searchOk (${searchResults.size} resultados)")

            val overallResult = versionOk && searchOk
            Log.d(TAG, "🧪 PASO TEST COMPLETADO: $overallResult")

            return@withContext overallResult

        } catch (e: Exception) {
            Log.e(TAG, "💥 Error en test", e)
            return@withContext false
        }
    }

    /**
     * Obtener versión (test básico)
     */
    private suspend fun getYtDlpVersion(): String = withContext(Dispatchers.IO) {
        try {
            val request = YoutubeDLRequest("--version")
            val response = withTimeoutOrNull(5000) {
                YoutubeDL.getInstance().execute(request)
            }
            response?.out?.trim() ?: "Error: Sin respuesta"
        } catch (e: Exception) {
            "Error: ${e.message}"
        }
    }

    /**
     * Limpiar cache de yt-dlp
     */
    private fun clearYtDlpCache() {
        try {
            val cacheDir = File(context.cacheDir, "youtubedl-android")
            if (cacheDir.exists()) {
                cacheDir.deleteRecursively()
                Log.d(TAG, "🧹 Cache yt-dlp limpiado")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error limpiando cache", e)
        }
    }

    /**
     * Limpiar cache público
     */
    suspend fun clearCache() = withContext(Dispatchers.IO) {
        clearYtDlpCache()
    }

    /**
     * Obtener estado de diagnóstico
     */
    fun getDiagnosticInfo(): String {
        return buildString {
            appendLine("🔧 Estado YtDlpWrapper:")
            appendLine("  • Inicializado: $isInitialized")
            if (initializationError != null) {
                appendLine("  • Error: $initializationError")
            }
            appendLine("  • Cache dir: ${context.cacheDir.absolutePath}")
            appendLine("  • Free space: ${context.cacheDir.freeSpace / (1024 * 1024)}MB")
            appendLine("  • Architecture: ${System.getProperty("os.arch")}")
            appendLine("  • ABIs: ${android.os.Build.SUPPORTED_ABIS?.joinToString(", ")}")
        }
    }

    /**
     * Cleanup
     */
    fun cleanup() {
        serviceScope.cancel()
        isInitialized = false
        initializationError = null
        Log.d(TAG, "🧹 YtDlpWrapper limpiado")
    }

    /**
     * Métodos adicionales requeridos por HybridMusicService
     */
    suspend fun getTrendingVideos(): List<Song> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "📈 Obteniendo trending...")
            searchSongsStep2("trending music 2025")
        } catch (e: Exception) {
            Log.e(TAG, "Error obteniendo trending", e)
            emptyList()
        }
    }
}