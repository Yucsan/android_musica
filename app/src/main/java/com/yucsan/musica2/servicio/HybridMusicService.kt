package com.yucsan.musica2.servicio

import android.content.Context
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import com.yucsan.musica2.modelo.Song
import com.yucsan.musica2.service.ModernYouTubeService
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Servicio híbrido simplificado: NewPipe + YtDlpWrapper
 * Sin YtDlpService para evitar conflictos
 */
@Singleton
class HybridMusicService @Inject constructor(
    private val context: Context
) {
    companion object {
        private const val TAG = "HybridMusicService"

        @Volatile
        private var INSTANCE: HybridMusicService? = null

        fun getInstance(context: Context): HybridMusicService {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: HybridMusicService(context).also { INSTANCE = it }
            }
        }
    }

    // Solo dos servicios: NewPipe y YtDlpWrapper
    private val newPipeService by lazy { ModernYouTubeService.getInstance() }
    private val ytDlpWrapper by lazy { YtDlpWrapper.getInstance(context) }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var isInitialized = false
    private var ytDlpAvailable = false

    /**
     * Inicialización del servicio híbrido
     */
    suspend fun initialize(): Boolean = withContext(Dispatchers.IO) {
        try {
            if (isInitialized) return@withContext true

            Log.d(TAG, "🚀 Inicializando servicio híbrido simplificado...")

            // Test NewPipe
            val newPipeTest = async {
                withTimeoutOrNull(5000) {
                    newPipeService.testConnection()
                } ?: false
            }

            // Inicializar YtDlpWrapper en paralelo
            val ytDlpInit = async {
                try {
                    ytDlpWrapper.initialize()
                } catch (e: Exception) {
                    Log.w(TAG, "YtDlpWrapper no disponible: ${e.message}")
                    false
                }
            }

            val newPipeOk = newPipeTest.await()
            ytDlpAvailable = ytDlpInit.await()

            // Test yt-dlp si se inicializó
            val ytDlpTest = if (ytDlpAvailable) {
                withTimeoutOrNull(5000) {
                    ytDlpWrapper.testConnection()
                } ?: false
            } else false

            Log.d(TAG, "📊 Estado de servicios:")
            Log.d(TAG, "   📺 NewPipe: $newPipeOk")
            Log.d(TAG, "   🔧 YtDlpWrapper: $ytDlpTest")

            isInitialized = newPipeOk || ytDlpTest

            if (isInitialized) {
                Log.d(TAG, "✅ Servicio híbrido inicializado")
                if (ytDlpTest) {
                    Log.d(TAG, "🎯 yt-dlp disponible como respaldo")
                }
            } else {
                Log.w(TAG, "⚠️ Ambos servicios fallaron, usando modo demo")
            }

            return@withContext true // Siempre permitir modo demo

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error inicializando servicio híbrido", e)
            isInitialized = false
            return@withContext true
        }
    }

    /**
     * Búsqueda inteligente: NewPipe primero, yt-dlp como respaldo
     */
    fun searchSongsFlow(query: String): Flow<List<Song>> = flow {
        emit(emptyList())

        try {
            if (!isInitialized) {
                initialize()
            }

            Log.d(TAG, "🔍 Búsqueda inteligente: $query")

            // Estrategia 1: NewPipe (más rápido)
            val newPipeResults = withTimeoutOrNull(10000) {
                try {
                    newPipeService.searchSongs(query)
                } catch (e: Exception) {
                    Log.w(TAG, "NewPipe búsqueda falló: ${e.message}")
                    emptyList()
                }
            }

            // Verificar si NewPipe devolvió resultados reales
            val newPipeRealResults = newPipeResults?.takeIf { results ->
                results.isNotEmpty() && !results.first().id.startsWith("demo")
            }

            if (newPipeRealResults != null) {
                Log.d(TAG, "✅ NewPipe: ${newPipeRealResults.size} resultados reales")
                emit(newPipeRealResults)
                return@flow
            }

            // Estrategia 2: YtDlpWrapper como respaldo
            if (ytDlpAvailable) {
                Log.d(TAG, "🔄 NewPipe solo devolvió demos, probando yt-dlp...")

                val ytDlpResults = withTimeoutOrNull(20000) {
                    try {
                        ytDlpWrapper.searchSongsFlow(query).let { flow ->
                            var results = emptyList<Song>()
                            flow.collect { results = it }
                            results
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "YtDlpWrapper búsqueda falló: ${e.message}")
                        emptyList()
                    }
                }

                if (!ytDlpResults.isNullOrEmpty()) {
                    Log.d(TAG, "✅ YtDlpWrapper: ${ytDlpResults.size} resultados")
                    emit(ytDlpResults)
                    return@flow
                }
            }

            // Fallback final: contenido demo
            Log.w(TAG, "⚠️ Todos los métodos fallaron, usando demos")
            emit(newPipeService.getPopularSongs())

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error en búsqueda inteligente", e)
            emit(newPipeService.getPopularSongs())
        }
    }

    /**
     * Obtención inteligente de URL de audio
     */
    suspend fun getAudioStreamUrl(videoId: String): String? = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "🎵 Obteniendo stream inteligente para: $videoId")

            if (!isInitialized) {
                initialize()
            }

            // Estrategia 1: NewPipe normal
            val newPipeResult = withTimeoutOrNull(8000) {
                try {
                    newPipeService.getAudioStreamUrl(videoId)
                } catch (e: Exception) {
                    Log.w(TAG, "NewPipe stream falló: ${e.message}")
                    null
                }
            }

            if (!newPipeResult.isNullOrBlank()) {
                Log.d(TAG, "✅ NewPipe stream obtenido")
                return@withContext newPipeResult
            }

            // Estrategia 2: YtDlpWrapper
            if (ytDlpAvailable) {
                Log.d(TAG, "🔄 NewPipe falló, probando yt-dlp...")
                val ytDlpResult = withTimeoutOrNull(15000) {
                    try {
                        ytDlpWrapper.getAudioStreamUrl(videoId)
                    } catch (e: Exception) {
                        Log.w(TAG, "YtDlpWrapper stream falló: ${e.message}")
                        null
                    }
                }

                if (!ytDlpResult.isNullOrBlank()) {
                    Log.d(TAG, "✅ YtDlpWrapper stream obtenido")
                    return@withContext ytDlpResult
                }
            }

            // Estrategia 3: Método alternativo de NewPipe
            Log.d(TAG, "🔄 Probando método alternativo NewPipe...")
            val alternativeResult = withTimeoutOrNull(12000) {
                try {
                    newPipeService.getAudioStreamUrlAlternative(videoId)
                } catch (e: Exception) {
                    Log.w(TAG, "Método alternativo falló: ${e.message}")
                    null
                }
            }

            if (!alternativeResult.isNullOrBlank()) {
                Log.d(TAG, "✅ Método alternativo exitoso")
                return@withContext alternativeResult
            }

            Log.w(TAG, "❌ Todas las estrategias fallaron para: $videoId")
            return@withContext null

        } catch (e: Exception) {
            Log.e(TAG, "💥 Error obteniendo stream inteligente", e)
            null
        }
    }

    /**
     * Obtener trending con fallbacks
     */
    fun getTrendingSongsFlow(): Flow<List<Song>> = flow {
        emit(emptyList())

        try {
            if (!isInitialized) {
                initialize()
            }

            // Intentar con NewPipe primero
            val newPipeResults = withTimeoutOrNull(15000) {
                newPipeService.getTrendingSongs()
            }

            // Verificar si son resultados reales
            val realResults = newPipeResults?.takeIf { results ->
                results.isNotEmpty() && !results.first().id.startsWith("demo")
            }

            if (realResults != null) {
                Log.d(TAG, "✅ Trending NewPipe obtenido")
                emit(realResults)
                return@flow
            }

            // Fallback con YtDlpWrapper
            if (ytDlpAvailable) {
                Log.d(TAG, "🔄 Probando trending con yt-dlp...")
                val ytDlpResults = withTimeoutOrNull(20000) {
                    try {
                        ytDlpWrapper.getTrendingVideos()
                    } catch (e: Exception) {
                        Log.w(TAG, "YtDlpWrapper trending falló: ${e.message}")
                        emptyList()
                    }
                }

                if (!ytDlpResults.isNullOrEmpty()) {
                    Log.d(TAG, "✅ Trending yt-dlp obtenido")
                    emit(ytDlpResults)
                    return@flow
                }
            }

            // Fallback a demos
            emit(newPipeService.getPopularSongs())

        } catch (e: Exception) {
            Log.e(TAG, "Error obteniendo trending", e)
            emit(newPipeService.getPopularSongs())
        }
    }

    /**
     * Obtener información detallada
     */
    suspend fun getVideoDetails(videoId: String): Song? = withContext(Dispatchers.IO) {
        try {
            // Intentar NewPipe primero
            val newPipeResult = withTimeoutOrNull(10000) {
                newPipeService.getVideoDetails(videoId)
            }

            if (newPipeResult != null) {
                Log.d(TAG, "✅ Detalles NewPipe obtenidos")
                return@withContext newPipeResult
            }

            // Por ahora no implementamos detalles con yt-dlp
            Log.d(TAG, "🔄 Detalles yt-dlp no implementados aún")

            return@withContext null

        } catch (e: Exception) {
            Log.e(TAG, "Error obteniendo detalles", e)
            null
        }
    }

    /**
     * Test completo de conectividad
     */
    suspend fun testAllServices(): Map<String, Boolean> = withContext(Dispatchers.IO) {
        val results = mutableMapOf<String, Boolean>()

        try {
            // Test NewPipe
            val newPipeTest = withTimeoutOrNull(5000) {
                newPipeService.testConnection()
            } ?: false

            // Test YtDlpWrapper
            val ytDlpTest = if (ytDlpAvailable) {
                withTimeoutOrNull(5000) {
                    ytDlpWrapper.testConnection()
                } ?: false
            } else false

            results["NewPipe"] = newPipeTest
            results["YtDlpWrapper"] = ytDlpTest

            Log.d(TAG, "🧪 Test completo: $results")

        } catch (e: Exception) {
            Log.e(TAG, "Error en test completo", e)
            results["NewPipe"] = false
            results["YtDlpWrapper"] = false
        }

        return@withContext results
    }

    /**
     * Limpiar caches
     */
    suspend fun clearAllCaches() = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "🧹 Limpiando caches...")

            if (ytDlpAvailable) {
                ytDlpWrapper.clearCache()
            }

            Log.d(TAG, "✅ Caches limpiados")
        } catch (e: Exception) {
            Log.e(TAG, "Error limpiando caches", e)
        }
    }

    /**
     * Obtener estadísticas del servicio
     */
    suspend fun getServiceStats(): Map<String, Any> = withContext(Dispatchers.IO) {
        val stats = mutableMapOf<String, Any>()

        try {
            val connectivity = testAllServices()

            stats["initialized"] = isInitialized
            stats["ytdlp_available"] = ytDlpAvailable
            stats["services_available"] = connectivity.values.count { it }
            stats["services_total"] = connectivity.size
            stats["connectivity"] = connectivity
            stats["primary_service"] = when {
                connectivity["NewPipe"] == true -> "NewPipe"
                connectivity["YtDlpWrapper"] == true -> "YtDlpWrapper"
                else -> "Demo"
            }

        } catch (e: Exception) {
            stats["error"] = e.message ?: "Error desconocido"
        }

        return@withContext stats
    }

    /**
     * Cleanup
     */
    fun cleanup() {
        serviceScope.cancel()
        if (ytDlpAvailable) {
            ytDlpWrapper.cleanup()
        }
        isInitialized = false
        Log.d(TAG, "🧹 Servicio híbrido limpiado")
    }
}