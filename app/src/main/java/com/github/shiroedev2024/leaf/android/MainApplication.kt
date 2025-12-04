package com.github.shiroedev2024.leaf.android

import android.content.Context
import com.zeugmasolutions.localehelper.LocaleAwareApplication

class MainApplication : LocaleAwareApplication() {
    companion object {
        private lateinit var appContext: Context

        fun getString(resId: Int, vararg formatArgs: Any): String {
            return appContext.getString(resId, *formatArgs)
        }

        fun getAppContext(): Context {
            return appContext
        }

        val languages =
            mapOf(
                "en" to "English",
                "fa" to "فارسی",
                "ar" to "العربية",
                "es" to "Español",
                "in" to "Bahasa Indonesia",
                "ru" to "Русский",
                "tr" to "Türkçe",
                "uz" to "O'zbek",
            )
    }

    override fun onCreate() {
        super.onCreate()

        appContext = applicationContext
    }
}
