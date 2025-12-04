package com.github.shiroedev2024.leaf.android.activity

import android.content.*
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import com.github.shiroedev2024.leaf.android.BuildConfig
import com.github.shiroedev2024.leaf.android.R
import com.github.shiroedev2024.leaf.android.library.LeafException
import com.github.shiroedev2024.leaf.android.library.LeafVPNService
import com.github.shiroedev2024.leaf.android.library.ServiceManagement
import com.github.shiroedev2024.leaf.android.library.delegate.AssetsCallback
import com.github.shiroedev2024.leaf.android.model.AppPreferences
import com.github.shiroedev2024.leaf.android.screen.MainScreen
import com.github.shiroedev2024.leaf.android.ui.theme.LeafAndroidTheme
import com.github.shiroedev2024.leaf.android.viewmodel.LeafViewModel
import com.github.shiroedev2024.leaf.android.viewmodel.UpdateViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : BaseActivity() {

    private val leafViewModel: LeafViewModel by viewModels()
    private val updateViewModel: UpdateViewModel by viewModels()

    private val preferences: SharedPreferences by lazy {
        getSharedPreferences("app_preferences", Context.MODE_PRIVATE)
    }

    private val vpnRequestActivityResult: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                startVPN()
            } else {
                Toast.makeText(this, R.string.grant_vpn_permission, Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (
            ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.POST_NOTIFICATIONS,
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                requestNotificationPermission()
            }
        }

        enableEdgeToEdge()
        setContent {
            LeafAndroidTheme {
                MainScreen(
                    leafViewModel,
                    updateViewModel,
                    onUpdateLocale = { locale ->
                        MaterialAlertDialogBuilder(this)
                            .setTitle(R.string.update_locale)
                            .setMessage(
                                getString(R.string.update_locale_message, locale.displayLanguage)
                            )
                            .setPositiveButton(R.string.apply) { _, _ -> updateLocale(locale) }
                            .setNegativeButton(R.string.cancel, null)
                            .show()
                    },
                    onStartLeaf = { startVPN() },
                )
            }
        }

        leafViewModel.serviceState.observe(this) { serviceState ->
            if (serviceState is LeafViewModel.ServiceState.Connected) {
                if (ServiceManagement.getInstance().isServiceDead) return@observe

                val key = "first_run"
                val isFirstRun = preferences.getBoolean(key, true)
                if (isFirstRun) {
                    preferences.edit { putBoolean(key, false) }

                    val appPrefs = AppPreferences()
                    leafViewModel.setPreferences(appPrefs)
                }

                ServiceManagement.getInstance().autoUpdateSubscription()

                // update assets
                var appVersionName = BuildConfig.VERSION_NAME
                // check it has this regex \d+\.\d+\.\d+
                if (!appVersionName.matches(Regex("""\d+\.\d+\.\d+"""))) {
                    appVersionName = "0.0.0"
                }
                val parts = appVersionName.split(".")
                val major = parts[0].toInt()
                val minor = parts[1].toInt()
                val patch = parts[2].toInt()
                ServiceManagement.getInstance()
                    .updateAssets(
                        major,
                        minor,
                        patch,
                        object : AssetsCallback {
                            override fun onUpdateSuccess() {
                                Log.d("MainActivity", "Assets updated")
                            }

                            override fun onUpdateFailed(exception: LeafException?) {
                                Log.e("MainActivity", "Failed to update assets", exception)
                            }
                        },
                    )
            }
        }

        leafViewModel.leafState.observe(this) { leafState ->
            if (leafState is LeafViewModel.LeafState.Started) {
                CoroutineScope(Dispatchers.IO).launch {
                    delay(3000)
                    withContext(Dispatchers.Main) {
                        if (!ServiceManagement.getInstance().isServiceDead) {
                            ServiceManagement.getInstance().autoUpdateSubscription()
                        }
                    }
                }
            }
        }

        leafViewModel.initListeners()
    }

    override fun onResume() {
        super.onResume()

        updateViewModel.checkForUpdate(3)
    }

    private fun startVPN() {
        val startTime = System.nanoTime()

        val intent = LeafVPNService.prepare(this)
        if (intent != null) {
            vpnRequestActivityResult.launch(intent)
            return
        }

        if (!leafViewModel.checkFileChecksum()) {
            return
        }

        var endTime = System.nanoTime()
        Log.d("LeafAndroid", "Checking VPN permission: ${endTime - startTime}")
        leafViewModel.startLeaf()

        endTime = System.nanoTime()
        Log.d("LeafAndroid", "Starting VPN: ${endTime - startTime}")
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun requestNotificationPermission() {
        val requestPermissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
                permissions ->
                permissions.entries.forEach { permission ->
                    if (permission.key == android.Manifest.permission.POST_NOTIFICATIONS) {
                        if (!permission.value) {
                            Toast.makeText(
                                    this,
                                    R.string.grant_notification_permission,
                                    Toast.LENGTH_SHORT,
                                )
                                .show()
                        }
                    }
                }
            }
        requestPermissionLauncher.launch(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS))
    }
}
