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

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.core.app.NotificationCompat

class VpnEventReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "VpnEventReceiver"
        private const val CHANNEL_ID = "vpn_connectivity_channel"
        private const val NOTIFICATION_ID = 12002
        private const val EVENT_TYPE_KEY = "eventType"
        private const val DATA_KEY = "data"
        private const val TIMESTAMP_KEY = "timestamp"
        private const val EVENT_CONNECTIVITY_CHANGED = "connectivity_changed"
        private const val DATA_LOST = "lost"
        private const val DATA_RECOVERED = "recovered"
    }

    override fun onReceive(context: Context, intent: Intent) {
        try {
            val action = intent.action
            val senderPackage = intent.`package`

            Log.d(TAG, "Received broadcast action=$action fromPackage=$senderPackage")

            val eventType = intent.getStringExtra(EVENT_TYPE_KEY)
            val data = intent.getStringExtra(DATA_KEY)
            val timestamp = intent.getLongExtra(TIMESTAMP_KEY, 0L)

            Log.d(TAG, "eventType=$eventType, data=$data, timestamp=$timestamp")

            val extras: Bundle? = intent.extras
            extras?.keySet()?.forEach { key -> Log.d(TAG, "extra[$key]=${extras.get(key)}") }

            if (eventType == EVENT_CONNECTIVITY_CHANGED) {
                handleConnectivityChange(context, data)
            }
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to handle VPN event broadcast", t)
        }
    }

    private fun handleConnectivityChange(context: Context, data: String?) {
        try {
            when (data) {
                DATA_LOST -> showNetworkLostNotification(context)
                DATA_RECOVERED -> dismissNetworkLostNotification(context)
                else -> Log.w(TAG, "Unknown connectivity data: $data")
            }
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to handle connectivity change", t)
        }
    }

    private fun showNetworkLostNotification(context: Context) {
        try {
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            createNotificationChannel(context, notificationManager)

            val notification =
                NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.drawable.baseline_warning_24)
                    .setContentTitle(context.getString(R.string.network_lost_title))
                    .setContentText(context.getString(R.string.network_lost_message))
                    .setStyle(
                        NotificationCompat.BigTextStyle()
                            .bigText(context.getString(R.string.network_lost_message))
                    )
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setAutoCancel(false)
                    .setOngoing(true)
                    .build()

            notificationManager.notify(NOTIFICATION_ID, notification)
            Log.d(TAG, "Network lost notification shown")
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to show network lost notification", t)
        }
    }

    private fun dismissNetworkLostNotification(context: Context) {
        try {
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.cancel(NOTIFICATION_ID)
            Log.d(TAG, "Network lost notification dismissed")
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to dismiss network lost notification", t)
        }
    }

    private fun createNotificationChannel(
        context: Context,
        notificationManager: NotificationManager,
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val existingChannel = notificationManager.getNotificationChannel(CHANNEL_ID)
            if (existingChannel == null) {
                val channel =
                    NotificationChannel(
                        CHANNEL_ID,
                        context.getString(R.string.connectivity_channel_name),
                        NotificationManager.IMPORTANCE_HIGH,
                    )
                channel.description = context.getString(R.string.connectivity_channel_description)
                notificationManager.createNotificationChannel(channel)
                Log.d(TAG, "Notification channel created")
            }
        }
    }
}
