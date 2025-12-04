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

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.github.shiroedev2024.leaf.android.R
import com.github.shiroedev2024.leaf.android.component.MessageComponent
import com.github.shiroedev2024.leaf.android.component.MessageType
import com.github.shiroedev2024.leaf.android.getLogColor
import com.github.shiroedev2024.leaf.android.library.ServiceManagement
import com.github.shiroedev2024.leaf.android.ui.theme.MemoryLoggerBackground
import com.github.shiroedev2024.leaf.android.viewmodel.LeafViewModel

@Composable
fun MemoryLoggerContent(leafViewModel: LeafViewModel, modifier: Modifier = Modifier) {
    val serviceState by leafViewModel.serviceState.observeAsState()
    val leafState by leafViewModel.leafState.observeAsState()
    val logs by leafViewModel.memoryLogs.collectAsState()
    val lazyListState: LazyListState = rememberLazyListState()
    val context = LocalContext.current

    Column(modifier = modifier.fillMaxWidth().padding(16.dp)) {
        when (serviceState) {
            LeafViewModel.ServiceState.Connected -> {
                when (leafState) {
                    is LeafViewModel.LeafState.Error -> {
                        MessageComponent(
                            type = MessageType.ERROR,
                            message = (leafState as LeafViewModel.LeafState.Error).error,
                        )
                    }
                    LeafViewModel.LeafState.Loading -> {
                        CircularProgressIndicator()
                    }
                    LeafViewModel.LeafState.Started -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = stringResource(R.string.logs),
                                style = MaterialTheme.typography.titleLarge,
                            )
                        }
                        Divider(modifier = Modifier.padding(vertical = 8.dp))
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth().weight(1f),
                            state = lazyListState,
                            contentPadding = PaddingValues(vertical = 8.dp, horizontal = 0.dp),
                            reverseLayout = true,
                        ) {
                            items(logs.reversed()) { log -> LogItem(log = log) }
                        }
                        LaunchedEffect(logs) { lazyListState.animateScrollToItem(index = 0) }
                    }
                    LeafViewModel.LeafState.Stopped -> {
                        MessageComponent(
                            type = MessageType.WARNING,
                            message = stringResource(R.string.leaf_is_stopped),
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
            LeafViewModel.ServiceState.Disconnected -> {
                MessageComponent(
                    type = MessageType.ERROR,
                    message = stringResource(R.string.vpn_service_off),
                    actionLabel = stringResource(R.string.connect),
                    action = { ServiceManagement.getInstance().bindService(context) },
                )
            }
            LeafViewModel.ServiceState.Loading -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
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

@Composable
fun LogItem(log: String) {
    Surface(
        color = MemoryLoggerBackground,
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
    ) {
        Text(
            modifier = Modifier.padding(8.dp),
            text = log,
            style = MaterialTheme.typography.bodyMedium,
            color = log.getLogColor(),
        )
    }
}
