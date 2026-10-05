package com.example

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.ui.screens.HomeScreen
import com.example.ui.theme.AudiophilesTheme
import com.example.ui.theme.CosmicBackground
import com.example.ui.viewmodel.SearchAndDownloadViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: SearchAndDownloadViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            AudiophilesTheme {
                var hasRequestedPermission by remember { mutableStateOf(false) }

                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestMultiplePermissions()
                ) { permissions ->
                    // Refrescar biblioteca tras conceder/denegar
                    viewModel.refreshLocalMusic()
                }

                LaunchedEffect(Unit) {
                    if (!hasRequestedPermission) {
                        hasRequestedPermission = true
                        val permissions = mutableListOf<String>()
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            permissions.add(Manifest.permission.READ_MEDIA_AUDIO)
                            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
                        }
                        permissionLauncher.launch(permissions.toTypedArray())
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = CosmicBackground
                ) {
                    HomeScreen(
                        viewModel = viewModel,
                        onRequestStoragePermission = {
                            val permissions = mutableListOf<String>()
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                permissions.add(Manifest.permission.READ_MEDIA_AUDIO)
                            } else {
                                permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
                            }
                            permissionLauncher.launch(permissions.toTypedArray())
                        }
                    )
                }
            }
        }
    }
}