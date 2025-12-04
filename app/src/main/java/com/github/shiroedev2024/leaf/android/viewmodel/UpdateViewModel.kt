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

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.shiroedev2024.leaf.android.BuildConfig
import com.github.shiroedev2024.leaf.android.MainApplication
import com.github.shiroedev2024.leaf.android.Utils
import com.github.shiroedev2024.leaf.android.Utils.isVersionNewer
import com.github.shiroedev2024.leaf.android.update.UpdateChecker
import com.github.shiroedev2024.leaf.android.update.UpdateResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class UpdateViewModel : ViewModel() {
    private var _updateState: MutableLiveData<UpdateState> = MutableLiveData(UpdateState.Initial)
    val updateState: LiveData<UpdateState> = _updateState

    fun checkForUpdate(retries: Int = 3) {
        viewModelScope.launch {
            _updateState.value = UpdateState.Checking

            // determine arch from installed APK versionCode (safer than device ABI)
            val arch = Utils.getInstalledAbi(MainApplication.getAppContext())

            val currentVersion = BuildConfig.VERSION_NAME

            try {
                val resp: UpdateResponse? =
                    withContext(Dispatchers.IO) {
                        UpdateChecker.fetchUpdate(arch, currentVersion, retries)
                    }

                if (resp != null && resp.available) {
                    val latest = resp.latestVersionName
                    if (isVersionNewer(latest, currentVersion)) {
                        _updateState.value = UpdateState.Available(resp)
                    } else {
                        _updateState.value = UpdateState.NotAvailable
                    }
                } else {
                    _updateState.value = UpdateState.NotAvailable
                }
            } catch (e: Exception) {
                _updateState.value = UpdateState.Error(e.message.orEmpty())
            }
        }
    }

    fun checkForUpdateManual(retries: Int = 3) {
        viewModelScope.launch {
            _updateState.value = UpdateState.ManualChecking

            // determine arch from installed APK versionCode (safer than device ABI)
            val arch = Utils.getInstalledAbi(MainApplication.getAppContext())

            val currentVersion = BuildConfig.VERSION_NAME

            try {
                val resp: UpdateResponse? =
                    withContext(Dispatchers.IO) {
                        UpdateChecker.fetchUpdate(arch, currentVersion, retries)
                    }

                if (resp != null && resp.available) {
                    val latest = resp.latestVersionName
                    if (isVersionNewer(latest, currentVersion)) {
                        _updateState.value = UpdateState.Available(resp)
                    } else {
                        _updateState.value = UpdateState.NotAvailable
                    }
                } else {
                    _updateState.value = UpdateState.NotAvailable
                }
            } catch (e: Exception) {
                _updateState.value = UpdateState.Error(e.message.orEmpty())
            }
        }
    }

    sealed class UpdateState {
        object Initial : UpdateState()

        object Checking : UpdateState()

        object ManualChecking : UpdateState()

        data class Available(val info: UpdateResponse) : UpdateState()

        object NotAvailable : UpdateState()

        data class Error(val error: String) : UpdateState()
    }
}
