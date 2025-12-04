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
package com.github.shiroedev2024.leaf.android.activity

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import com.github.shiroedev2024.leaf.android.R
import com.github.shiroedev2024.leaf.android.screen.MemoryLoggerContent
import com.github.shiroedev2024.leaf.android.ui.theme.LeafAndroidTheme
import com.github.shiroedev2024.leaf.android.viewmodel.LeafViewModel

class MemoryLoggerActivity : BaseActivity() {

    private val leafViewModel: LeafViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val memoryLoggerState by leafViewModel.memoryLoggerState.observeAsState()

            LeafAndroidTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        TopAppBar(
                            title = { Text(stringResource(R.string.memory_logger)) },
                            navigationIcon = {
                                IconButton(onClick = { finish() }) {
                                    Icon(
                                        imageVector =
                                            ImageVector.vectorResource(
                                                id = R.drawable.baseline_arrow_back_24
                                            ),
                                        contentDescription = stringResource(R.string.back),
                                    )
                                }
                            },
                            actions = {
                                IconButton(
                                    onClick = {
                                        if (
                                            memoryLoggerState
                                                is LeafViewModel.MemoryLoggerState.Started
                                        ) {
                                            leafViewModel.stopLogger()
                                        } else {
                                            leafViewModel.startLogger()
                                        }
                                    }
                                ) {
                                    val icon =
                                        if (
                                            memoryLoggerState
                                                is LeafViewModel.MemoryLoggerState.Started
                                        ) {
                                            R.drawable.baseline_stop_24
                                        } else {
                                            R.drawable.baseline_play_arrow_24
                                        }
                                    val description =
                                        if (
                                            memoryLoggerState
                                                is LeafViewModel.MemoryLoggerState.Started
                                        ) {
                                            R.string.stop
                                        } else {
                                            R.string.start
                                        }
                                    Icon(
                                        imageVector = ImageVector.vectorResource(id = icon),
                                        contentDescription = stringResource(description),
                                    )
                                }

                                IconButton(onClick = { leafViewModel.clearLogs() }) {
                                    Icon(
                                        imageVector =
                                            ImageVector.vectorResource(
                                                id = R.drawable.baseline_clear_all_24
                                            ),
                                        contentDescription = stringResource(R.string.clear_all_logs),
                                    )
                                }
                            },
                        )
                    },
                    floatingActionButton = {
                        FloatingActionButton(onClick = { copyLogsToClipboard() }) {
                            Icon(
                                imageVector =
                                    ImageVector.vectorResource(
                                        id = R.drawable.baseline_content_copy_24
                                    ),
                                contentDescription = stringResource(R.string.copy_logs),
                            )
                        }
                    },
                ) { innerPadding ->
                    MemoryLoggerContent(
                        leafViewModel = leafViewModel,
                        modifier = Modifier.padding(innerPadding),
                    )
                }
            }
        }

        leafViewModel.initListeners()
    }

    private fun copyLogsToClipboard() {
        val clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val logs = leafViewModel.memoryLogs.value.joinToString("\n")
        val clipData = ClipData.newPlainText("Logs", logs)
        clipboardManager.setPrimaryClip(clipData)
        Toast.makeText(this, getString(R.string.logs_copied), Toast.LENGTH_SHORT).show()
    }
}
