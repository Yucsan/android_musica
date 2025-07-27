package com.yucsan.musica2.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.runtime.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import com.yucsan.musica2.modelo.Song
import com.yucsan.musica2.servicio.HybridMusicService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

@HiltViewModel
class MusicViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    companion object {
        private const val TAG = "MusicViewModel"
    }

    // Servicio híbrido
    private val hybridMusicService = HybridMusicService.getInstance(context)

    // Estados UI
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _searchResults = MutableStateFlow<List<Song>>(emptyList())
    val searchResults: StateFlow<List<Song>> = _searchResults.asStateFlow()

    private val _trendingSongs = MutableStateFlow<List<Song>>(emptyList())
    val trendingSongs: StateFlow<List<Song>> = _trendingSongs.asStateFlow()

    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong: StateFlow<Song?> = _currentSong.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _debugInfo = MutableStateFlow<String>("")
    val debugInfo: StateFlow<String> = _debugInfo.asStateFlow()

    // Estado de inicialización
    private val _isInitialized = MutableStateFlow(false)
    val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()

    // Tu MusicPlayer actual
    private var musicPlayer: com.yucsan.musica2.servicio.MusicPlayer? = null

    init {
        initializeServices()
        loadTrendingSongs()
    }

    /**
     * Método para establecer el MusicPlayer (desde tu MainActivity)
     */
    fun setMusicPlayer(player: com.yucsan.musica2.servicio.MusicPlayer) {
        this.musicPlayer = player
        Log.d(TAG, "🎵 MusicPlayer establecido")
        _debugInfo.value = "🎵 MusicPlayer conectado"

        // Configurar listener para actualizar estado
        player.setPlayerListener(object : com.yucsan.musica2.servicio.MusicPlayer.MusicPlayerListener {
            override fun onPlaybackStateChanged(isPlaying: Boolean) {
                _isPlaying.value = isPlaying
                _debugInfo.value = if (isPlaying) "▶️ Reproduciendo" else "⏸️ Pausado"
            }

            override fun onSongChanged(song: Song?) {
                _currentSong.value = song
                if (song != null) {
                    _debugInfo.value = "🎵 ${song.title}"
                }
            }

            override fun onProgressUpdate(currentPosition: Long, duration: Long) {
                // Actualizar progreso si es necesario
            }
        })
    }

    /**
     * Inicializar servicios con debugging detallado
     */
    private fun initializeServices() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _debugInfo.value = "🚀 Inicializando servicios..."
                Log.d(TAG, "🚀 Inicializando servicios...")

                // Inicializar servicio híbrido
                val initialized = hybridMusicService.initialize()
                _isInitialized.value = initialized

                if (initialized) {
                    Log.d(TAG, "✅ Servicios inicializados correctamente")
                    _debugInfo.value = "✅ Servicios inicializados"

                    // Test rápido de búsqueda
                    testYouTubeConnection()
                } else {
                    Log.w(TAG, "⚠️ Inicialización parcial")
                    _debugInfo.value = "⚠️ Usando modo demo"
                    _errorMessage.value = "Servicios en modo demo"
                }

            } catch (e: Exception) {
                Log.e(TAG, "❌ Error inicializando servicios", e)
                _errorMessage.value = "Error inicializando: ${e.message}"
                _debugInfo.value = "❌ Error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Test de conexión a YouTube
     */
    private suspend fun testYouTubeConnection() {
        try {
            _debugInfo.value = "🧪 Probando conexión YouTube..."

            val stats = hybridMusicService.getServiceStats()
            Log.d(TAG, "📊 Stats: $stats")

            val connectivity = stats["connectivity"] as? Map<String, Boolean>
            val newPipeWorking = connectivity?.get("NewPipe") ?: false

            if (newPipeWorking) {
                _debugInfo.value = "✅ YouTube conectado vía NewPipe"

                // Test de búsqueda real
                Log.d(TAG, "🔍 Test de búsqueda: 'piano music'")
                hybridMusicService.searchSongsFlow("piano music")
                    .collect { songs ->
                        if (songs.isNotEmpty() && songs.first().id != "demo1") {
                            _debugInfo.value = "✅ YouTube funcionando - ${songs.size} resultados reales"
                            Log.d(TAG, "✅ Búsqueda real exitosa: ${songs.size} resultados")
                        } else {
                            _debugInfo.value = "⚠️ Solo resultados demo disponibles"
                            Log.w(TAG, "⚠️ Solo demos disponibles")
                        }
                    }
            } else {
                _debugInfo.value = "❌ YouTube no conectado"
                Log.w(TAG, "❌ NewPipe no funciona")
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error en test YouTube", e)
            _debugInfo.value = "❌ Test falló: ${e.message}"
        }
    }

    /**
     * Buscar canciones con debugging detallado
     */
    fun searchSongs(query: String) {
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            return
        }

        viewModelScope.launch {
            try {
                _isLoading.value = true
                _errorMessage.value = null
                _debugInfo.value = "🔍 Buscando: '$query'"

                Log.d(TAG, "🔍 Búsqueda iniciada: $query")

                var resultCount = 0
                var isRealResults = false

                hybridMusicService.searchSongsFlow(query)
                    .collect { songs ->
                        _searchResults.value = songs
                        resultCount = songs.size

                        // Verificar si son resultados reales o demos
                        isRealResults = songs.isNotEmpty() && !songs.first().id.startsWith("demo")

                        if (isRealResults) {
                            _debugInfo.value = "✅ Encontrados $resultCount resultados de YouTube"
                            Log.d(TAG, "✅ Resultados reales: $resultCount")

                            // Log de los primeros resultados
                            songs.take(3).forEach { song ->
                                Log.d(TAG, "   🎵 ${song.title} - ${song.artist}")
                            }
                        } else if (songs.isNotEmpty()) {
                            _debugInfo.value = "⚠️ Solo demos disponibles ($resultCount)"
                            Log.w(TAG, "⚠️ Solo demos: $resultCount")
                        } else {
                            _debugInfo.value = "❌ Sin resultados para '$query'"
                            Log.w(TAG, "❌ Sin resultados")
                        }
                    }

            } catch (e: Exception) {
                Log.e(TAG, "❌ Error en búsqueda", e)
                _errorMessage.value = "Error en búsqueda: ${e.message}"
                _debugInfo.value = "❌ Error: ${e.message}"
                _searchResults.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Reproducir canción con debugging detallado
     */
    fun playSong(song: Song) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _errorMessage.value = null
                _debugInfo.value = "🎵 Preparando: ${song.title}"

                Log.d(TAG, "🎵 Reproduciendo: ${song.title} (ID: ${song.id})")

                // Verificar que MusicPlayer esté disponible
                val player = musicPlayer
                if (player == null) {
                    _errorMessage.value = "MusicPlayer no disponible"
                    _debugInfo.value = "❌ MusicPlayer no conectado"
                    Log.e(TAG, "❌ MusicPlayer no establecido")
                    return@launch
                }

                if (!player.isServiceReady()) {
                    _debugInfo.value = "⏳ Esperando MusicService..."
                    Log.w(TAG, "⏳ MusicService no está listo, esperando...")
                    delay(1000) // Dar tiempo para conectar
                }

                // Verificar si es demo o real
                if (song.id.startsWith("demo")) {
                    Log.d(TAG, "🎧 Reproduciendo demo")
                    _debugInfo.value = "🎧 Reproduciendo demo: ${song.title}"

                    // Para demos, usar el método directo
                    _currentSong.value = song
                    player.playSong(song)
                    _isPlaying.value = true

                } else {
                    Log.d(TAG, "🔍 Obteniendo stream de YouTube para: ${song.id}")
                    _debugInfo.value = "🔍 Obteniendo stream de YouTube..."

                    // Obtener URL de stream de YouTube
                    val streamUrl = hybridMusicService.getAudioStreamUrl(song.id)

                    if (streamUrl.isNullOrBlank()) {
                        _errorMessage.value = "No se pudo obtener stream para '${song.title}'"
                        _debugInfo.value = "❌ Stream no disponible"
                        Log.w(TAG, "❌ No se obtuvo stream para: ${song.id}")
                        return@launch
                    }

                    Log.d(TAG, "✅ Stream obtenido: ${streamUrl.take(50)}...")
                    _debugInfo.value = "✅ Stream obtenido, iniciando reproducción"

                    // Actualizar canción con URL de stream
                    val updatedSong = song.copy(audioUrl = streamUrl)
                    _currentSong.value = updatedSong

                    // Reproducir usando playFromUrlWithRetry para mejor confiabilidad
                    player.playFromUrlWithRetry(streamUrl, song.title)
                    _isPlaying.value = true
                    _debugInfo.value = "▶️ Reproduciendo: ${song.title}"
                    Log.d(TAG, "▶️ Reproducción iniciada")
                }

            } catch (e: Exception) {
                Log.e(TAG, "❌ Error reproduciendo canción", e)
                _errorMessage.value = "Error reproduciendo '${song.title}': ${e.message}"
                _debugInfo.value = "❌ Error: ${e.message}"
                _isPlaying.value = false
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Cargar trending con debugging
     */
    private fun loadTrendingSongs() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "📈 Cargando trending...")
                _debugInfo.value = "📈 Cargando trending..."

                hybridMusicService.getTrendingSongsFlow()
                    .collect { songs ->
                        _trendingSongs.value = songs

                        val isRealTrending = songs.isNotEmpty() && !songs.first().id.startsWith("demo")

                        if (isRealTrending) {
                            _debugInfo.value = "📈 Trending real cargado (${songs.size})"
                            Log.d(TAG, "📈 Trending real: ${songs.size}")
                        } else {
                            _debugInfo.value = "📈 Trending demo (${songs.size})"
                            Log.d(TAG, "📈 Trending demo: ${songs.size}")
                        }
                    }

            } catch (e: Exception) {
                Log.e(TAG, "❌ Error cargando trending", e)
                _debugInfo.value = "❌ Error trending: ${e.message}"
            }
        }
    }

    /**
     * Toggle play/pause
     */
    fun togglePlayPause() {
        val current = _currentSong.value
        val player = musicPlayer

        if (current != null && player != null) {
            if (_isPlaying.value) {
                player.pause()
                _isPlaying.value = false
                _debugInfo.value = "⏸️ Pausado"
                Log.d(TAG, "⏸️ Pausado")
            } else {
                player.resume()
                _isPlaying.value = true
                _debugInfo.value = "▶️ Reanudado"
                Log.d(TAG, "▶️ Reanudado")
            }
        } else {
            _errorMessage.value = "No hay canción para pausar/reanudar"
        }
    }

    /**
     * Parar reproducción
     */
    fun stopPlayback() {
        val player = musicPlayer
        if (player != null) {
            player.stop()
            _isPlaying.value = false
            _currentSong.value = null
            _debugInfo.value = "⏹️ Detenido"
            Log.d(TAG, "⏹️ Reproducción detenida")
        }
    }

    /**
     * Test manual de servicios
     */
    fun testServices() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _debugInfo.value = "🧪 Probando servicios..."

                val results = hybridMusicService.testAllServices()

                val message = buildString {
                    appendLine("🧪 Test de servicios:")
                    results.forEach { (service, working) ->
                        appendLine("• $service: ${if (working) "✅ OK" else "❌ Error"}")
                    }
                    appendLine()
                    appendLine("🎵 MusicPlayer:")
                    val player = musicPlayer
                    if (player != null) {
                        appendLine("• Conectado: ✅")
                        appendLine("• Servicio listo: ${if (player.isServiceReady()) "✅" else "❌"}")
                    } else {
                        appendLine("• Conectado: ❌")
                    }
                }

                _debugInfo.value = message.trim()
                Log.d(TAG, message)

            } catch (e: Exception) {
                Log.e(TAG, "Error en test", e)
                _debugInfo.value = "❌ Error test: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Limpiar mensaje de error
     */
    fun clearError() {
        _errorMessage.value = null
    }

    /**
     * Limpiar info de debug
     */
    fun clearDebugInfo() {
        _debugInfo.value = ""
    }

    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "🧹 Limpiando ViewModel...")
        hybridMusicService.cleanup()
    }
}