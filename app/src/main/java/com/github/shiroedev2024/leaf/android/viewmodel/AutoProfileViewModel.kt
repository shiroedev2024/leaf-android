package com.github.shiroedev2024.leaf.android.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class AutoProfileViewModel : ViewModel() {
    private var _profileId: MutableLiveData<String?> = MutableLiveData(null)
    val profileId: LiveData<String?> = _profileId

    fun updateProfile(url: String) {
        _profileId.value = url
    }
}
