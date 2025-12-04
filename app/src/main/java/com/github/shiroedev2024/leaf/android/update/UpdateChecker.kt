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
package com.github.shiroedev2024.leaf.android.update

import android.util.Log
import com.github.shiroedev2024.leaf.android.BuildConfig
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor

object UpdateChecker {
    private val client: OkHttpClient by lazy {
        val builder = OkHttpClient.Builder()

        if (BuildConfig.DEBUG) {
            val logging = HttpLoggingInterceptor { message -> Log.d("UpdateChecker", message) }
            logging.level = HttpLoggingInterceptor.Level.BODY
            builder.addInterceptor(logging)
        }

        builder.build()
    }

    private val gson = Gson()

    suspend fun fetchUpdate(
        arch: String,
        currentVersion: String,
        retries: Int = 3,
    ): UpdateResponse? {
        return withContext(Dispatchers.IO) {
            var attempt = 0
            var lastException: Exception? = null

            val url =
                "${if (BuildConfig.DEBUG) {UpdateConstants.BASE_URL_DEBUG} else {UpdateConstants.BASE_URL_RELEASE}}/downloads/android/${arch}/${currentVersion}"

            while (attempt < retries) {
                try {
                    val request = Request.Builder().url(url).get().build()
                    val response = client.newCall(request).execute()
                    response.use {
                        if (!it.isSuccessful) throw Exception("Unexpected HTTP ${it.code}")
                        val body = it.body.string()
                        return@withContext gson.fromJson(body, UpdateResponse::class.java)
                    }
                } catch (e: Exception) {
                    lastException = e
                    attempt++
                    // simple backoff
                    delay(500L * attempt)
                }
            }

            if (lastException != null) throw lastException
            null
        }
    }
}
