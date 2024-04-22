package com.github.shiroedev2024.leaf.android

import android.content.*
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import com.github.shiroedev2024.leaf.android.library.IMyAidlInterface
import com.github.shiroedev2024.leaf.android.library.LeafVPNService
import com.github.shiroedev2024.leaf.android.ui.theme.LeafAndroidTheme

class MainActivity : ComponentActivity() {

    private var myAidlInterface: IMyAidlInterface? = null

    private val serviceConnection = object: ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            myAidlInterface = IMyAidlInterface.Stub.asInterface(service)

            Log.i("LeafAndroid", "onServiceConnected")
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            myAidlInterface = null

            Log.i("LeafAndroid", "onServiceDisconnected")
        }
    }

    private val broadcastReceiver = object: BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            Log.i("LeafAndroid", "onReceive")
            val action = intent?.action
            if (action == "$packageName.ACTION_DATA_FROM_SERVICE_TO_ACTIVITY") {
                val data = intent?.getStringExtra("data")
                Log.i("LeafAndroid", "data: $data")

                when (data) {
                    "started" -> {

                    }
                    "stopped" -> {

                    }
                    "reloaded" -> {
                    }
                    "permission_error" -> {

                    }
                }
            }
        }
    }

    private val vpnRequestActivityResult: ActivityResultLauncher<Intent> = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            toggleVpnService(LeafVPNService.ACTION_START)
        } else {
            Toast.makeText(this, R.string.grant_vpn_permission, Toast.LENGTH_SHORT).show()
        }
    }

    fun isServiceRunning(): Boolean {
        return myAidlInterface?.isRunning() ?: false
    }

    fun reloadLeaf() {
        myAidlInterface?.reload()
    }

    fun stopLeaf() {
        myAidlInterface?.stop()
    }

    private fun startVPN() {
        val intent = LeafVPNService.prepare(this)
        if (intent != null) {
            vpnRequestActivityResult.launch(intent)
        } else {
            toggleVpnService(LeafVPNService.ACTION_START)
        }
    }

    private fun toggleVpnService(action: String) {
        val intent = Intent(this, LeafVPNService::class.java)
        intent.setAction(action)
        ContextCompat.startForegroundService(this, intent)
    }

    override fun onStart() {
        super.onStart()

        val intent = Intent(this, LeafVPNService::class.java)
        val ret = bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        Log.d("LeafAndroid", "bindService: $ret")

        val filter = IntentFilter("$packageName.ACTION_DATA_FROM_SERVICE_TO_ACTIVITY")
        registerReceiver(broadcastReceiver, filter)
    }

    override fun onStop() {
        super.onStop()

        unbindService(serviceConnection)
        Log.d("LeafAndroid", "unbindService")
        unregisterReceiver(broadcastReceiver)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LeafAndroidTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    LeafVPNComponent(onToggleVpn = { if (isServiceRunning()) stopLeaf() else startVPN() }, modifier = Modifier)
                }
            }
        }
    }
}

@Composable
fun LeafVPNComponent(onToggleVpn: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onToggleVpn,
        modifier = modifier
    ) {
        Text(text = "Toggle")
    }
}
