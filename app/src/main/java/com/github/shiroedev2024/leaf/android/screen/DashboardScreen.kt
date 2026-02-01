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
package com.github.shiroedev2024.leaf.android.screen

import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.github.shiroedev2024.leaf.android.R
import com.github.shiroedev2024.leaf.android.component.ConnectionButton
import com.github.shiroedev2024.leaf.android.component.MessageComponent
import com.github.shiroedev2024.leaf.android.component.MessageType
import com.github.shiroedev2024.leaf.android.component.OutboundsScreen
import com.github.shiroedev2024.leaf.android.component.UpdateScreen
import com.github.shiroedev2024.leaf.android.library.ServiceManagement
import com.github.shiroedev2024.leaf.android.parseDateToMillis
import com.github.shiroedev2024.leaf.android.update.UpdateResponse
import com.github.shiroedev2024.leaf.android.viewmodel.LeafViewModel
import com.github.shiroedev2024.leaf.android.viewmodel.UpdateViewModel

@Composable
fun DashboardContent(
    leafViewModel: LeafViewModel,
    updateViewModel: UpdateViewModel,
    modifier: Modifier = Modifier,
    onStartLeaf: () -> Unit,
    onNavigateToProfile: () -> Unit,
) {
    val context = LocalContext.current

    val serviceState by leafViewModel.serviceState.observeAsState()
    val leafState by leafViewModel.leafState.observeAsState()
    val preferencesState by leafViewModel.preferencesState.observeAsState()
    val updateState by
        updateViewModel.updateState.observeAsState(UpdateViewModel.UpdateState.Initial)
    var showUpdateDialog by remember { mutableStateOf(false) }
    var availableUpdate by remember { mutableStateOf<UpdateResponse?>(null) }
    var selectedSource by remember { mutableStateOf("") }

    LaunchedEffect(leafState) {
        if (leafState is LeafViewModel.LeafState.Stopped) {
            Toast.makeText(context, R.string.vpn_service_off, Toast.LENGTH_SHORT).show()
        } else if (leafState is LeafViewModel.LeafState.Reloaded) {
            Toast.makeText(context, R.string.vpn_service_reload, Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(updateState) {
        if (updateState is UpdateViewModel.UpdateState.Available) {
            availableUpdate = (updateState as UpdateViewModel.UpdateState.Available).info
        }
    }

    Box(modifier = modifier.fillMaxSize().padding(16.dp)) {
        when (serviceState) {
            LeafViewModel.ServiceState.Connected -> {
                when (preferencesState) {
                    is LeafViewModel.PreferencesState.Error -> {
                        MessageComponent(
                            type = MessageType.ERROR,
                            message =
                                (preferencesState as LeafViewModel.PreferencesState.Error).error,
                        )
                    }
                    LeafViewModel.PreferencesState.Initial -> {
                        MessageComponent(
                            type = MessageType.INFO,
                            message = stringResource(R.string.initial),
                        )
                    }
                    is LeafViewModel.PreferencesState.Success -> {
                        val preferences =
                            (preferencesState as LeafViewModel.PreferencesState.Success).preferences

                        Column(modifier = Modifier.fillMaxSize()) {
                            // handle auto-update
                            if (availableUpdate != null) {
                                val info = availableUpdate!!
                                val label =
                                    context.getString(
                                        R.string.update_new_version,
                                        info.latestVersionName,
                                    )
                                Spacer(modifier = Modifier.height(8.dp))
                                MessageComponent(
                                    type = MessageType.INFO,
                                    message = label,
                                    actionLabel = context.getString(R.string.update_details),
                                    action = { showUpdateDialog = true },
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                if (showUpdateDialog) {
                                    UpdateScreen(
                                        info = info,
                                        onDismiss = { showUpdateDialog = false },
                                        onOpenUrl = { urlToOpen ->
                                            val url =
                                                urlToOpen.ifEmpty {
                                                    info.downloadSources.firstOrNull()?.url
                                                }
                                            if (!url.isNullOrEmpty()) {
                                                val intent = Intent(Intent.ACTION_VIEW, url.toUri())
                                                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                                context.startActivity(intent)
                                                Log.d("UpdateDialog", "Opening URL: $url")
                                            }
                                            showUpdateDialog = false
                                        },
                                    )
                                }
                            }

                            if (preferences.lastUpdateTime == null) {
                                MessageComponent(
                                    type = MessageType.INFO,
                                    message = stringResource(R.string.no_subscription),
                                    actionLabel = stringResource(R.string.client_id),
                                    action = onNavigateToProfile,
                                    actionIcon =
                                        ImageVector.vectorResource(id = R.drawable.baseline_add_24),
                                )
                            } else {
                                val time = preferences.expireTime
                                val traffic = preferences.traffic
                                val used = preferences.usedTraffic

                                val now = System.currentTimeMillis()
                                val expire = parseDateToMillis(time)
                                if (now > expire) {
                                    MessageComponent(
                                        type = MessageType.WARNING,
                                        message =
                                            stringResource(R.string.subscription_expired_warning),
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                }

                                if (traffic > 0 && used >= traffic) {
                                    MessageComponent(
                                        type = MessageType.WARNING,
                                        message = stringResource(R.string.traffic_limit_warning),
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                }

                                if (leafState is LeafViewModel.LeafState.Error) {
                                    val error = leafState as LeafViewModel.LeafState.Error
                                    MessageComponent(
                                        type = MessageType.ERROR,
                                        message = error.error,
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))
                                }

                                OutboundsScreen(leafViewModel, Modifier.weight(1.0f))

                                Spacer(modifier = Modifier.height(8.dp))

                                ConnectionButton(leafViewModel) {
                                    if (leafState == LeafViewModel.LeafState.Started) {
                                        leafViewModel.stopLeaf()
                                    } else {
                                        onStartLeaf()
                                    }
                                }
                            }
                        }
                    }
                    else -> {
                        MessageComponent(
                            type = MessageType.ERROR,
                            message = stringResource(R.string.unknown),
                        )
                    }
                }
            }
            LeafViewModel.ServiceState.Disconnected -> {
                MessageComponent(
                    type = MessageType.ERROR,
                    message = stringResource(R.string.vpn_service_disconnected),
                    actionLabel = stringResource(R.string.connect),
                    action = { ServiceManagement.getInstance().bindService(context) },
                )
            }
            LeafViewModel.ServiceState.Loading -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
            is LeafViewModel.ServiceState.Error -> {
                MessageComponent(
                    type = MessageType.ERROR,
                    message =
                        stringResource(
                            R.string.vpn_service_error,
                            (serviceState as LeafViewModel.ServiceState.Error).error,
                        ),
                )
            }
            else -> {
                MessageComponent(
                    type = MessageType.ERROR,
                    message = stringResource(R.string.unknown),
                )
            }
        }
    }
}
