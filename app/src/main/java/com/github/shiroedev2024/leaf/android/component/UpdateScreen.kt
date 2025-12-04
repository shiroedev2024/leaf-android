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

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.github.shiroedev2024.leaf.android.R
import com.github.shiroedev2024.leaf.android.update.UpdateResponse
import com.zeugmasolutions.localehelper.LocaleHelper
import java.text.DateFormat
import java.time.Instant
import java.util.Date

@Composable
fun UpdateScreen(
    info: UpdateResponse,
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit,
    onOpenUrl: (String) -> Unit,
) {
    val context = LocalContext.current
    val appLang = LocaleHelper.getLocale(context).language
    val lang = appLang.substringBefore('-')

    val changelog =
        info.changeLog.firstOrNull { it.languageCode == lang }?.text
            ?: info.changeLog.firstOrNull { it.languageCode.startsWith("en") }?.text
            ?: info.changeLog.firstOrNull()?.text
            ?: stringResource(R.string.update_no_changelog)

    var selectedSource by remember { mutableStateOf(info.downloadSources.first().url) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(id = R.drawable.baseline_download_24),
                        contentDescription = stringResource(R.string.update_download),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = info.downloadName ?: stringResource(R.string.update_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                }

                val publishedLabel =
                    info.publishedDate?.let { raw ->
                        var label: String? = null
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                            try {
                                val ms = Instant.parse(raw).toEpochMilli()
                                if (ms > 0) label = DateFormat.getDateInstance().format(Date(ms))
                            } catch (e: Exception) {
                                // fall back to raw
                                label = raw
                            }
                        }
                        label
                    }

                val versionLabel = info.latestVersionName ?: ""

                Text(
                    text =
                        listOfNotNull(
                                versionLabel.takeIf { it.isNotEmpty() }?.let { "v$it" },
                                publishedLabel,
                            )
                            .joinToString(" • "),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        text = {
            Column(modifier = modifier.verticalScroll(rememberScrollState())) {
                Text(
                    text = (info.downloadDescription ?: ""),
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(id = R.drawable.baseline_update_24),
                        contentDescription = stringResource(R.string.update_changelog_label),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.update_changelog_label),
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
                Text(
                    text = changelog.replace("\\n", "\n"),
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = stringResource(R.string.update_type_label),
                    modifier = Modifier.padding(bottom = 4.dp),
                )
                info.downloadSources.forEach { source ->
                    Row(
                        modifier =
                            Modifier.fillMaxWidth()
                                .clickable { selectedSource = source.url }
                                .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        val isSelected = selectedSource == source.url
                        val iconRes =
                            when (source.type) {
                                "google" -> R.drawable.baseline_store_24
                                "direct" -> R.drawable.baseline_link_24
                                else -> R.drawable.baseline_cloud_24
                            }
                        Icon(
                            painter = painterResource(id = iconRes),
                            contentDescription = null,
                            tint =
                                if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            modifier = Modifier.weight(1f),
                            text =
                                when (source.type) {
                                    "google" -> stringResource(R.string.update_type_google)
                                    "direct" -> stringResource(R.string.update_type_direct)
                                    else -> stringResource(R.string.update_type_other)
                                },
                            color =
                                if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onBackground,
                        )
                        RadioButton(
                            selected = isSelected,
                            onClick = { selectedSource = source.url },
                            colors =
                                RadioButtonDefaults.colors(
                                    selectedColor = MaterialTheme.colorScheme.primary
                                ),
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val urlToOpen =
                        selectedSource.ifEmpty { info.downloadSources.firstOrNull()?.url.orEmpty() }
                    if (urlToOpen.isNotEmpty()) {
                        onOpenUrl(urlToOpen)
                    }
                    onDismiss()
                }
            ) {
                Text(text = stringResource(R.string.update_download))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(text = stringResource(R.string.update_close)) }
        },
    )
}
