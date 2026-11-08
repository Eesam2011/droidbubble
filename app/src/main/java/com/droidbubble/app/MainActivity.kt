package com.droidbubble.app

import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.droidbubble.app.data.SettingsRepository
import com.droidbubble.app.service.IslandOverlayService
import com.droidbubble.app.theme.DroidbubbleTheme
import com.droidbubble.app.ui.main.MainScreen
import com.droidbubble.app.ui.main.MainScreenViewModel
import com.droidbubble.app.ui.main.PermissionHelper
import android.content.Intent
class MainActivity : ComponentActivity() {

    private companion object {
        const val REQUEST_BLUETOOTH_CONNECT = 4201
    }

    private val viewModel by viewModels<MainScreenViewModel>(
        factoryProducer = {
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return MainScreenViewModel(SettingsRepository(applicationContext)) as T
                }
            }
        }
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        com.droidbubble.app.util.CrashLog.install(this)
        enableEdgeToEdge()

        viewModel.checkPermissions(this)
        if (PermissionHelper.isOverlayGranted(this)) {
            IslandOverlayService.start(this)
        }
        // Android 12+ only delivers Bluetooth connection broadcasts to apps holding
        // BLUETOOTH_CONNECT; without asking for it the Bluetooth alert never fired.
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S &&
            checkSelfPermission(android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(android.Manifest.permission.BLUETOOTH_CONNECT), REQUEST_BLUETOOTH_CONNECT)
        }
        if (intent.getBooleanExtra("EXTRA_SHOW_UPDATE", false)) {
            viewModel.checkForUpdates(this)
        }

        setContent {
            DroidbubbleTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(viewModel = viewModel)
                }
            }
        }
    }


    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.getBooleanExtra("EXTRA_SHOW_UPDATE", false)) {
            viewModel.checkForUpdates(this)
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.checkPermissions(this)
        if (PermissionHelper.isOverlayGranted(this)) {
            IslandOverlayService.start(this)
        }
    }
}
