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
