package com.yucsan.musica2.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yucsan.musica2.modelo.Song
import com.yucsan.musica2.modelo.getFormattedDuration
import com.yucsan.musica2.viewmodel.MusicViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicScreen(
    modifier: Modifier = Modifier,
    viewModel: MusicViewModel
) {
    // Estados del ViewModel
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()
    val trendingSongs by viewModel.trendingSongs.collectAsStateWithLifecycle()
    val currentSong by viewModel.currentSong.collectAsStateWithLifecycle()
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    val debugInfo by viewModel.debugInfo.collectAsStateWithLifecycle()
    val isInitialized by viewModel.isInitialized.collectAsStateWithLifecycle()
    val availableGenres by viewModel.availableGenres.collectAsStateWithLifecycle()
    val selectedGenre by viewModel.selectedGenre.collectAsStateWithLifecycle()

    // Estado local
    var searchQuery by remember { mutableStateOf("") }
    var showDebugPanel by remember { mutableStateOf(true) }

    // Determinar qué lista mostrar
    val songsToShow = if (searchResults.isNotEmpty()) {
        searchResults
    } else {
        trendingSongs
    }

    Column(modifier = modifier.fillMaxSize()) {

        // ============================================================================
        // PANEL DE DEBUG OPCIONAL
        // ============================================================================
        if (showDebugPanel && (debugInfo.isNotEmpty() || !isInitialized)) {
            DebugPanel(
                debugInfo = debugInfo,
                isInitialized = isInitialized,
                onTest = { viewModel.testServices() },
                onClose = { showDebugPanel = false }
            )
        }

        // Botón para mostrar debug si está oculto
        if (!showDebugPanel) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = { showDebugPanel = true }) {
                    Icon(Icons.Default.BugReport, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Debug", style = MaterialTheme.typography.labelSmall)
                }
                Text(
                    text = "🎵 Jamendo ${if (isInitialized) "✅" else "⚠️"}",
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }

        // ============================================================================
        // BARRA DE BÚSQUEDA
        // ============================================================================
        SearchBar(
            query = searchQuery,
            onQueryChange = { newQuery ->
                searchQuery = newQuery
                if (newQuery.isNotBlank()) {
                    viewModel.searchSongs(newQuery)
                } else {
                    viewModel.clearSearch()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        )

        // ============================================================================
        // FILTROS DE GÉNERO
        // ============================================================================
        if (availableGenres.isNotEmpty() && searchQuery.isBlank()) {
            GenreFilterRow(
                genres = availableGenres,
                selectedGenre = selectedGenre,
                onGenreSelected = { genre ->
                    viewModel.searchByGenre(genre)
                },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }

        // ============================================================================
        // INFORMACIÓN DE CONTENIDO
        // ============================================================================
        ContentInfoRow(
            searchResults = searchResults,
            trendingSongs = trendingSongs,
            songsToShow = songsToShow,
            searchQuery = searchQuery,
            selectedGenre = selectedGenre,
            onClearSearch = {
                searchQuery = ""
                viewModel.clearSearch()
            }
        )

        // ============================================================================
        // MENSAJES DE ERROR
        // ============================================================================
        errorMessage?.let { error ->
            ErrorCard(
                errorMessage = error,
                onDismiss = { viewModel.clearError() }
            )
        }

        // ============================================================================
        // INDICADOR DE CARGA
        // ============================================================================
        if (isLoading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        // ============================================================================
        // CONTENIDO PRINCIPAL
        // ============================================================================
        when {
            isLoading -> {
                LoadingContent()
            }
            songsToShow.isEmpty() -> {
                EmptyContent(
                    searchQuery = searchQuery,
                    onClearSearch = {
                        searchQuery = ""
                        viewModel.clearSearch()
                    }
                )
            }
            else -> {
                SongsList(
                    songs = songsToShow,
                    currentSong = currentSong,
                    onSongClick = { song -> viewModel.playSong(song) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // ============================================================================
        // MINI PLAYER
        // ============================================================================
        currentSong?.let { song ->
            MiniPlayer(
                song = song,
                isPlaying = isPlaying,
                onPlayPause = { viewModel.togglePlayPause() },
                onNext = { viewModel.nextSong() },
                onPrevious = { viewModel.previousSong() },
                onStop = { viewModel.stopPlayback() }
            )
        }
    }
}

// ============================================================================
// COMPONENTES AUXILIARES
// ============================================================================

@Composable
fun DebugPanel(
    debugInfo: String,
    isInitialized: Boolean,
    onTest: () -> Unit,
    onClose: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isInitialized)
                MaterialTheme.colorScheme.surfaceVariant
            else
                MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isInitialized) "🔧 Debug Jamendo" else "⚠️ Estado",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                Row {
                    TextButton(onClick = onTest) {
                        Text("Test", style = MaterialTheme.typography.labelSmall)
                    }
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Cerrar debug",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            if (debugInfo.isNotEmpty()) {
                Text(
                    text = debugInfo,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            // Indicador de estado
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 4.dp)
            ) {
                Icon(
                    imageVector = if (isInitialized) Icons.Default.CheckCircle else Icons.Default.Warning,
                    contentDescription = null,
                    tint = if (isInitialized) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (isInitialized) "Jamendo conectado" else "Modo demo",
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}

@Composable
fun GenreFilterRow(
    genres: List<String>,
    selectedGenre: String?,
    onGenreSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = "🎵 Géneros",
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 4.dp)
        ) {
            // Botón "Todos" para limpiar género
            item {
                FilterChip(
                    onClick = { onGenreSelected("featured") },
                    label = { Text("Destacado") },
                    selected = selectedGenre == null || selectedGenre == "featured",
                    leadingIcon = {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                )
            }

            items(genres) { genre ->
                FilterChip(
                    onClick = { onGenreSelected(genre) },
                    label = { Text(genre.capitalize()) },
                    selected = selectedGenre == genre
                )
            }
        }
    }
}

@Composable
fun ContentInfoRow(
    searchResults: List<Song>,
    trendingSongs: List<Song>,
    songsToShow: List<Song>,
    searchQuery: String,
    selectedGenre: String?,
    onClearSearch: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = when {
                searchResults.isNotEmpty() -> "🔍 Resultados: ${searchResults.size}"
                selectedGenre != null && selectedGenre != "featured" -> "🎵 Género '$selectedGenre': ${songsToShow.size}"
                trendingSongs.isNotEmpty() -> "📈 Destacado: ${trendingSongs.size}"
                else -> "Canciones: 0"
            },
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Indicador de fuente
            if (songsToShow.isNotEmpty()) {
                val isRealContent = !songsToShow.first().id.startsWith("demo")
                Text(
                    text = if (isRealContent) "🎵 Jamendo" else "🎧 Demo",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isRealContent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                )
            }

            // Botón limpiar búsqueda
            if (searchQuery.isNotBlank() || (selectedGenre != null && selectedGenre != "featured")) {
                TextButton(
                    onClick = onClearSearch,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(
                        Icons.Default.Clear,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Limpiar", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
fun ErrorCard(
    errorMessage: String,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Error,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Cerrar",
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun LoadingContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Cargando música de Jamendo...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}

@Composable
fun EmptyContent(
    searchQuery: String,
    onClearSearch: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                Icons.Default.MusicNote,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.outline
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = if (searchQuery.isBlank()) {
                    "Busca música libre en Jamendo"
                } else {
                    "No se encontraron resultados para '$searchQuery'"
                },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.outline
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Prueba con géneros como 'rock', 'electronic', 'jazz'",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )
            if (searchQuery.isNotBlank()) {
                Spacer(modifier = Modifier.height(16.dp))
                TextButton(onClick = onClearSearch) {
                    Text("Limpiar búsqueda")
                }
            }
        }
    }
}

@Composable
fun SongsList(
    songs: List<Song>,
    currentSong: Song?,
    onSongClick: (Song) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        items(songs) { song ->
            SongItem(
                song = song,
                isCurrentSong = song.id == currentSong?.id,
                onClick = { onSongClick(song) }
            )
        }

        // Espaciado para el mini player
        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        label = { Text("Buscar música libre en Jamendo") },
        placeholder = { Text("Ej: piano music, electronic, jazz...") },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Buscar"
            )
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Default.Clear, contentDescription = "Limpiar")
                }
            }
        },
        modifier = modifier,
        singleLine = true
    )
}

@Composable
fun SongItem(
    song: Song,
    isCurrentSong: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrentSong)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (isCurrentSong) FontWeight.Bold else FontWeight.Normal,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = song.artist,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Indicador del tipo de fuente
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Icon(
                        imageVector = if (song.id.startsWith("demo"))
                            Icons.Default.Audiotrack
                        else
                            Icons.Default.LibraryMusic,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = if (song.id.startsWith("demo"))
                            MaterialTheme.colorScheme.outline
                        else
                            MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (song.id.startsWith("demo")) "Demo" else "Jamendo",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (song.id.startsWith("demo"))
                            MaterialTheme.colorScheme.outline
                        else
                            MaterialTheme.colorScheme.primary
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = song.getFormattedDuration(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (isCurrentSong) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = "Reproduciendo",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
fun MiniPlayer(
    song: Song,
    isPlaying: Boolean,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onStop: () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Información de la canción
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = song.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = song.artist,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    // Indicador de fuente
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Icon(
                            imageVector = if (song.id.startsWith("demo"))
                                Icons.Default.Audiotrack
                            else
                                Icons.Default.LibraryMusic,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (song.id.startsWith("demo")) "Demo" else "Jamendo",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Controles de reproducción
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(onClick = onPrevious) {
                        Icon(
                            imageVector = Icons.Default.SkipPrevious,
                            contentDescription = "Anterior"
                        )
                    }

                    FilledIconButton(
                        onClick = onPlayPause,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pausar" else "Reproducir"
                        )
                    }

                    IconButton(onClick = onNext) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = "Siguiente"
                        )
                    }

                    // Botón stop opcional
                    IconButton(
                        onClick = onStop,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Stop,
                            contentDescription = "Parar",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}