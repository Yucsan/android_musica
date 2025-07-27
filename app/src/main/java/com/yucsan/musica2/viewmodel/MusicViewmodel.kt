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
import com.yucsan.musica2.service.JamendoService
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

    // ============================================================================
    // SERVICIOS
    // ============================================================================

    // Jamendo como servicio principal
    private val jamendoService = JamendoService.getInstance()

    // Tu MusicPlayer actual
    private var musicPlayer: com.yucsan.musica2.servicio.MusicPlayer? = null

    // ============================================================================
    // ESTADOS UI
    // ============================================================================

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

    // ============================================================================
    // GÉNEROS DISPONIBLES
    // ============================================================================

    private val _availableGenres = MutableStateFlow<List<String>>(emptyList())
    val availableGenres: StateFlow<List<String>> = _availableGenres.asStateFlow()

    private val _selectedGenre = MutableStateFlow<String?>(null)
    val selectedGenre: StateFlow<String?> = _selectedGenre.asStateFlow()

    // ============================================================================
    // PLAYLIST ACTUAL (para next/previous)
    // ============================================================================

    private val _currentPlaylist = MutableStateFlow<List<Song>>(emptyList())
    val currentPlaylist: StateFlow<List<Song>> = _currentPlaylist.asStateFlow()

    private val _currentSongIndex = MutableStateFlow(-1)
    val currentSongIndex: StateFlow<Int> = _currentSongIndex.asStateFlow()

    init {
        initializeServices()
        loadInitialContent()
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
     * Inicializar servicios con Jamendo
     */
    private fun initializeServices() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _debugInfo.value = "🚀 Inicializando Jamendo..."
                Log.d(TAG, "🚀 Inicializando servicios Jamendo...")

                // Test de conectividad Jamendo
                val jamendoWorking = jamendoService.testConnection()

                if (jamendoWorking) {
                    Log.d(TAG, "✅ Jamendo conectado correctamente")
                    _debugInfo.value = "✅ Jamendo conectado"
                    _isInitialized.value = true
                } else {
                    Log.w(TAG, "⚠️ Jamendo en modo demo")
                    _debugInfo.value = "⚠️ Jamendo en modo demo"
                    _isInitialized.value = true // Aún así permitir usar demos
                }

                // Cargar géneros disponibles
                loadGenres()

            } catch (e: Exception) {
                Log.e(TAG, "❌ Error inicializando servicios", e)
                _errorMessage.value = "Error inicializando: ${e.message}"
                _debugInfo.value = "❌ Error: ${e.message}"
                _isInitialized.value = false
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Cargar géneros disponibles
     */
    private fun loadGenres() {
        viewModelScope.launch {
            try {
                val genres = jamendoService.getAvailableGenres()
                _availableGenres.value = genres
                Log.d(TAG, "📂 Géneros cargados: ${genres.size}")

                // Seleccionar "rock" por defecto
                if (genres.isNotEmpty() && _selectedGenre.value == null) {
                    _selectedGenre.value = "rock"
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error cargando géneros", e)
            }
        }
    }

    /**
     * Cargar contenido inicial
     */
    private fun loadInitialContent() {
        viewModelScope.launch {
            try {
                _debugInfo.value = "📈 Cargando música destacada..."

                // Cargar música destacada de Jamendo
                val featuredTracks = jamendoService.getFeaturedTracks()
                _trendingSongs.value = featuredTracks
                _currentPlaylist.value = featuredTracks

                val isRealContent = featuredTracks.isNotEmpty() && !featuredTracks.first().id.startsWith("demo")

                if (isRealContent) {
                    _debugInfo.value = "📈 Música destacada cargada (${featuredTracks.size} de Jamendo)"
                    Log.d(TAG, "📈 Featured real: ${featuredTracks.size}")
                } else {
                    _debugInfo.value = "📈 Demos cargados (${featuredTracks.size})"
                    Log.d(TAG, "📈 Featured demo: ${featuredTracks.size}")
                }

            } catch (e: Exception) {
                Log.e(TAG, "❌ Error cargando contenido inicial", e)
                _debugInfo.value = "❌ Error cargando contenido: ${e.message}"
            }
        }
    }

    /**
     * Buscar canciones en Jamendo
     */
    fun searchSongs(query: String) {
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            _currentPlaylist.value = _trendingSongs.value
            return
        }

        viewModelScope.launch {
            try {
                _isLoading.value = true
                _errorMessage.value = null
                _debugInfo.value = "🔍 Buscando en Jamendo: '$query'"

                Log.d(TAG, "🔍 Búsqueda Jamendo: $query")

                val results = jamendoService.searchTracks(query)
                _searchResults.value = results
                _currentPlaylist.value = results

                val isRealResults = results.isNotEmpty() && !results.first().id.startsWith("demo")

                if (isRealResults) {
                    _debugInfo.value = "✅ Encontrados ${results.size} resultados de Jamendo"
                    Log.d(TAG, "✅ Resultados reales: ${results.size}")

                    // Log de los primeros resultados
                    results.take(3).forEach { song ->
                        Log.d(TAG, "   🎵 ${song.title} - ${song.artist}")
                    }
                } else if (results.isNotEmpty()) {
                    _debugInfo.value = "⚠️ Solo demos disponibles (${results.size})"
                    Log.w(TAG, "⚠️ Solo demos: ${results.size}")
                } else {
                    _debugInfo.value = "❌ Sin resultados para '$query'"
                    Log.w(TAG, "❌ Sin resultados")
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
     * Buscar por género
     */
    fun searchByGenre(genre: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _selectedGenre.value = genre
                _debugInfo.value = "🎵 Cargando género: $genre"

                Log.d(TAG, "🎵 Búsqueda por género: $genre")

                val results = jamendoService.getTracksByGenre(genre)
                _searchResults.value = results
                _currentPlaylist.value = results

                val isRealResults = results.isNotEmpty() && !results.first().id.startsWith("demo")

                if (isRealResults) {
                    _debugInfo.value = "✅ Género '$genre': ${results.size} canciones"
                    Log.d(TAG, "✅ Género real: ${results.size}")
                } else {
                    _debugInfo.value = "⚠️ Género '$genre': demos (${results.size})"
                    Log.w(TAG, "⚠️ Género demo: ${results.size}")
                }

            } catch (e: Exception) {
                Log.e(TAG, "❌ Error búsqueda por género", e)
                _errorMessage.value = "Error cargando género: ${e.message}"
                _debugInfo.value = "❌ Error género: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Reproducir canción con soporte para playlist
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
                    delay(1000)
                }

                // Actualizar índice en playlist
                val currentPlaylist = _currentPlaylist.value
                val songIndex = currentPlaylist.indexOfFirst { it.id == song.id }
                if (songIndex >= 0) {
                    _currentSongIndex.value = songIndex
                }

                // Reproducir canción
                _currentSong.value = song
                player.playSong(song)
                _isPlaying.value = true

                _debugInfo.value = "▶️ Reproduciendo: ${song.title}"
                Log.d(TAG, "▶️ Reproducción iniciada")

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
     * Siguiente canción en playlist
     */
    fun nextSong() {
        val currentPlaylist = _currentPlaylist.value
        val currentIndex = _currentSongIndex.value

        if (currentPlaylist.isNotEmpty() && currentIndex >= 0) {
            val nextIndex = if (currentIndex < currentPlaylist.size - 1) {
                currentIndex + 1
            } else {
                0 // Volver al inicio
            }

            val nextSong = currentPlaylist[nextIndex]
            playSong(nextSong)
            Log.d(TAG, "⏭️ Siguiente: ${nextSong.title}")
        } else {
            _errorMessage.value = "No hay siguiente canción"
            Log.w(TAG, "⏭️ No hay siguiente canción")
        }
    }

    /**
     * Canción anterior en playlist
     */
    fun previousSong() {
        val currentPlaylist = _currentPlaylist.value
        val currentIndex = _currentSongIndex.value

        if (currentPlaylist.isNotEmpty() && currentIndex >= 0) {
            val previousIndex = if (currentIndex > 0) {
                currentIndex - 1
            } else {
                currentPlaylist.size - 1 // Ir al final
            }

            val previousSong = currentPlaylist[previousIndex]
            playSong(previousSong)
            Log.d(TAG, "⏮️ Anterior: ${previousSong.title}")
        } else {
            _errorMessage.value = "No hay canción anterior"
            Log.w(TAG, "⏮️ No hay canción anterior")
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
            _currentSongIndex.value = -1
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

                val jamendoWorking = jamendoService.testConnection()

                val message = buildString {
                    appendLine("🧪 Test de servicios:")
                    appendLine("• Jamendo: ${if (jamendoWorking) "✅ OK" else "❌ Demo"}")
                    appendLine()
                    appendLine("🎵 MusicPlayer:")
                    val player = musicPlayer
                    if (player != null) {
                        appendLine("• Conectado: ✅")
                        appendLine("• Servicio listo: ${if (player.isServiceReady()) "✅" else "❌"}")
                    } else {
                        appendLine("• Conectado: ❌")
                    }
                    appendLine()
                    appendLine("📊 Estadísticas:")
                    appendLine("• Trending: ${_trendingSongs.value.size}")
                    appendLine("• Búsqueda: ${_searchResults.value.size}")
                    appendLine("• Géneros: ${_availableGenres.value.size}")
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
     * Limpiar búsqueda y volver a trending
     */
    fun clearSearch() {
        _searchResults.value = emptyList()
        _currentPlaylist.value = _trendingSongs.value
        _debugInfo.value = "📈 Mostrando música destacada"
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
        // Jamendo service no necesita cleanup especial
    }
}