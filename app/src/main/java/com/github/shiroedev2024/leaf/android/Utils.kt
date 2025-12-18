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

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.Process
import android.util.Log
import com.neovisionaries.i18n.CountryCode
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.concurrent.TimeUnit

object Utils {
    fun getRelativeTime(date: Date): String {
        val diffInMillis = Date().time - date.time

        if (diffInMillis < 0) {
            return "In the future"
        }

        val diffInSeconds = TimeUnit.MILLISECONDS.toSeconds(diffInMillis)
        val diffInMinutes = TimeUnit.MILLISECONDS.toMinutes(diffInMillis)
        val diffInHours = TimeUnit.MILLISECONDS.toHours(diffInMillis)
        val diffInDays = TimeUnit.MILLISECONDS.toDays(diffInMillis)

        return when {
            diffInSeconds < 60 -> MainApplication.getString(R.string.just_now)
            diffInMinutes == 1L -> MainApplication.getString(R.string.an_minute_ago)
            diffInMinutes < 60 -> MainApplication.getString(R.string.x_minutes_ago, diffInMinutes)
            diffInHours == 1L -> MainApplication.getString(R.string.an_hour_ago)
            diffInHours < 24 -> MainApplication.getString(R.string.x_hours_ago, diffInHours)
            diffInDays == 1L -> MainApplication.getString(R.string.yesterday)
            else -> MainApplication.getString(R.string.x_days_ago, diffInDays)
        }
    }

    fun formatBytes(bytes: Long): String {
        if (bytes <= 0) return "0 B"

        val units = listOf("B", "KB", "MB", "GB", "TB")
        var value = bytes.toDouble()

        for (i in units.indices) {
            if (i < units.size - 1) {
                val nextUnitValue = value / 1024
                if (nextUnitValue < 1) {
                    return String.format(Locale.ENGLISH, "%.2f %s", value, units[i])
                }
                value = nextUnitValue
            } else {
                return String.format(Locale.ENGLISH, "%.2f %s", value, units[i])
            }
        }

        return String.format(Locale.ENGLISH, "%.2f %s", value, units.last())
    }

    fun createCustomUserAgent(): String {
        val version: String = BuildConfig.VERSION_NAME
        val os = "Android " + Build.VERSION.RELEASE
        val manufacturer = Build.MANUFACTURER
        var model = Build.MODEL

        if (!model.startsWith(manufacturer)) {
            model = "$manufacturer $model"
        }

        return String.format("%s/%s (%s; %s)", "Leaf", version, os, model)
    }

    fun getCountryInfo(isoCode: String): String {
        if (isoCode == "AUTO") {
            return "\uD83C\uDF10 Auto"
        }

        val countryCode = CountryCode.getByCode(isoCode.uppercase(Locale.ENGLISH))
        return if (countryCode != null) {
            val flag =
                isoCode
                    .uppercase(Locale.ENGLISH)
                    .map { String(Character.toChars(0x1F1E6 + (it - 'A'))) }
                    .joinToString("")
            val countryName = countryCode.getName()
            "$flag $countryName"
        } else {
            "Invalid ISO code"
        }
    }

    /**
     * Determine the installed APK ABI by parsing the app's versionCode which encodes ABI.
     * VersionCode is formed as (abiId * 1_000_000) + baseVersionCode in the Gradle config. Mapping:
     * 1 -> armeabi-v7a, 2 -> arm64-v8a, 3 -> x86, 4 -> x86_64, 5 -> all
     */
    fun getInstalledAbi(context: Context): String {
        try {
            val pm = context.packageManager
            val pi = pm.getPackageInfo(context.packageName, 0)
            val installedCode =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    pi.longVersionCode
                } else {
                    @Suppress("DEPRECATION") pi.versionCode.toLong()
                }
            Log.d("Utils", "Installed version code: $installedCode")

            val abiId = (installedCode / 1_000_000L).toInt()
            return when (abiId) {
                1 -> "armeabi-v7a"
                2 -> "arm64-v8a"
                3 -> "x86"
                4 -> "x86_64"
                5 -> "all"
                else -> throw Exception("Unknown ABI ID")
            }
        } catch (e: Exception) {
            Log.e("Utils", "Failed to determine installed ABI", e)
            return "all"
        }
    }

    /**
     * Compares two version strings to determine if the latest version is newer than the current
     * version. Versions are expected to be in semantic versioning format (e.g., "1.2.3"), separated
     * by dots. If the versions are equal or the latest is null/older, returns false.
     *
     * @param latest the latest version string, can be null
     * @param current the current version string, not null
     * @return true if latest is newer than current, false otherwise
     */
    fun isVersionNewer(latest: String?, current: String): Boolean {
        if (latest == null) return false
        try {
            val lp = latest.split('.').map { it.toIntOrNull() ?: 0 }
            val cp = current.split('.').map { it.toIntOrNull() ?: 0 }
            for (i in 0 until maxOf(lp.size, cp.size)) {
                val lv = lp.getOrElse(i) { 0 }
                val cv = cp.getOrElse(i) { 0 }
                if (lv > cv) return true
                if (lv < cv) return false
            }
        } catch (e: Exception) {
            Log.e("Utils", "Failed to compare versions", e)
            return false
        }
        return false
    }

    /**
     * Validates whether the given string is a valid UUID, specifically a UUID version 4.
     *
     * This method first attempts to parse the string using [UUID.fromString]. If parsing succeeds,
     * it then checks if the string matches the regex pattern for a UUID version 4, which includes
     * the version bit set to 4 and the variant bits set to 8, 9, A, or B.
     *
     * @param uuid the string to validate as a UUID
     * @return true if the string is a valid UUID version 4, false otherwise
     */
    fun isValidUUID(uuid: String): Boolean {
        return try {
            UUID.fromString(uuid)
            uuid.matches(
                Regex(
                    "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-4[0-9a-fA-F]{3}-[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}$"
                )
            )
        } catch (e: IllegalArgumentException) {
            false
        }
    }
}

/**
 * Determines if the current process is the main application process.
 *
 * This function retrieves the current process ID using [Process.myPid] and uses the
 * [ActivityManager] to inspect all running app processes. It checks if the process with the
 * matching PID has a process name equal to the application's package name, indicating it is the
 * main process. If no matching process is found, it defaults to assuming it is the main process.
 *
 * Note: This relies on [ActivityManager.getRunningAppProcesses], which may have privacy
 * restrictions on newer Android versions (API 29+), but is suitable for internal app process
 * detection.
 *
 * @param context The application context, used to retrieve the [ActivityManager] service and
 *   package name.
 * @return `true` if the current process is the main process, `false` otherwise.
 */
fun isMainProcess(context: Context): Boolean {
    val myPid = Process.myPid()
    val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    for (processInfo in activityManager.runningAppProcesses ?: emptyList()) {
        if (processInfo.pid == myPid) {
            return processInfo.processName == context.packageName
        }
    }
    return true
}
