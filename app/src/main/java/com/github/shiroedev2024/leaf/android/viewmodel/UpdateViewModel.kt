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
