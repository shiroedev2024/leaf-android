package com.github.shiroedev2024.leaf.android

import android.os.RemoteException
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.util.Log
import com.github.shiroedev2024.leaf.android.library.ServiceManagement
import com.github.shiroedev2024.leaf.android.library.delegate.LeafListener
import com.github.shiroedev2024.leaf.android.library.delegate.ServiceListener

class LeafVPNTileService : TileService() {

    private val leafListener =
        object : LeafListener {
            override fun onStartSuccess() {
                Log.d(TAG, "Service started")
                updateTileState()
            }

            override fun onStartFailed(message: String?) {
                Log.e(TAG, "Failed to start service: $message")
                updateTileState()
            }

            override fun onReloadSuccess() {
                Log.d(TAG, "Service reloaded")
            }

            override fun onReloadFailed(message: String?) {
                Log.e(TAG, "Failed to reload service: $message")
            }

            override fun onStopSuccess() {
                Log.d(TAG, "Service stopped")
                updateTileState()
            }

            override fun onStopFailed(message: String?) {
                Log.e(TAG, "Failed to stop service: $message")
                updateTileState()
            }
        }

    private val serviceListener =
        object : ServiceListener {
            override fun onConnect() {
                updateTileState()
            }

            override fun onDisconnect() {
                Log.d(TAG, "Service disconnected")
            }

            override fun onError(throwable: Throwable?) {
                Log.e(TAG, "Failed to connect to service", throwable)
            }
        }

    override fun onClick() {
        super.onClick()
        toggleVPN()
    }

    override fun onStartListening() {
        super.onStartListening()

        ServiceManagement.getInstance().addServiceListener(serviceListener)
        ServiceManagement.getInstance().addLeafListener(leafListener)

        ServiceManagement.getInstance().bindService(this)

        Log.d(TAG, "onStartListening")
    }

    override fun onStopListening() {
        super.onStopListening()

        ServiceManagement.getInstance().removeServiceListener(serviceListener)
        ServiceManagement.getInstance().removeLeafListener(leafListener)

        ServiceManagement.getInstance().unbindService(this)

        Log.d(TAG, "onStopListening")
    }

    private fun toggleVPN() {
        try {
            if (ServiceManagement.getInstance().isLeafRunning) {
                ServiceManagement.getInstance().stopLeaf()
            } else {
                ServiceManagement.getInstance().startLeaf("Leaf VPN")
            }
        } catch (e: RemoteException) {
            Log.e(TAG, "Failed to toggle VPN", e)
        }
    }

    private fun updateTileState() {
        try {
            val tile = qsTile
            if (ServiceManagement.getInstance().isLeafRunning) {
                tile.state = Tile.STATE_ACTIVE
                tile.label = getString(R.string.vpn_service_on)
            } else {
                tile.state = Tile.STATE_INACTIVE
                tile.label = getString(R.string.vpn_service_off)
            }
            tile.updateTile()
        } catch (e: RemoteException) {
            Log.e(TAG, "Failed to update tile state", e)
        }
    }

    companion object {
        private const val TAG = "LeafVPNTileService"
    }
}
