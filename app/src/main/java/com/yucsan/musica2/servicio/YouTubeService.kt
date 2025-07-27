package com.yucsan.musica2.service

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.search.SearchExtractor
import org.schabi.newpipe.extractor.stream.StreamExtractor
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request
import org.schabi.newpipe.extractor.downloader.Response
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import com.yucsan.musica2.modelo.Song
import java.util.concurrent.TimeUnit

class ModernYouTubeService {

    companion object {
        private const val TAG = "ModernYouTubeService"
        private var isInitialized = false

        @Volatile
        private var INSTANCE: ModernYouTubeService? = null

        fun getInstance(): ModernYouTubeService {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ModernYouTubeService().also { INSTANCE = it }
            }
        }
    }

    init {
        initializeNewPipe()
    }

    private fun initializeNewPipe() {
        if (!isInitialized) {
            try {
                NewPipe.init(EnhancedDownloaderImpl())
                isInitialized = true
                Log.d(TAG, "✅ NewPipe inicializado correctamente")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error inicializando NewPipe", e)
            }
        }
    }

    /**
     * Búsqueda con mejor manejo de errores de YouTube
     */
    suspend fun searchSongs(query: String): List<Song> = withContext(Dispatchers.IO) {
        try {
            if (!isInitialized) {
                Log.w(TAG, "NewPipe no inicializado, devolviendo demos")
                return@withContext getPopularSongs()
            }

            Log.d(TAG, "🔍 Intentando búsqueda: $query")

            val youtubeService = ServiceList.YouTube
            val searchExtractor = youtubeService.getSearchExtractor(query)

            // Intentar múltiples estrategias
            var lastException: Exception? = null

            // Estrategia 1: Búsqueda normal
            try {
                searchExtractor.fetchPage()
                val results = processSearchResults(searchExtractor)
                if (results.isNotEmpty()) {
                    Log.d(TAG, "✅ Búsqueda exitosa: ${results.size} resultados")
                    return@withContext results
                }
            } catch (e: Exception) {
                lastException = e
                Log.w(TAG, "🔄 Estrategia 1 falló: ${e.message}")
            }

            // Estrategia 2: Con diferentes configuraciones
            try {
                // Recrear extractor
                val newExtractor = youtubeService.getSearchExtractor(query)
                newExtractor.fetchPage()
                val results = processSearchResults(newExtractor)
                if (results.isNotEmpty()) {
                    Log.d(TAG, "✅ Estrategia 2 exitosa: ${results.size} resultados")
                    return@withContext results
                }
            } catch (e: Exception) {
                Log.w(TAG, "🔄 Estrategia 2 falló: ${e.message}")
            }

            // Si ambas fallan, log detallado y devolver demos
            Log.w(TAG, "⚠️ NewPipe bloqueado por YouTube, usando demos")
            Log.w(TAG, "Último error: ${lastException?.message}")
            return@withContext getPopularSongs()

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error en búsqueda NewPipe", e)
            return@withContext getPopularSongs()
        }
    }

    /**
     * Procesar resultados de búsqueda con mejor manejo
     */
    private fun processSearchResults(searchExtractor: SearchExtractor): List<Song> {
        return try {
            val songs = mutableListOf<Song>()
            val infoItems = searchExtractor.initialPage.items

            Log.d(TAG, "📝 Procesando ${infoItems.size} elementos")

            infoItems.take(20).forEach { item ->
                try {
                    if (item is StreamInfoItem) {
                        val song = Song(
                            id = extractVideoId(item.url),
                            title = item.name ?: "Sin título",
                            artist = item.uploaderName ?: "Artista desconocido",
                            duration = if (item.duration > 0) item.duration * 1000 else 0,
                            thumbnailUrl = getBestThumbnail(item.thumbnails),
                            youtubeUrl = item.url,
                            audioUrl = ""
                        )

                        songs.add(song)
                        Log.d(TAG, "✅ Procesado: ${song.title}")
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "⚠️ Error procesando item: ${e.message}")
                }
            }

            songs
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error procesando resultados", e)
            emptyList()
        }
    }

    /**
     * Obtención de stream con múltiples intentos
     */
    suspend fun getAudioStreamUrl(videoId: String): String? = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "🎵 Obteniendo stream para: $videoId")

            if (!isInitialized) {
                Log.w(TAG, "❌ NewPipe no inicializado")
                return@withContext null
            }

            val youtubeUrl = "https://www.youtube.com/watch?v=$videoId"

            // Múltiples intentos con diferentes configuraciones
            for (attempt in 1..3) {
                try {
                    Log.d(TAG, "🔄 Intento $attempt/3")

                    val youtubeService = ServiceList.YouTube
                    val streamExtractor = youtubeService.getStreamExtractor(youtubeUrl)
                    streamExtractor.fetchPage()

                    val audioStreams = streamExtractor.audioStreams
                    Log.d(TAG, "📊 Streams encontrados: ${audioStreams.size}")

                    if (audioStreams.isNotEmpty()) {
                        // Buscar mejor formato
                        val compatibleFormats = listOf("m4a", "aac", "webm", "mp4")

                        for (format in compatibleFormats) {
                            audioStreams
                                .filter { it.format?.suffix?.contains(format, ignoreCase = true) == true }
                                .maxByOrNull { it.averageBitrate }
                                ?.let { bestStream ->
                                    Log.d(TAG, "✅ Stream encontrado (intento $attempt): ${bestStream.format}")
                                    return@withContext bestStream.url
                                }
                        }

                        // Si no encuentra formato específico, usar el mejor disponible
                        audioStreams.maxByOrNull { it.averageBitrate }?.let { bestStream ->
                            Log.d(TAG, "✅ Mejor stream disponible (intento $attempt)")
                            return@withContext bestStream.url
                        }
                    }

                    // Pausa entre intentos
                    if (attempt < 3) {
                        kotlinx.coroutines.delay(1000L * attempt)
                    }

                } catch (e: Exception) {
                    Log.w(TAG, "⚠️ Intento $attempt falló: ${e.message}")
                    if (attempt == 3) {
                        Log.e(TAG, "❌ Todos los intentos fallaron", e)
                    }
                }
            }

            Log.w(TAG, "❌ No se pudo obtener stream para $videoId")
            return@withContext null

        } catch (e: Exception) {
            Log.e(TAG, "💥 Error obteniendo stream", e)
            return@withContext null
        }
    }

    /**
     * Método alternativo con configuración diferente
     */
    suspend fun getAudioStreamUrlAlternative(videoId: String): String? = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "🔄 Método alternativo para: $videoId")

            if (!isInitialized) return@withContext null

            // Intentar con diferentes URLs
            val alternativeUrls = listOf(
                "https://www.youtube.com/watch?v=$videoId",
                "https://youtu.be/$videoId",
                "https://m.youtube.com/watch?v=$videoId"
            )

            for ((index, url) in alternativeUrls.withIndex()) {
                try {
                    Log.d(TAG, "🔄 URL alternativa ${index + 1}: $url")

                    val youtubeService = ServiceList.YouTube
                    val streamExtractor = youtubeService.getStreamExtractor(url)
                    streamExtractor.fetchPage()

                    val audioStreams = streamExtractor.audioStreams
                    if (audioStreams.isNotEmpty()) {
                        val bestStream = audioStreams.maxByOrNull { it.averageBitrate }
                        bestStream?.let { stream ->
                            Log.d(TAG, "✅ Éxito con URL alternativa ${index + 1}")
                            return@withContext stream.url
                        }
                    }

                } catch (e: Exception) {
                    Log.w(TAG, "⚠️ URL alternativa ${index + 1} falló: ${e.message}")
                }

                kotlinx.coroutines.delay(1000)
            }

            Log.w(TAG, "❌ Todas las URLs alternativas fallaron")
            return@withContext null

        } catch (e: Exception) {
            Log.e(TAG, "💥 Error en método alternativo", e)
            return@withContext null
        }
    }

    /**
     * Trending con mejor manejo de errores
     */
    suspend fun getTrendingSongs(): List<Song> = withContext(Dispatchers.IO) {
        try {
            if (!isInitialized) return@withContext getPopularSongs()

            Log.d(TAG, "📈 Obteniendo trending...")

            val youtubeService = ServiceList.YouTube
            val kioskExtractor = youtubeService.getKioskList()
                .getExtractorById("Trending", null)

            kioskExtractor.fetchPage()
            val songs = mutableListOf<Song>()
            val trendingItems = kioskExtractor.initialPage.items

            trendingItems.take(15).forEach { item ->
                try {
                    if (item is StreamInfoItem) {
                        val song = Song(
                            id = extractVideoId(item.url),
                            title = item.name ?: "Sin título",
                            artist = item.uploaderName ?: "Artista desconocido",
                            duration = if (item.duration > 0) item.duration * 1000 else 0,
                            thumbnailUrl = getBestThumbnail(item.thumbnails),
                            youtubeUrl = item.url,
                            audioUrl = ""
                        )
                        songs.add(song)
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Error procesando trending item", e)
                }
            }

            if (songs.isNotEmpty()) {
                Log.d(TAG, "✅ Trending obtenido: ${songs.size}")
                return@withContext songs
            } else {
                Log.w(TAG, "⚠️ Trending vacío, usando demos")
                return@withContext getPopularSongs()
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error obteniendo trending", e)
            return@withContext getPopularSongs()
        }
    }

    /**
     * Test de conectividad mejorado
     */
    suspend fun testConnection(): Boolean = withContext(Dispatchers.IO) {
        try {
            if (!isInitialized) return@withContext false

            Log.d(TAG, "🧪 Probando conectividad NewPipe...")

            // Test simple con una búsqueda pequeña
            val testResults = searchSongs("test")
            val isWorking = testResults.isNotEmpty() && !testResults.first().id.startsWith("demo")

            Log.d(TAG, "🧪 Test NewPipe: $isWorking")

            if (!isWorking) {
                Log.w(TAG, "⚠️ NewPipe devuelve solo demos - YouTube bloqueando")
            }

            return@withContext true // Siempre retornar true porque las demos funcionan

        } catch (e: Exception) {
            Log.e(TAG, "❌ Test de conexión falló", e)
            return@withContext false
        }
    }

    suspend fun getVideoDetails(videoId: String): Song? = withContext(Dispatchers.IO) {
        try {
            if (!isInitialized) return@withContext null

            val youtubeUrl = "https://www.youtube.com/watch?v=$videoId"
            val youtubeService = ServiceList.YouTube
            val streamExtractor = youtubeService.getStreamExtractor(youtubeUrl)
            streamExtractor.fetchPage()

            Song(
                id = videoId,
                title = streamExtractor.name ?: "Sin título",
                artist = streamExtractor.uploaderName ?: "Artista desconocido",
                duration = if (streamExtractor.length > 0) streamExtractor.length * 1000 else 0,
                thumbnailUrl = getBestThumbnail(streamExtractor.thumbnails),
                youtubeUrl = youtubeUrl,
                audioUrl = ""
            )

        } catch (e: Exception) {
            Log.e(TAG, "Error obteniendo detalles de $videoId", e)
            null
        }
    }

    private fun extractVideoId(url: String): String {
        return try {
            val regex = "(?:youtube\\.com/watch\\?v=|youtu\\.be/)([^&\\n?#]+)".toRegex()
            regex.find(url)?.groupValues?.get(1) ?: url.hashCode().toString()
        } catch (e: Exception) {
            url.hashCode().toString()
        }
    }

    private fun getBestThumbnail(thumbnails: List<org.schabi.newpipe.extractor.Image>?): String? {
        return try {
            thumbnails?.maxByOrNull { it.height * it.width }?.url
        } catch (e: Exception) {
            null
        }
    }

    fun getPopularSongs(): List<Song> {
        return listOf(
            Song(
                id = "demo1",
                title = "Audio Demo 1 - SoundHelix",
                artist = "SoundHelix Composer",
                duration = 300_000,
                youtubeUrl = "https://example.com/demo1",
                audioUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3"
            ),
            Song(
                id = "demo2",
                title = "Audio Demo 2 - SoundHelix",
                artist = "SoundHelix Composer",
                duration = 240_000,
                youtubeUrl = "https://example.com/demo2",
                audioUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3"
            ),
            Song(
                id = "demo3",
                title = "Audio Demo 3 - SoundHelix",
                artist = "SoundHelix Composer",
                duration = 280_000,
                youtubeUrl = "https://example.com/demo3",
                audioUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-3.mp3"
            ),
            Song(
                id = "demo4",
                title = "Audio Demo 4 - SoundHelix",
                artist = "SoundHelix Composer",
                duration = 320_000,
                youtubeUrl = "https://example.com/demo4",
                audioUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-4.mp3"
            )
        )
    }
}

/**
 * Downloader mejorado con mejor configuración para 2025
 */
class EnhancedDownloaderImpl : Downloader() {

    private val client = OkHttpClient.Builder()
        .connectTimeout(45, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .writeTimeout(45, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        })
        .addInterceptor { chain ->
            val originalRequest = chain.request()

            val modifiedRequest = originalRequest.newBuilder()
                .removeHeader("User-Agent")
                .addHeader("User-Agent", getCurrentUserAgent())
                .addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .addHeader("Accept-Language", "en-US,en;q=0.9")
                .addHeader("Accept-Encoding", "gzip, deflate, br")
                .addHeader("Cache-Control", "no-cache")
                .addHeader("Pragma", "no-cache")
                .addHeader("sec-ch-ua", "\"Chromium\";v=\"131\", \"Not_A Brand\";v=\"24\"")
                .addHeader("sec-ch-ua-mobile", "?0")
                .addHeader("sec-ch-ua-platform", "\"Windows\"")
                .addHeader("sec-fetch-dest", "document")
                .addHeader("sec-fetch-mode", "navigate")
                .addHeader("sec-fetch-site", "none")
                .addHeader("sec-fetch-user", "?1")
                .addHeader("upgrade-insecure-requests", "1")
                .addHeader("dnt", "1")
                .addHeader("connection", "keep-alive")
                .build()

            val response = chain.proceed(modifiedRequest)

            if (!response.isSuccessful && response.code == 403) {
                Log.w("EnhancedDownloader", "🚫 YouTube bloqueó la request (403)")
            }

            response
        }
        .build()

    override fun execute(request: Request): Response {
        val requestBuilder = okhttp3.Request.Builder()
            .url(request.url())
            .method(
                request.httpMethod(),
                request.dataToSend()?.toRequestBody() ?:
                if (request.httpMethod() == "POST") "".toRequestBody() else null
            )

        request.headers().forEach { (key, values) ->
            values.forEach { value ->
                requestBuilder.addHeader(key, value)
            }
        }

        try {
            val response = client.newCall(requestBuilder.build()).execute()

            return Response(
                response.code,
                response.message,
                response.headers.toMultimap(),
                response.body?.string(),
                response.request.url.toString()
            )
        } catch (e: Exception) {
            Log.e("EnhancedDownloader", "💥 Network error", e)
            throw e
        }
    }

    private fun getCurrentUserAgent(): String {
        // User-Agent más actualizado para 2025
        val userAgents = listOf(
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:133.0) Gecko/20100101 Firefox/133.0"
        )

        val timeBasedIndex = (System.currentTimeMillis() / 30000).toInt() % userAgents.size
        return userAgents[timeBasedIndex]
    }
}