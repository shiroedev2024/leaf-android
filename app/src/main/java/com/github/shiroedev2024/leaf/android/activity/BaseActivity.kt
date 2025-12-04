package com.github.shiroedev2024.leaf.android.activity

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import com.github.shiroedev2024.leaf.android.library.ServiceManagement
import com.zeugmasolutions.localehelper.LocaleHelper
import com.zeugmasolutions.localehelper.LocaleHelperActivityDelegate
import com.zeugmasolutions.localehelper.LocaleHelperActivityDelegateImpl
import java.util.Locale

open class BaseActivity : ComponentActivity() {
    private val localeDelegate: LocaleHelperActivityDelegate = LocaleHelperActivityDelegateImpl()

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(localeDelegate.attachBaseContext(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        localeDelegate.onCreate(this)
    }

    override fun onResume() {
        super.onResume()
        localeDelegate.onResumed(this)
    }

    override fun onPause() {
        super.onPause()
        localeDelegate.onPaused()
    }

    override fun onStart() {
        super.onStart()
        ServiceManagement.getInstance().bindService(this)
    }

    override fun onStop() {
        super.onStop()
        ServiceManagement.getInstance().unbindService(this)
    }

    override fun createConfigurationContext(overrideConfiguration: Configuration): Context {
        val context = super.createConfigurationContext(overrideConfiguration)
        return LocaleHelper.onAttach(context)
    }

    override fun getApplicationContext(): Context =
        localeDelegate.getApplicationContext(super.getApplicationContext())

    open fun updateLocale(locale: Locale) {
        localeDelegate.setLocale(this, locale)
    }
}
