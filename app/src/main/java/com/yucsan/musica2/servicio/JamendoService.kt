package com.yucsan.musica2.service

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.yucsan.musica2.modelo.Song
import com.yucsan.musica2.config.JamendoConfig
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import kotlinx.serialization.json.*
import java.util.concurrent.TimeUnit

/**
 * Servicio para música libre usando Jamendo API - VERSIÓN SEGURA
 * Música 100% legal y gratuita
 */
class JamendoService {

    companion object {
        private const val TAG = "JamendoService"

        @Volatile
        private var INSTANCE: JamendoService? = null

        fun getInstance(): JamendoService {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: JamendoService().also { INSTANCE = it }
            }
        }
    }

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(JamendoConfig.CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(JamendoConfig.READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        })
        .build()

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    /**
     * Buscar música en Jamendo
     */
    suspend fun searchTracks(query: String): List<Song> = withContext(Dispatchers.IO) {
        try {
            if (!JamendoConfig.isApiKeyConfigured()) {
                Log.w(TAG, "⚠️ API Key no configurada, usando demos")
                return@withContext getDemoSongs()
            }

            Log.d(TAG, "🔍 Búsqueda Jamendo: $query")
            Log.d(TAG, "🔑 API Key: ${JamendoConfig.getApiKeyForLogging()}")

            val cleanQuery = JamendoConfig.cleanSearchQuery(query)
            val url = JamendoConfig.buildSearchUrl(cleanQuery)

            val request = Request.Builder()
                .url(url)
                .addHeader("Accept", "application/json")
                .addHeader("User-Agent", "MusicApp/1.0")
                .build()

            val response = httpClient.newCall(request).execute()

            if (!response.isSuccessful) {
                Log.e(TAG, "❌ Error HTTP: ${response.code}")
                return@withContext getDemoSongs()
            }

            val responseBody = response.body?.string()
            if (responseBody.isNullOrBlank()) {
                Log.w(TAG, "⚠️ Respuesta vacía")
                return@withContext getDemoSongs()
            }

            return@withContext parseTracksResponse(responseBody)

        } catch (e: Exception) {
            Log.e(TAG, "💥 Error en búsqueda", e)
            return@withContext getDemoSongs()
        }
    }

    /**
     * Obtener música por género
     */
    suspend fun getTracksByGenre(genre: String): List<Song> = withContext(Dispatchers.IO) {
        try {
            if (!JamendoConfig.isApiKeyConfigured()) {
                return@withContext getDemoSongs()
            }

            Log.d(TAG, "🎵 Género: $genre")

            val url = JamendoConfig.buildGenreUrl(genre)

            val request = Request.Builder()
                .url(url)
                .addHeader("Accept", "application/json")
                .addHeader("User-Agent", "MusicApp/1.0")
                .build()

            val response = httpClient.newCall(request).execute()

            if (!response.isSuccessful) {
                Log.e(TAG, "❌ Error HTTP género: ${response.code}")
                return@withContext getDemoSongs()
            }

            val responseBody = response.body?.string()
            if (responseBody.isNullOrBlank()) {
                return@withContext getDemoSongs()
            }

            return@withContext parseTracksResponse(responseBody)

        } catch (e: Exception) {
            Log.e(TAG, "💥 Error obteniendo género", e)
            return@withContext getDemoSongs()
        }
    }

    /**
     * Obtener música popular/featured
     */
    suspend fun getFeaturedTracks(): List<Song> = withContext(Dispatchers.IO) {
        try {
            if (!JamendoConfig.isApiKeyConfigured()) {
                return@withContext getDemoSongs()
            }

            Log.d(TAG, "⭐ Featured tracks")

            val url = JamendoConfig.buildFeaturedUrl()

            val request = Request.Builder()
                .url(url)
                .addHeader("Accept", "application/json")
                .addHeader("User-Agent", "MusicApp/1.0")
                .build()

            val response = httpClient.newCall(request).execute()

            if (!response.isSuccessful) {
                Log.e(TAG, "❌ Error HTTP featured: ${response.code}")
                return@withContext getDemoSongs()
            }

            val responseBody = response.body?.string()
            if (responseBody.isNullOrBlank()) {
                return@withContext getDemoSongs()
            }

            return@withContext parseTracksResponse(responseBody)

        } catch (e: Exception) {
            Log.e(TAG, "💥 Error obteniendo featured", e)
            return@withContext getDemoSongs()
        }
    }

    /**
     * Obtener géneros disponibles
     */
    suspend fun getAvailableGenres(): List<String> = withContext(Dispatchers.IO) {
        try {
            return@withContext JamendoConfig.POPULAR_GENRES
        } catch (e: Exception) {
            Log.e(TAG, "Error obteniendo géneros", e)
            return@withContext listOf("rock", "pop", "electronic")
        }
    }

    /**
     * Parsear respuesta de tracks
     */
    private fun parseTracksResponse(responseBody: String): List<Song> {
        return try {
            if (!JamendoConfig.isValidApiResponse(responseBody)) {
                Log.e(TAG, "❌ Respuesta de API inválida")
                return getDemoSongs()
            }

            val jsonElement = json.parseToJsonElement(responseBody)
            val jsonObject = jsonElement.jsonObject

            // Verificar status
            val headers = jsonObject["headers"]?.jsonObject
            val status = headers?.get("status")?.jsonPrimitive?.content

            if (status != "success") {
                Log.e(TAG, "❌ API Status: $status")
                return getDemoSongs()
            }

            val results = jsonObject["results"]?.jsonArray ?: return getDemoSongs()

            val songs = mutableListOf<Song>()

            results.forEach { item ->
                try {
                    val track = item.jsonObject

                    val id = track["id"]?.jsonPrimitive?.content ?: return@forEach
                    val name = track["name"]?.jsonPrimitive?.content ?: "Sin título"
                    val artistName = track["artist_name"]?.jsonPrimitive?.content ?: "Artista desconocido"
                    val duration = track["duration"]?.jsonPrimitive?.intOrNull ?: 0
                    val audioUrl = track["audio"]?.jsonPrimitive?.content ?: return@forEach
                    val albumImage = track["album_image"]?.jsonPrimitive?.content

                    // URL de imagen mejorada
                    val imageUrl = JamendoConfig.getImprovedImageUrl(albumImage)

                    val song = Song(
                        id = "jamendo_$id",
                        title = name,
                        artist = artistName,
                        duration = JamendoConfig.formatDurationToMillis(duration),
                        thumbnailUrl = imageUrl,
                        youtubeUrl = "https://www.jamendo.com/track/$id",
                        audioUrl = audioUrl
                    )

                    songs.add(song)
                    Log.d(TAG, "✅ Track parseado: $name - $artistName")

                } catch (e: Exception) {
                    Log.w(TAG, "⚠️ Error parseando track individual", e)
                }
            }

            if (songs.isNotEmpty()) {
                Log.d(TAG, "✅ Jamendo: ${songs.size} canciones parseadas")
                songs
            } else {
                Log.w(TAG, "⚠️ Sin canciones válidas, usando demos")
                getDemoSongs()
            }

        } catch (e: Exception) {
            Log.e(TAG, "💥 Error parseando respuesta", e)
            getDemoSongs()
        }
    }

    /**
     * Test de conectividad básico
     */
    suspend fun testConnection(): Boolean = withContext(Dispatchers.IO) {
        try {
            if (!JamendoConfig.isApiKeyConfigured()) {
                Log.w(TAG, "🧪 Test: API Key no configurada")
                return@withContext false
            }

            Log.d(TAG, "🧪 Test de conexión Jamendo con API Key: ${JamendoConfig.getApiKeyForLogging()}")

            val results = searchTracks("test")
            val isWorking = results.isNotEmpty() && !results.first().id.startsWith("demo")

            Log.d(TAG, "🧪 Test Jamendo: $isWorking")
            return@withContext isWorking

        } catch (e: Exception) {
            Log.e(TAG, "Error en test", e)
            return@withContext false
        }
    }

    /**
     * Test simple con la API key hardcodeada para comparar
     */
    suspend fun testWithHardcodedKey(): String = withContext(Dispatchers.IO) {
        val debugInfo = StringBuilder()

        try {
            debugInfo.appendLine("🧪 TEST COMPARATIVO:")
            debugInfo.appendLine("═════════════════════")

            // Test con la key actual
            debugInfo.appendLine("1. API Key desde BuildConfig: '${JamendoConfig.API_KEY}'")
            debugInfo.appendLine("2. API Key length: ${JamendoConfig.API_KEY.length}")
            debugInfo.appendLine("3. API Key configurada: ${JamendoConfig.isApiKeyConfigured()}")

            // Test directo con la key que sabemos que funcionaba
            val hardcodedKey = "c6808c99"
            debugInfo.appendLine("4. Hardcoded key: '$hardcodedKey'")
            debugInfo.appendLine("5. Keys son iguales: ${JamendoConfig.API_KEY == hardcodedKey}")

            // Test de URL
            val testUrl = buildString {
                append("https://api.jamendo.com/v3.0/tracks/")
                append("?client_id=$hardcodedKey")
                append("&format=json")
                append("&limit=5")
                append("&search=piano")
                append("&include=musicinfo")
                append("&groupby=artist_id")
            }

            debugInfo.appendLine("6. URL de test directo:")
            debugInfo.appendLine("   ${testUrl}")

            // Hacer request directo
            val request = Request.Builder()
                .url(testUrl)
                .addHeader("Accept", "application/json")
                .addHeader("User-Agent", "MusicApp/1.0")
                .build()

            val response = httpClient.newCall(request).execute()
            debugInfo.appendLine("7. Response directo código: ${response.code}")
            debugInfo.appendLine("8. Response directo exitoso: ${response.isSuccessful}")

            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                debugInfo.appendLine("9. Response body length: ${responseBody?.length ?: 0}")

                if (responseBody?.isNotBlank() == true) {
                    val containsSuccess = responseBody.contains("\"status\":\"success\"")
                    debugInfo.appendLine("10. Contains success: $containsSuccess")

                    if (containsSuccess) {
                        debugInfo.appendLine("✅ HARDCODED KEY FUNCIONA!")
                    } else {
                        debugInfo.appendLine("❌ Hardcoded key también falla")
                        debugInfo.appendLine("    Response preview: ${responseBody.take(200)}")
                    }
                } else {
                    debugInfo.appendLine("❌ Response vacía")
                }
            } else {
                debugInfo.appendLine("❌ Request falló: ${response.message}")
            }

        } catch (e: Exception) {
            debugInfo.appendLine("💥 ERROR: ${e.message}")
        }

        return@withContext debugInfo.toString()
    }

    /**
     * Test de conectividad con debugging detallado
     */
    suspend fun debugConnection(): String = withContext(Dispatchers.IO) {
        val debugInfo = StringBuilder()

        try {
            debugInfo.appendLine("🔧 DEBUG JAMENDO CONNECTION:")
            debugInfo.appendLine("═══════════════════════════════")

            // 1. Verificar API Key
            val apiKeyConfigured = JamendoConfig.isApiKeyConfigured()
            debugInfo.appendLine("1. API Key configurada: $apiKeyConfigured")
            debugInfo.appendLine("   API Key preview: ${JamendoConfig.getApiKeyForLogging()}")
            debugInfo.appendLine("   API Key length: ${JamendoConfig.API_KEY.length}")

            if (!apiKeyConfigured) {
                debugInfo.appendLine("❌ PROBLEMA: API Key no válida")
                return@withContext debugInfo.toString()
            }

            // 2. Construir URL de test
            val testQuery = "piano"
            val testUrl = JamendoConfig.buildSearchUrl(testQuery, limit = 5)
            debugInfo.appendLine("2. URL de test: ${testUrl.take(100)}...")

            // 3. Hacer request de test
            debugInfo.appendLine("3. Haciendo request...")
            val startTime = System.currentTimeMillis()

            val request = Request.Builder()
                .url(testUrl)
                .addHeader("Accept", "application/json")
                .addHeader("User-Agent", "MusicApp/1.0")
                .build()

            val response = httpClient.newCall(request).execute()
            val endTime = System.currentTimeMillis()
            val responseTime = endTime - startTime

            debugInfo.appendLine("4. Response tiempo: ${responseTime}ms")
            debugInfo.appendLine("5. Response código: ${response.code}")
            debugInfo.appendLine("6. Response exitoso: ${response.isSuccessful}")

            if (!response.isSuccessful) {
                debugInfo.appendLine("❌ PROBLEMA: HTTP ${response.code} - ${response.message}")
                return@withContext debugInfo.toString()
            }

            // 4. Leer respuesta
            val responseBody = response.body?.string()
            debugInfo.appendLine("7. Response body length: ${responseBody?.length ?: 0}")

            if (responseBody.isNullOrBlank()) {
                debugInfo.appendLine("❌ PROBLEMA: Respuesta vacía")
                return@withContext debugInfo.toString()
            }

            // 5. Verificar JSON
            val isValidJson = JamendoConfig.isValidApiResponse(responseBody)
            debugInfo.appendLine("8. JSON válido: $isValidJson")

            if (isValidJson) {
                // Parsear para obtener detalles
                try {
                    val jsonElement = json.parseToJsonElement(responseBody)
                    val jsonObject = jsonElement.jsonObject

                    val headers = jsonObject["headers"]?.jsonObject
                    val status = headers?.get("status")?.jsonPrimitive?.content
                    val code = headers?.get("code")?.jsonPrimitive?.intOrNull
                    val results = jsonObject["results"]?.jsonArray

                    debugInfo.appendLine("9. API Status: $status")
                    debugInfo.appendLine("10. API Code: $code")
                    debugInfo.appendLine("11. Resultados count: ${results?.size ?: 0}")

                    if (status == "success" && results != null && results.size > 0) {
                        debugInfo.appendLine("✅ ÉXITO: Jamendo funcionando correctamente")

                        // Mostrar primer resultado
                        val firstResult = results[0].jsonObject
                        val trackName = firstResult["name"]?.jsonPrimitive?.content
                        val artistName = firstResult["artist_name"]?.jsonPrimitive?.content
                        debugInfo.appendLine("12. Primer resultado: '$trackName' por '$artistName'")

                    } else {
                        debugInfo.appendLine("❌ PROBLEMA: API response inválida")
                        debugInfo.appendLine("    Status: $status, Results: ${results?.size}")
                    }

                } catch (e: Exception) {
                    debugInfo.appendLine("❌ PROBLEMA parseando JSON: ${e.message}")
                }
            } else {
                debugInfo.appendLine("❌ PROBLEMA: Response no es JSON válido")
                debugInfo.appendLine("Primeros 200 chars: ${responseBody.take(200)}")
            }

        } catch (e: Exception) {
            debugInfo.appendLine("💥 ERROR GENERAL: ${e.message}")
            debugInfo.appendLine("Stack trace: ${e.stackTraceToString().take(500)}")
        }

        return@withContext debugInfo.toString()
    }

    /**
     * Test avanzado con estadísticas
     */
    suspend fun testConnectionWithStats(): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        return@withContext try {
            val startTime = System.currentTimeMillis()

            if (!JamendoConfig.isApiKeyConfigured()) {
                false to "❌ API Key no configurada en local.properties"
            } else {
                val results = searchTracks("piano")
                val endTime = System.currentTimeMillis()
                val responseTime = endTime - startTime

                val isWorking = results.isNotEmpty() && !results.first().id.startsWith("demo")

                val message = if (isWorking) {
                    "✅ Jamendo OK - ${results.size} resultados en ${responseTime}ms"
                } else {
                    "⚠️ Solo demos disponibles - Verificar API Key"
                }

                isWorking to message
            }
        } catch (e: Exception) {
            false to "❌ Error: ${JamendoConfig.getFriendlyErrorMessage(e)}"
        }
    }

    /**
     * Test de todos los servicios
     */
    suspend fun testAllServices(): Map<String, Boolean> = withContext(Dispatchers.IO) {
        return@withContext try {
            val results = mutableMapOf<String, Boolean>()

            // Test de conectividad básica
            results["Jamendo Connection"] = testConnection()

            // Test de búsqueda
            val searchResults = searchTracks("test music")
            results["Search Function"] = searchResults.isNotEmpty()

            // Test de géneros
            val genreResults = getTracksByGenre("rock")
            results["Genre Function"] = genreResults.isNotEmpty()

            // Test de featured
            val featuredResults = getFeaturedTracks()
            results["Featured Function"] = featuredResults.isNotEmpty()

            // Test de géneros disponibles
            val genres = getAvailableGenres()
            results["Genres Available"] = genres.isNotEmpty()

            Log.d(TAG, "🧪 Test completo realizado: $results")
            results

        } catch (e: Exception) {
            Log.e(TAG, "Error en test completo", e)
            mapOf("Error" to false)
        }
    }

    /**
     * Obtener estadísticas del servicio
     */
    fun getServiceStats(): Map<String, Any> {
        return mapOf(
            "service" to "Jamendo",
            "apiKeyConfigured" to JamendoConfig.isApiKeyConfigured(),
            "apiKeyPreview" to JamendoConfig.getApiKeyForLogging(),
            "baseUrl" to JamendoConfig.BASE_URL,
            "availableGenres" to JamendoConfig.POPULAR_GENRES.size,
            "demoUrlsAvailable" to JamendoConfig.DEMO_AUDIO_URLS.size,
            "maxSearchLimit" to JamendoConfig.DEFAULT_SEARCH_LIMIT,
            "timeoutSeconds" to JamendoConfig.CONNECT_TIMEOUT_SECONDS,
            "connectivity" to mapOf(
                "Jamendo" to JamendoConfig.isApiKeyConfigured()
            )
        )
    }

    /**
     * Limpiar recursos si es necesario
     */
    fun cleanup() {
        try {
            Log.d(TAG, "🧹 Limpiando JamendoService...")
            // No hay recursos específicos que limpiar por ahora
        } catch (e: Exception) {
            Log.e(TAG, "Error en cleanup", e)
        }
    }

    /**
     * Obtener información detallada para debugging
     */
    fun getDetailedInfo(): String {
        return buildString {
            appendLine("🎵 JamendoService Info:")
            appendLine("  API Key configurada: ${JamendoConfig.isApiKeyConfigured()}")
            appendLine("  API Key preview: ${JamendoConfig.getApiKeyForLogging()}")
            appendLine("  Base URL: ${JamendoConfig.BASE_URL}")
            appendLine("  Timeout conexión: ${JamendoConfig.CONNECT_TIMEOUT_SECONDS}s")
            appendLine("  Timeout lectura: ${JamendoConfig.READ_TIMEOUT_SECONDS}s")
            appendLine("  Límite búsqueda: ${JamendoConfig.DEFAULT_SEARCH_LIMIT}")
            appendLine("  Géneros disponibles: ${JamendoConfig.POPULAR_GENRES.size}")
            appendLine("  URLs demo: ${JamendoConfig.DEMO_AUDIO_URLS.size}")
        }
    }

    /**
     * Canciones demo como fallback
     */
    private fun getDemoSongs(): List<Song> {
        return listOf(
            Song(
                id = JamendoConfig.generateDemoId(),
                title = "Demo Song 1 - SoundHelix",
                artist = "SoundHelix",
                duration = 300_000,
                youtubeUrl = "https://example.com/demo1",
                audioUrl = JamendoConfig.DEMO_AUDIO_URLS[0],
                thumbnailUrl = null
            ),
            Song(
                id = JamendoConfig.generateDemoId(),
                title = "Demo Song 2 - SoundHelix",
                artist = "SoundHelix",
                duration = 240_000,
                youtubeUrl = "https://example.com/demo2",
                audioUrl = JamendoConfig.DEMO_AUDIO_URLS[1],
                thumbnailUrl = null
            ),
            Song(
                id = JamendoConfig.generateDemoId(),
                title = "Demo Song 3 - SoundHelix",
                artist = "SoundHelix",
                duration = 280_000,
                youtubeUrl = "https://example.com/demo3",
                audioUrl = JamendoConfig.DEMO_AUDIO_URLS[2],
                thumbnailUrl = null
            ),
            Song(
                id = JamendoConfig.generateDemoId(),
                title = "Demo Song 4 - SoundHelix",
                artist = "SoundHelix",
                duration = 320_000,
                youtubeUrl = "https://example.com/demo4",
                audioUrl = JamendoConfig.DEMO_AUDIO_URLS[3],
                thumbnailUrl = null
            ),
            Song(
                id = JamendoConfig.generateDemoId(),
                title = "Demo Song 5 - SoundHelix",
                artist = "SoundHelix",
                duration = 290_000,
                youtubeUrl = "https://example.com/demo5",
                audioUrl = JamendoConfig.DEMO_AUDIO_URLS[4],
                thumbnailUrl = null
            )
        )
    }
}