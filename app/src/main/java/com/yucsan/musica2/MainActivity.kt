package com.yucsan.musica2

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.yucsan.musica2.servicio.MusicPlayer
import com.yucsan.musica2.ui.theme.Musica2Theme
import com.yucsan.musica2.ui.screens.MusicScreen
import com.yucsan.musica2.ui.screens.PermissionHandler
import com.yucsan.musica2.viewmodel.MusicViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    companion object {
        private const val TAG = "MainActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.d(TAG, "🚀 MainActivity iniciada")

        enableEdgeToEdge()

        setContent {
            Musica2Theme {
                MusicApp()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "🔚 MainActivity destruida")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicApp() {
    val context = LocalContext.current

    // ✅ Usar hiltViewModel() en lugar de viewModel()
    val viewModel: MusicViewModel = hiltViewModel()

    // Crear MusicPlayer y hacer bind solo 1 vez
    val musicPlayer = remember {
        Log.d("MusicApp", "🎵 Creando MusicPlayer")
        MusicPlayer(context)
    }

    // Efecto para inicializar servicios
    LaunchedEffect(musicPlayer) {
        try {
            Log.d("MusicApp", "🔗 Iniciando bind del servicio...")
            musicPlayer.bindService()

            // Dar tiempo para que el servicio se conecte
            kotlinx.coroutines.delay(1000)

            // Establecer MusicPlayer en el ViewModel
            viewModel.setMusicPlayer(musicPlayer)

            Log.d("MusicApp", "✅ MusicPlayer configurado en ViewModel")

        } catch (e: Exception) {
            Log.e("MusicApp", "❌ Error configurando MusicPlayer", e)
        }
    }

    // Desvincular el servicio cuando MusicApp desaparezca
    DisposableEffect(musicPlayer) {
        onDispose {
            try {
                Log.d("MusicApp", "🔚 Desvinculando MusicPlayer...")
                musicPlayer.unbindService()
            } catch (e: Exception) {
                Log.e("MusicApp", "Error desvinculando servicio", e)
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text("🎵 Mi Música")
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { innerPadding ->
        PermissionHandler {
            MusicScreen(
                modifier = Modifier.padding(innerPadding),
                viewModel = viewModel
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MusicAppPreview() {
    Musica2Theme {
        // Para preview, usar un ViewModel dummy
        MusicAppPreviewContent()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicAppPreviewContent() {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text("🎵 Mi Música")
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { innerPadding ->
        // Contenido de preview simplificado
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = androidx.compose.ui.Alignment.Center
        ) {
            androidx.compose.foundation.layout.Column(
                horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
            ) {
                androidx.compose.material.icons.Icons
                androidx.compose.material3.Icon(
                    imageVector = androidx.compose.material.icons.Icons.Default.MusicNote,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Vista previa de la app",
                    style = MaterialTheme.typography.headlineSmall
                )
                androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "🎵 Jamendo Music Player",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}