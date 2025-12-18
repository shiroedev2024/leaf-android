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
package com.github.shiroedev2024.leaf.android.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import com.github.shiroedev2024.leaf.android.R
import com.github.shiroedev2024.leaf.android.ui.theme.ConnectionButtonConnected
import com.github.shiroedev2024.leaf.android.ui.theme.ConnectionButtonDisconnected
import com.github.shiroedev2024.leaf.android.ui.theme.ConnectionButtonLoading
import com.github.shiroedev2024.leaf.android.viewmodel.LeafViewModel

@Composable
fun ConnectionButton(
    leafViewModel: LeafViewModel,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val leafState by leafViewModel.leafState.observeAsState()

    val buttonData =
        when (leafState) {
            LeafViewModel.LeafState.Started ->
                ButtonData(
                    label = stringResource(R.string.disconnect),
                    icon = ImageVector.vectorResource(id = R.drawable.baseline_stop_24),
                    containerColor = ConnectionButtonConnected,
                )
            LeafViewModel.LeafState.Loading ->
                ButtonData(
                    label = stringResource(R.string.loading),
                    icon = null,
                    containerColor = ConnectionButtonLoading,
                )
            LeafViewModel.LeafState.Reloaded ->
                ButtonData(
                    label = stringResource(R.string.reloaded),
                    icon = ImageVector.vectorResource(id = R.drawable.baseline_refresh_24),
                    containerColor = ConnectionButtonLoading,
                )
            else ->
                ButtonData(
                    label = stringResource(R.string.connect),
                    icon = ImageVector.vectorResource(id = R.drawable.baseline_play_arrow_24),
                    containerColor = ConnectionButtonDisconnected,
                )
        }

    Button(
        modifier = modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(containerColor = buttonData.containerColor),
        enabled =
            leafState != LeafViewModel.LeafState.Loading &&
                leafState != LeafViewModel.LeafState.Reloaded,
        onClick = onClick,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (leafState == LeafViewModel.LeafState.Loading) {
                Box(
                    modifier =
                        Modifier.size(24.dp).padding(end = 8.dp).align(Alignment.CenterVertically),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp,
                    )
                }
            } else {
                buttonData.icon?.let {
                    Icon(
                        imageVector = it,
                        contentDescription = buttonData.label,
                        modifier =
                            Modifier.size(24.dp)
                                .padding(end = 8.dp)
                                .align(Alignment.CenterVertically),
                    )
                }
            }
            Text(text = buttonData.label, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

data class ButtonData(val label: String, val icon: ImageVector?, val containerColor: Color)
