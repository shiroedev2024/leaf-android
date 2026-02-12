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
package com.github.shiroedev2024.leaf.android.viewmodel

import android.net.Uri
import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.shiroedev2024.leaf.android.library.ApiClient
import com.github.shiroedev2024.leaf.android.library.LeafException
import com.github.shiroedev2024.leaf.android.library.ServiceManagement
import com.github.shiroedev2024.leaf.android.library.delegate.LeafListener
import com.github.shiroedev2024.leaf.android.library.delegate.ServiceListener
import com.github.shiroedev2024.leaf.android.library.delegate.SubscriptionCallback
import com.github.shiroedev2024.leaf.android.library.model.LeafPreferences
import com.github.shiroedev2024.leaf.android.library.model.OutboundInfo
import com.github.shiroedev2024.leaf.android.model.AppPreferences
import com.github.shiroedev2024.leaf.android.model.toUpdateLeafPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LeafViewModel : ViewModel() {
    private var _serviceState: MutableLiveData<ServiceState> = MutableLiveData(ServiceState.Loading)
    val serviceState: LiveData<ServiceState> = _serviceState

    private var _leafState: MutableLiveData<LeafState> = MutableLiveData(LeafState.Loading)
    val leafState: LiveData<LeafState> = _leafState

    private var _subscriptionState: MutableLiveData<SubscriptionState> =
        MutableLiveData(SubscriptionState.Initial)
    val subscriptionState: LiveData<SubscriptionState> = _subscriptionState

    private var _preferencesState: MutableLiveData<PreferencesState> =
        MutableLiveData(PreferencesState.Initial)
    val preferencesState: LiveData<PreferencesState> = _preferencesState

    private var _outboundState: MutableLiveData<OutboundState> =
        MutableLiveData(OutboundState.Initial)
    val outboundState: LiveData<OutboundState> = _outboundState

    private var _memoryLoggerState: MutableLiveData<MemoryLoggerState> =
        MutableLiveData(MemoryLoggerState.Initial)
    val memoryLoggerState: LiveData<MemoryLoggerState> = _memoryLoggerState

    private var _outbounds: MutableStateFlow<SnapshotStateList<OutboundInfo>> =
        MutableStateFlow(mutableStateListOf())
    val outbounds: StateFlow<SnapshotStateList<OutboundInfo>> = _outbounds

    private var _selectedSubgroupTag: MutableLiveData<String?> = MutableLiveData(null)
    val selectedSubgroupTag: LiveData<String?> = _selectedSubgroupTag

    private var _pingValues: MutableStateFlow<Map<String, Double?>> = MutableStateFlow(emptyMap())
    val pingValues: StateFlow<Map<String, Double?>> = _pingValues

    private var _memoryLogs: MutableStateFlow<SnapshotStateList<String>> =
        MutableStateFlow(mutableStateListOf())
    val memoryLogs: StateFlow<SnapshotStateList<String>> = _memoryLogs

    private var loggingJob: Job? = null
    private var pingJob: Job? = null
    private var autoPingJob: Job? = null
    private var leafApi: ApiClient? = null

    private var _isRefreshingPings: MutableLiveData<Boolean> = MutableLiveData(false)
    val isRefreshingPings: LiveData<Boolean> = _isRefreshingPings

    private val _pendingImportUri: MutableStateFlow<Uri?> = MutableStateFlow(null)
    val pendingImportUri: StateFlow<Uri?> = _pendingImportUri

    private val leafListener =
        object : LeafListener {
            override fun onStarting() {
                _leafState.value = LeafState.Loading
            }

            override fun onStartSuccess() {
                _leafState.value = LeafState.Started

                viewModelScope.launch {
                    _outboundState.value = OutboundState.Loading
                    getOutboundList()

                    val initialPings = _outbounds.value.associate { it.name to Double.NaN }
                    _pingValues.value = initialPings

                    autoPingJob?.cancel()
                    autoPingJob = launch {
                        delay(3000)
                        if (_outbounds.value.isNotEmpty()) {
                            refreshPings()
                        }
                    }
                }
            }

            override fun onStartFailed(message: String?) {
                _leafState.value = LeafState.Error(message.orEmpty())
            }

            override fun onReloadSuccess() {
                _leafState.value = LeafState.Reloaded

                getOutboundList()
                viewModelScope.launch {
                    delay(1000)
                    if (ServiceManagement.getInstance().isLeafRunning) {
                        _leafState.value = LeafState.Started
                    }
                }
            }

            override fun onReloadFailed(message: String?) {
                _leafState.value = LeafState.Error(message.orEmpty())

                viewModelScope.launch {
                    delay(1000)
                    if (ServiceManagement.getInstance().isLeafRunning) {
                        _leafState.value = LeafState.Started
                    }
                }
            }

            override fun onStopSuccess() {
                _leafState.value = LeafState.Stopped
                _outboundState.value = OutboundState.Initial

                pingJob?.cancel()
                autoPingJob?.cancel()
            }

            override fun onStopFailed(message: String?) {
                _leafState.value = LeafState.Error(message.orEmpty())
            }
        }

    private val serviceListener =
        object : ServiceListener {
            override fun onConnect() {
                getPreferences()

                if (ServiceManagement.getInstance().isLeafRunning) {
                    getOutboundList()
                    _leafState.value = LeafState.Started
                } else {
                    _outboundState.value = OutboundState.Initial
                    _leafState.value = LeafState.Stopped
                }

                _serviceState.value = ServiceState.Connected
            }

            override fun onDisconnect() {
                _serviceState.value = ServiceState.Disconnected

                _leafState.value = LeafState.Loading
                _outboundState.value = OutboundState.Initial
                _preferencesState.value = PreferencesState.Initial
                _outbounds.value.clear()
                _pingValues.value = emptyMap()

                stopLogger()
                leafApi = null

                // Cancel ping jobs
                pingJob?.cancel()
                autoPingJob?.cancel()
            }

            override fun onError(throwable: Throwable?) {
                _serviceState.value = ServiceState.Error(throwable?.message.orEmpty())
            }
        }

    private val subscriptionCallback =
        object : SubscriptionCallback {
            override fun onSubscriptionUpdating() {
                _subscriptionState.value = SubscriptionState.Fetching
            }

            override fun onSubscriptionSuccess() {
                _subscriptionState.value = SubscriptionState.Success
                getPreferences()
            }

            override fun onSubscriptionFailure(exception: LeafException) {
                _subscriptionState.value = SubscriptionState.Error(exception.message.orEmpty())
            }
        }

    fun initListeners() {
        ServiceManagement.getInstance().addLeafListener(leafListener)
        ServiceManagement.getInstance().addServiceListener(serviceListener)
    }

    override fun onCleared() {
        super.onCleared()

        ServiceManagement.getInstance().removeServiceListener(serviceListener)
        ServiceManagement.getInstance().removeLeafListener(leafListener)

        stopLogger()

        pingJob?.cancel()
        autoPingJob?.cancel()

        Log.d("LeafAndroid", "onCleared")
    }

    fun setPendingImportUri(uri: Uri?) {
        _pendingImportUri.value = uri
    }

    fun clearPendingImportUri() {
        _pendingImportUri.value = null
    }

    fun getOutboundList(tag: String? = null) {
        viewModelScope.launch {
            val currentTag = tag ?: _selectedSubgroupTag.value ?: "OUT"
            _outboundState.value = OutboundState.Loading

            try {
                val outboundItems =
                    withContext(Dispatchers.IO) { leafApi?.getSelectOutboundItems(currentTag) }
                val selectedResponse =
                    withContext(Dispatchers.IO) {
                        leafApi?.getCurrentSelectOutboundItem(currentTag)
                    }

                val selectedName = selectedResponse?.selected

                _outbounds.value.clear()
                outboundItems?.outbounds?.forEach { outbound ->
                    outbound.isSelected = outbound.name == selectedName
                    _outbounds.value.add(outbound)
                }

                _outboundState.value = OutboundState.Success
            } catch (e: Exception) {
                Log.e("LeafAndroid", "Error fetching outbound items for $currentTag: ${e.message}")
                _outboundState.value = OutboundState.Error(e.message.orEmpty())
            }
        }
    }

    fun setSubgroup(tag: String?) {
        _selectedSubgroupTag.value = tag
        getOutboundList(tag ?: "OUT")
    }

    fun changeSelectedOutbound(outbound: String) {
        viewModelScope.launch {
            val currentTag = _selectedSubgroupTag.value ?: "OUT"
            _outboundState.value = OutboundState.Loading

            try {
                withContext(Dispatchers.IO) { leafApi?.setSelectOutboundItem(currentTag, outbound) }
                Log.d("LeafAndroid", "Selected outbound changed to $outbound for tag $currentTag")

                // If we are in a subgroup, selecting a node should also set OUT to point to that
                // country
                if (currentTag != "OUT") {
                    withContext(Dispatchers.IO) {
                        leafApi?.setSelectOutboundItem("OUT", currentTag)
                    }
                }

                // Update local selection state
                _outbounds.value.forEach { it.isSelected = it.name == outbound }

                _outboundState.value = OutboundState.Success
            } catch (e: Exception) {
                Log.e("LeafAndroid", "Error changing selected outbound: ${e.message}")
                _outboundState.value = OutboundState.Error(e.message.orEmpty())
            }
        }
    }

    fun startLogger() {
        if (loggingJob?.isActive == true) {
            _memoryLoggerState.value = MemoryLoggerState.Error("Logger is already running")
            return
        }

        _memoryLoggerState.value = MemoryLoggerState.Loading

        loggingJob =
            viewModelScope.launch {
                while (true) {
                    try {
                        val logs =
                            withContext(Dispatchers.IO) {
                                leafApi
                                    ?.getLogs(200, _memoryLogs.value.size)
                                    ?.messages
                                    ?.filter { it.isNotBlank() }
                                    ?.map { it.trim() }
                            }

                        logs?.forEach { Log.d("LeafAndroid", it) }

                        _memoryLogs.value.addAll(logs.orEmpty())
                    } catch (e: Exception) {
                        _memoryLoggerState.value = MemoryLoggerState.Error(e.message.orEmpty())
                    }

                    delay(1000)
                }
            }

        Log.d("LeafAndroid", "Logger started")
        _memoryLoggerState.value = MemoryLoggerState.Started
    }

    fun clearLogs() {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) { leafApi?.clearLogs() }
                _memoryLogs.value.clear()
            } catch (e: Exception) {
                _memoryLoggerState.value = MemoryLoggerState.Error(e.message.orEmpty())
            }
        }
    }

    fun stopLogger() {
        _memoryLoggerState.value = MemoryLoggerState.Loading

        loggingJob?.cancel()
        _memoryLoggerState.value = MemoryLoggerState.Initial

        Log.d("LeafAndroid", "Logger stopped")
    }

    fun getPreferences() {
        val preferences = ServiceManagement.getInstance().preferences

        if (preferences != null) {
            _preferencesState.value = PreferencesState.Success(preferences)
            leafApi = ApiClient("http://127.0.0.1:${preferences.apiPort}")
        } else {
            _preferencesState.value = PreferencesState.Error("No preferences found")
        }
    }

    fun setPreferences(preferences: AppPreferences) {
        val updatePreferences = preferences.toUpdateLeafPreferences()

        ServiceManagement.getInstance().setPreferences(updatePreferences)
        getPreferences()

        if (ServiceManagement.getInstance().isLeafRunning) {
            _leafState.value = LeafState.Loading
            ServiceManagement.getInstance().reloadLeaf()
        }
    }

    fun startLeaf() {
        _leafState.value = LeafState.Loading
        ServiceManagement.getInstance().startLeaf()
    }

    fun stopLeaf() {
        _leafState.value = LeafState.Loading
        ServiceManagement.getInstance().stopLeaf()
    }

    fun updateSubscription(clientId: String) {
        _subscriptionState.value = SubscriptionState.Fetching
        ServiceManagement.getInstance().updateSubscription(clientId, subscriptionCallback)
    }

    fun importOfflineSubscription(path: String, passphrase: String?) {
        _subscriptionState.value = SubscriptionState.Fetching

        val keyIds = listOf("k1")
        val verifyingKeys = listOf("FjBD6zMrxVtHpWqzqsuFmT8uB7RZKmMuO94nT0N5LKo")

        try {
            ServiceManagement.getInstance()
                .importOfflineSubscription(
                    path,
                    passphrase,
                    keyIds,
                    verifyingKeys,
                    subscriptionCallback,
                )
        } catch (e: Exception) {
            _subscriptionState.value = SubscriptionState.Error(e.message.orEmpty())
        }
    }

    fun updateCustomSubscription(config: String) {
        _subscriptionState.value = SubscriptionState.Fetching
        ServiceManagement.getInstance().updateCustomSubscription(config, subscriptionCallback)
    }

    fun checkFileChecksum(): Boolean {
        try {
            ServiceManagement.getInstance().verifyFileIntegrity()
            Log.d("LeafAndroid", "File integrity verified")
            return true
        } catch (e: LeafException) {
            Log.e("LeafAndroid", "File integrity verification failed", e)
            _leafState.value = LeafState.Error(e.message ?: "Unknown error")
            return false
        }
    }

    fun refreshPings() {
        pingJob?.cancel()
        pingJob =
            viewModelScope.launch {
                _isRefreshingPings.value = true

                try {
                    val currentOutbounds = _outbounds.value.toList()
                    val initialPings =
                        currentOutbounds.associate { it.name to Double.NaN }.toMutableMap()
                    _pingValues.value = initialPings

                    // Ping each outbound in parallel
                    val childJobs =
                        currentOutbounds.map { outbound ->
                            launch {
                                val pingValue =
                                    try {
                                        val health =
                                            withContext(Dispatchers.IO) {
                                                leafApi?.getOutboundHealth(outbound.name)
                                            }

                                        // Use TCP first, fallback to UDP
                                        when {
                                            health?.tcpMs != null -> health.tcpMs
                                            health?.udpMs != null -> health.udpMs
                                            else -> null
                                        }
                                    } catch (e: Exception) {
                                        Log.e("LeafAndroid", "Failed to ping ${outbound.name}", e)
                                        null
                                    }

                                // Update map on main thread (which is the default for this scope)
                                val nextPings = _pingValues.value.toMutableMap()
                                nextPings[outbound.name] = pingValue
                                _pingValues.value = nextPings
                            }
                        }

                    // Wait for all child ping jobs to complete
                    childJobs.forEach { it.join() }
                } catch (e: Exception) {
                    Log.e("LeafAndroid", "Error in refreshPings", e)
                } finally {
                    // All child jobs have completed due to the join() call above
                    // The separate job below handles isRefreshingPings state coordination
                }
            }

        // We use a separate job to manage the refreshing state if we want it to be precise
        viewModelScope.launch {
            pingJob?.join()
            _isRefreshingPings.value = false
        }
    }

    fun pingOutbound(outboundName: String) {
        viewModelScope.launch {
            // Set to loading
            val currentPings = _pingValues.value.toMutableMap()
            currentPings[outboundName] = Double.NaN
            _pingValues.value = currentPings

            try {
                val health =
                    withContext(Dispatchers.IO) { leafApi?.getOutboundHealth(outboundName) }

                val pingValue =
                    when {
                        health?.tcpMs != null -> health.tcpMs
                        health?.udpMs != null -> health.udpMs
                        else -> null
                    }

                val finalPings = _pingValues.value.toMutableMap()
                finalPings[outboundName] = pingValue
                _pingValues.value = finalPings
            } catch (e: Exception) {
                Log.e("LeafAndroid", "Failed to ping $outboundName", e)

                val finalPings = _pingValues.value.toMutableMap()
                finalPings[outboundName] = null
                _pingValues.value = finalPings
            }
        }
    }

    // MemoryLoggerState
    sealed class MemoryLoggerState {
        data object Initial : MemoryLoggerState()

        data object Loading : MemoryLoggerState()

        data object Started : MemoryLoggerState()

        data class Error(val error: String) : MemoryLoggerState()
    }

    // PreferencesState
    sealed class PreferencesState {
        data object Initial : PreferencesState()

        data class Success(val preferences: LeafPreferences) : PreferencesState()

        data class Error(val error: String) : PreferencesState()
    }

    // OutboundState
    sealed class OutboundState {
        data object Initial : OutboundState()

        data object Loading : OutboundState()

        data object Success : OutboundState()

        data class Error(val error: String) : OutboundState()
    }

    // SubscriptionState
    sealed class SubscriptionState {
        data object Initial : SubscriptionState()

        data object Fetching : SubscriptionState()

        data object Success : SubscriptionState()

        data class Error(val error: String) : SubscriptionState()
    }

    // ServiceState
    sealed class ServiceState {
        data object Loading : ServiceState()

        data object Connected : ServiceState()

        data object Disconnected : ServiceState()

        data class Error(val error: String) : ServiceState()
    }

    // LeafState
    sealed class LeafState {
        data object Loading : LeafState()

        data object Started : LeafState()

        data object Stopped : LeafState()

        data object Reloaded : LeafState()

        data class Error(val error: String) : LeafState()
    }
}
