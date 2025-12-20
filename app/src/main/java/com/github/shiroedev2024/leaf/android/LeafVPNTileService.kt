/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 * Copyright (c) 2025 Shiroe Dev <shiroedev@proton.me>
 */
package com.github.shiroedev2024.leaf.android

import android.os.RemoteException
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.util.Log
import com.github.shiroedev2024.leaf.android.library.ServiceManagement
import com.github.shiroedev2024.leaf.android.library.delegate.ConnectivityChangeListener
import com.github.shiroedev2024.leaf.android.library.delegate.LeafListener
import com.github.shiroedev2024.leaf.android.library.delegate.ServiceListener

class LeafVPNTileService : TileService() {

    private val leafListener =
        object : LeafListener {
            override fun onStarting() {
                Log.d(TAG, "Service starting")
            }

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

    private val connectivityListener =
        object : ConnectivityChangeListener {
            override fun onConnectivityRecovered() {
                Log.d(TAG, "Connectivity recovered")
            }

            override fun onConnectivityLost() {
                Log.d(TAG, "Connectivity lost")
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
        ServiceManagement.getInstance().addConnectivityChangeListener(connectivityListener)

        ServiceManagement.getInstance().bindService(this)

        Log.d(TAG, "onStartListening")
    }

    override fun onStopListening() {
        super.onStopListening()

        ServiceManagement.getInstance().removeServiceListener(serviceListener)
        ServiceManagement.getInstance().removeLeafListener(leafListener)
        ServiceManagement.getInstance().removeConnectivityChangeListener(connectivityListener)

        ServiceManagement.getInstance().unbindService(this)

        Log.d(TAG, "onStopListening")
    }

    private fun toggleVPN() {
        try {
            if (ServiceManagement.getInstance().isLeafRunning) {
                ServiceManagement.getInstance().stopLeaf()
            } else {
                ServiceManagement.getInstance().startLeaf()
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
