package com.github.shiroedev2024.leaf.android.activity

import android.content.Intent
import android.os.Bundle
import android.util.Base64
import android.util.Log
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.github.shiroedev2024.leaf.android.R
import com.github.shiroedev2024.leaf.android.screen.AutoProfileContent
import com.github.shiroedev2024.leaf.android.ui.theme.LeafAndroidTheme
import com.github.shiroedev2024.leaf.android.viewmodel.AutoProfileViewModel
import com.github.shiroedev2024.leaf.android.viewmodel.LeafViewModel

class AutoProfileActivity : BaseActivity() {

    private val leafViewModel: LeafViewModel by viewModels()
    private val autoProfileViewModel: AutoProfileViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            LeafAndroidTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = { TopAppBar(title = { Text(stringResource(R.string.auto_profile)) }) },
                ) { innerPadding ->
                    AutoProfileContent(
                        leafViewModel = leafViewModel,
                        autoProfileViewModel = autoProfileViewModel,
                        modifier = Modifier.padding(innerPadding),
                    )
                }
            }
        }

        leafViewModel.initListeners()

        handleDeepLink(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleDeepLink(intent)
    }

    private fun handleDeepLink(intent: Intent?) {
        intent?.data?.let { uri ->
            if (uri.scheme == "leafvpn" && uri.host == "install") {
                val profile = uri.getQueryParameter("profile")
                profile?.let { s ->
                    val decodedProfile = decodeBase64(s)
                    Log.d("AutoProfileActivity", "Decoded Profile: $decodedProfile")
                    autoProfileViewModel.updateProfile(decodedProfile)
                }
            }
        }
    }

    private fun decodeBase64(encodedString: String): String {
        return String(Base64.decode(encodedString, Base64.NO_WRAP))
    }
}
