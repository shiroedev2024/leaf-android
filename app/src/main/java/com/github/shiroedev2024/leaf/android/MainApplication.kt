package com.github.shiroedev2024.leaf.android

import android.app.Application
import com.github.shiroedev2024.leaf.android.library.Native

class MainApplication: Application() {

    override fun onCreate() {
        super.onCreate()

        Native.init()
    }

}