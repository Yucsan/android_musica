package com.yucsan.musica2.ui.screens

import android.Manifest
import android.os.Build
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.*

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PermissionHandler(
    content: @Composable () -> Unit
) {
    // Lista de permisos necesarios según la versión de Android
    val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        listOf(
            Manifest.permission.READ_MEDIA_AUDIO,
            Manifest.permission.POST_NOTIFICATIONS
        )
    } else {
        listOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    val multiplePermissionsState = rememberMultiplePermissionsState(permissions)

    when {
        multiplePermissionsState.allPermissionsGranted -> {
            // Todos los permisos concedidos, mostrar contenido
            content()
        }
        multiplePermissionsState.shouldShowRationale -> {
            // Mostrar explicación de por qué necesitamos los permisos
            PermissionRationaleDialog(
                onRequestPermission = { multiplePermissionsState.launchMultiplePermissionRequest() },
                onDismiss = { /* Manejar cuando el usuario cancela */ }
            )
        }
        else -> {
            // Primera vez o permisos denegados permanentemente
            PermissionRequestScreen(
                onRequestPermission = { multiplePermissionsState.launchMultiplePermissionRequest() }
            )
        }
    }
}

@Composable
private fun PermissionRequestScreen(
    onRequestPermission: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.MusicNote,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Acceso a tu música",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Para reproducir tu música, necesitamos acceso a tus archivos de audio y poder mostrar notificaciones cuando la música esté reproduciéndose.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onRequestPermission,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Conceder permisos")
        }
    }
}

@Composable
private fun PermissionRationaleDialog(
    onRequestPermission: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Permisos necesarios") },
        text = {
            Text("Esta aplicación necesita acceso a tus archivos de música y permisos de notificación para funcionar correctamente.")
        },
        confirmButton = {
            TextButton(onClick = onRequestPermission) {
                Text("Conceder")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}