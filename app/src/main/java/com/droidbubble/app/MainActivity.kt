package com.droidbubble.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.droidbubble.app.data.SettingsRepository
import com.droidbubble.app.service.IslandOverlayService
import com.droidbubble.app.theme.DroidbubbleTheme
import com.droidbubble.app.ui.main.LicenseActivationScreen
import com.droidbubble.app.ui.main.MainScreen
import com.droidbubble.app.ui.main.MainScreenViewModel
import com.droidbubble.app.ui.main.PermissionHelper
import com.droidbubble.app.util.DynamicFeatureEngine
import com.droidbubble.app.util.LicenseManager
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

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
        enableEdgeToEdge()

        // Khởi chạy Engine cập nhật tính năng & vá lỗi trực tuyến (OTA Live Patch)
        DynamicFeatureEngine.start(applicationContext)

        viewModel.checkPermissions(this)
        if (LicenseManager.isActivated(this) && PermissionHelper.isOverlayGranted(this)) {
            IslandOverlayService.start(this)
        }
        // Update check disabled per user request

        setContent {
            val settings by viewModel.settings.collectAsState()
            androidx.compose.runtime.LaunchedEffect(settings.hideStatusBar) {
                val windowInsetsController = androidx.core.view.WindowCompat.getInsetsController(window, window.decorView)
                if (settings.hideStatusBar) {
                    windowInsetsController.systemBarsBehavior =
                        androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                    windowInsetsController.hide(androidx.core.view.WindowInsetsCompat.Type.statusBars())
                } else {
                    windowInsetsController.show(androidx.core.view.WindowInsetsCompat.Type.statusBars())
                }
                com.droidbubble.app.service.NativeIslandSuppressor.applyStatusBarVisibility(this@MainActivity, settings.hideStatusBar)
            }
            DroidbubbleTheme(
                themeMode = settings.appThemeMode,
                accentColorKey = settings.accentColorKey
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var isLicenseActivated by remember {
                        mutableStateOf(LicenseManager.isActivated(this@MainActivity))
                    }

                    if (!isLicenseActivated) {
                        LicenseActivationScreen(
                            onActivated = {
                                isLicenseActivated = true
                                DynamicFeatureEngine.syncNow(applicationContext)
                                if (PermissionHelper.isOverlayGranted(this@MainActivity)) {
                                    IslandOverlayService.start(this@MainActivity)
                                }
                                lifecycleScope.launch {
                                    LicenseManager.syncDeviceTelemetry(this@MainActivity)
                                }
                            }
                        )
                    } else {
                        MainScreen(
                            viewModel = viewModel,
                            onDeactivateLicense = {
                                LicenseManager.deactivate(this@MainActivity)
                                IslandOverlayService.stop(this@MainActivity)
                                isLicenseActivated = false
                            }
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        // Update check disabled per user request
    }

    override fun onResume() {
        super.onResume()
        viewModel.checkPermissions(this)
        DynamicFeatureEngine.syncNow(applicationContext)
        if (LicenseManager.isActivated(this)) {
            if (PermissionHelper.isOverlayGranted(this)) {
                IslandOverlayService.start(this)
            }
            lifecycleScope.launch {
                LicenseManager.syncDeviceTelemetry(this@MainActivity)
            }
        } else {
            IslandOverlayService.stop(this)
        }
    }
}
