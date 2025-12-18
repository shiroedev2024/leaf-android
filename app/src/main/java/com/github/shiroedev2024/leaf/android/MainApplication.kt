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
import android.util.Log
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
        } else {
            Log.d("MainApplication", "Not main process")
        }
    }
}
