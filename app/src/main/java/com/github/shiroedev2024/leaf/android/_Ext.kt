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

import androidx.compose.ui.graphics.Color
import com.github.shiroedev2024.leaf.android.ui.theme.CardInfoGreen
import com.github.shiroedev2024.leaf.android.ui.theme.CardInfoRed
import com.github.shiroedev2024.leaf.android.ui.theme.CardInfoYellow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun Long?.parseAsLastUpdatedTime(): String {
    val lastUpdatedTime = this ?: 0L
    return if (lastUpdatedTime > 0L) {
        Utils.getRelativeTime(Date(lastUpdatedTime * 1000L))
    } else {
        MainApplication.getString(R.string.never)
    }
}

fun getRemainingTraffic(traffic: Long, used: Long): String {
    return if (traffic > 0) {
        Utils.formatBytes(traffic - used)
    } else if (traffic == 0L) {
        "∞"
    } else {
        "N/A"
    }
}

fun getRemainingTrafficTextColor(traffic: Long, used: Long): Color {
    if (traffic == 0L) {
        return CardInfoGreen
    }

    val remaining = traffic - used
    return when {
        remaining <= 0 -> CardInfoRed
        remaining <= traffic * 0.1 -> CardInfoYellow
        else -> CardInfoGreen
    }
}

fun String?.getExpirationTextColor(): Color {
    val date =
        if (this == null) {
            0
        } else {
            parseDateToMillis(this)
        }
    val now = Date().time
    val oneWeek = 7 * 24 * 60 * 60 * 1000
    val diff = date - now

    return when {
        diff <= 0 -> CardInfoRed
        diff <= oneWeek -> CardInfoYellow
        else -> CardInfoGreen
    }
}

fun parseDateToMillis(dateString: String): Long {
    val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    val date = format.parse(dateString)
    return date?.time ?: 0L
}

fun String.parseAsCountryInfo(): String {
    return Utils.getCountryInfo(this)
}

fun String.getLogColor(): Color {
    val log: String = this
    return when {
        log.contains("ERROR") -> Color(0xFFF87171) // text-red-400
        log.contains("WARN") -> Color(0xFFFACC15) // text-yellow-400
        log.contains("INFO") -> Color(0xFF60A5FA) // text-blue-400
        log.contains("DEBUG") -> Color(0xFF34D399) // text-green-400
        log.contains("TRACE") -> Color(0xFF9CA3AF) // text-gray-400
        else -> Color(0xFFD1D5DB) // text-gray-300
    }
}
