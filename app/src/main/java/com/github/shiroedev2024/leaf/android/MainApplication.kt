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

import android.content.Context
import android.content.Intent
import android.util.Log
import com.github.shiroedev2024.leaf.android.activity.MainActivity
import com.github.shiroedev2024.leaf.android.library.ServiceManagement
import com.github.shiroedev2024.leaf.android.library.delegate.ServiceListener
import com.github.shiroedev2024.leaf.android.library.model.LeafConfig
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

        if (isMainProcess(appContext)) {
            Log.d("MainApplication", "Main process")

            ServiceManagement.getInstance()
                .addServiceListener(
                    object : ServiceListener {
                        override fun onConnect() {
                            Log.d("MainApplication", "Service connected")

                            val intent = Intent(getAppContext(), MainActivity::class.java)
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            val uri = intent.toUri(Intent.URI_INTENT_SCHEME)

                            val leafConfig =
                                LeafConfig.Builder()
                                    .setSessionName(MainApplication.getString(R.string.app_name))
                                    .setActivityIntentUri(uri)
                                    .setNotificationDetails(
                                        MainApplication.getString(R.string.notification_title),
                                        MainApplication.getString(R.string.notification_content),
                                        MainApplication.getString(R.string.notification_stop_button),
                                    )
                                    .build()

                            ServiceManagement.getInstance().setConfig(leafConfig)
                        }

                        override fun onDisconnect() {
                            Log.d("MainApplication", "Service disconnected")
                        }

                        override fun onError(throwable: Throwable?) {
                            Log.e("MainApplication", "Failed to connect to service", throwable)
                        }
                    }
                )
        } else {
            Log.d("MainApplication", "Not main process")
        }
    }
}
