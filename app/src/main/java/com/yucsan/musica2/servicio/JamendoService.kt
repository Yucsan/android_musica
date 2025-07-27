package com.yucsan.musica2.service

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.yucsan.musica2.modelo.Song
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import kotlinx.serialization.json.*
import java.util.concurrent.TimeUnit

/**
 * Servicio para música libre usando Jamendo API
 * Música 100% legal y gratuita
 */
class JamendoService {

    companion object {
        private const val TAG = "JamendoService"
        private const val BASE_URL = "https://api.jamendo.com/v3.0/"

        // ⚠️ REEMPLAZA CON TU API KEY REAL
        private const val API_KEY = "c6808c99" // ← Pon tu API key aquí

        @Volatile
        private var INSTANCE: JamendoService? = null

        fun getInstance(): JamendoService {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: JamendoService().also { INSTANCE = it }
            }
        }
    }

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
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
            if (API_KEY == "TU_API_KEY_AQUI") {
                Log.w(TAG, "⚠️ API Key no configurada")
                return@withContext getDemoSongs()
            }

            Log.d(TAG, "🔍 Búsqueda Jamendo: $query")

            val url = buildString {
                append("${BASE_URL}tracks/")
                append("?client_id=$API_KEY")
                append("&format=json")
                append("&limit=20")
                append("&search=$query")
                append("&include=musicinfo")
                append("&groupby=artist_id") // Evitar duplicados del mismo artista
            }

            val request = Request.Builder()
                .url(url)
                .addHeader("Accept", "application/json")
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
            if (API_KEY == "TU_API_KEY_AQUI") {
                return@withContext getDemoSongs()
            }

            Log.d(TAG, "🎵 Género: $genre")

            val url = buildString {
                append("${BASE_URL}tracks/")
                append("?client_id=$API_KEY")
                append("&format=json")
                append("&limit=15")
                append("&fuzzytags=$genre")
                append("&include=musicinfo")
                append("&order=popularity_total")
            }

            val request = Request.Builder()
                .url(url)
                .addHeader("Accept", "application/json")
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
            if (API_KEY == "TU_API_KEY_AQUI") {
                return@withContext getDemoSongs()
            }

            Log.d(TAG, "⭐ Featured tracks")

            val url = buildString {
                append("${BASE_URL}tracks/")
                append("?client_id=$API_KEY")
                append("&format=json")
                append("&limit=15")
                append("&featured=1")
                append("&include=musicinfo")
                append("&order=popularity_total")
            }

            val request = Request.Builder()
                .url(url)
                .addHeader("Accept", "application/json")
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
            // Géneros populares en Jamendo
            return@withContext listOf(
                "rock", "pop", "electronic", "jazz", "classical",
                "blues", "folk", "ambient", "experimental", "world",
                "indie", "metal", "acoustic", "instrumental", "chill"
            )
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

                    // URL de imagen más grande si está disponible
                    val imageUrl = albumImage?.replace("1.100.jpg", "1.500.jpg")
                        ?: "https://usercontent.jamendo.com/default/track.png"

                    val song = Song(
                        id = "jamendo_$id",
                        title = name,
                        artist = artistName,
                        duration = duration * 1000L, // Convertir a millisegundos
                        thumbnailUrl = imageUrl,
                        youtubeUrl = "https://www.jamendo.com/track/$id",
                        audioUrl = audioUrl
                    )

                    songs.add(song)
                    Log.d(TAG, "✅ Track: $name - $artistName")

                } catch (e: Exception) {
                    Log.w(TAG, "⚠️ Error parseando track", e)
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
     * Test de conectividad
     */
    suspend fun testConnection(): Boolean = withContext(Dispatchers.IO) {
        try {
            if (API_KEY == "TU_API_KEY_AQUI") {
                Log.w(TAG, "🧪 Test: API Key no configurada")
                return@withContext false
            }

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
     * Canciones demo como fallback
     */
    private fun getDemoSongs(): List<Song> {
        return listOf(
            Song(
                id = "demo1",
                title = "Demo Song 1 - SoundHelix",
                artist = "SoundHelix",
                duration = 300_000,
                youtubeUrl = "https://example.com/demo1",
                audioUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3"
            ),
            Song(
                id = "demo2",
                title = "Demo Song 2 - SoundHelix",
                artist = "SoundHelix",
                duration = 240_000,
                youtubeUrl = "https://example.com/demo2",
                audioUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3"
            ),
            Song(
                id = "demo3",
                title = "Demo Song 3 - SoundHelix",
                artist = "SoundHelix",
                duration = 280_000,
                youtubeUrl = "https://example.com/demo3",
                audioUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-3.mp3"
            ),
            Song(
                id = "demo4",
                title = "Demo Song 4 - SoundHelix",
                artist = "SoundHelix",
                duration = 320_000,
                youtubeUrl = "https://example.com/demo4",
                audioUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-4.mp3"
            )
        )
    }
}