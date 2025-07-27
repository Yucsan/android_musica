package com.yucsan.musica2.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.yucsan.musica2.viewmodel.MusicViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicScreen(
    modifier: Modifier = Modifier,
    viewModel: MusicViewModel
) {
    // Estados del nuevo ViewModel
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()
    val trendingSongs by viewModel.trendingSongs.collectAsStateWithLifecycle()
    val currentSong by viewModel.currentSong.collectAsStateWithLifecycle()
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    val debugInfo by viewModel.debugInfo.collectAsStateWithLifecycle()
    val isInitialized by viewModel.isInitialized.collectAsStateWithLifecycle()

    // Estado local para búsqueda
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
        // PANEL DE DEBUG OPCIONAL (se puede ocultar)
        // ============================================================================
        if (showDebugPanel && (debugInfo.isNotEmpty() || !isInitialized)) {
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
                            text = if (isInitialized) "🔧 Debug" else "⚠️ Estado",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Row {
                            TextButton(
                                onClick = { viewModel.testServices() }
                            ) {
                                Text("Test", style = MaterialTheme.typography.labelSmall)
                            }
                            IconButton(
                                onClick = { showDebugPanel = false },
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
                            text = if (isInitialized) "Servicios listos" else "Modo demo",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        }

        // Botón para mostrar debug si está oculto
        if (!showDebugPanel) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(
                    onClick = { showDebugPanel = true }
                ) {
                    Icon(
                        Icons.Default.BugReport,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Debug", style = MaterialTheme.typography.labelSmall)
                }

                Text(
                    text = if (isInitialized) "✅" else "⚠️",
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }

        // ============================================================================
        // BARRA DE BÚSQUEDA (tu diseño original)
        // ============================================================================
        SearchBar(
            query = searchQuery,
            onQueryChange = { newQuery ->
                searchQuery = newQuery
                if (newQuery.isNotBlank()) {
                    viewModel.searchSongs(newQuery)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        )

        // ============================================================================
        // CONTADOR DE CANCIONES (tu diseño original)
        // ============================================================================
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (searchResults.isNotEmpty()) {
                    "🔍 Resultados: ${searchResults.size}"
                } else if (trendingSongs.isNotEmpty()) {
                    "📈 Trending: ${trendingSongs.size}"
                } else {
                    "Canciones: 0"
                },
                modifier = Modifier.weight(1f)
            )

            // Indicador de tipo de contenido
            if (songsToShow.isNotEmpty()) {
                val isRealContent = !songsToShow.first().id.startsWith("demo")
                Text(
                    text = if (isRealContent) "📺 YouTube" else "🎧 Demo",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isRealContent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                )
            }
        }

        // ============================================================================
        // MENSAJES DE ERROR
        // ============================================================================
        errorMessage?.let { error ->
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
                        text = error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = { viewModel.clearError() },
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

        // ============================================================================
        // INDICADOR DE CARGA
        // ============================================================================
        if (isLoading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        // ============================================================================
        // LISTA DE CANCIONES (tu diseño original mejorado)
        // ============================================================================
        if (isLoading) {
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Cargando música...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        } else if (songsToShow.isEmpty()) {
            // Estado vacío
            Box(
                modifier = Modifier.weight(1f),
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
                            "Busca música para comenzar"
                        } else {
                            "No se encontraron resultados para '$searchQuery'"
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.outline
                    )
                    if (searchQuery.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(
                            onClick = {
                                searchQuery = ""
                                viewModel.clearError()
                            }
                        ) {
                            Text("Limpiar búsqueda")
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(songsToShow) { song ->
                    SongItem(
                        song = song,
                        isCurrentSong = song.id == currentSong?.id,
                        onClick = { viewModel.playSong(song) }
                    )
                }

                // Espaciado para el mini player
                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }

        // ============================================================================
        // MINI PLAYER (tu diseño original mejorado)
        // ============================================================================
        currentSong?.let { song ->
            MiniPlayer(
                song = song,
                isPlaying = isPlaying,
                onPlayPause = { viewModel.togglePlayPause() },
                onNext = { /* implementar si tienes next */ },
                onPrevious = { /* implementar si tienes previous */ },
                onStop = { viewModel.stopPlayback() }
            )
        }
    }
}

// ============================================================================
// COMPONENTES AUXILIARES (tu diseño original)
// ============================================================================

@Composable
fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        label = { Text("Buscar música en YouTube") },
        placeholder = { Text("Ej: imagine dragons, piano music...") },
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
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    Icon(
                        imageVector = if (song.id.startsWith("demo"))
                            Icons.Default.Audiotrack
                        else
                            Icons.Default.OndemandVideo,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = if (song.id.startsWith("demo"))
                            MaterialTheme.colorScheme.outline
                        else
                            MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (song.id.startsWith("demo")) "Demo" else "YouTube",
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
            }

            Row {
                IconButton(onClick = onPlayPause) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pausar" else "Reproducir"
                    )
                }
                IconButton(onClick = onStop) {
                    Icon(Icons.Default.Stop, contentDescription = "Parar")
                }
            }
        }
    }
}