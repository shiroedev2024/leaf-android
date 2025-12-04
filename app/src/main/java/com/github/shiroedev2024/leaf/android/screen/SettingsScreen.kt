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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.github.shiroedev2024.leaf.android.BuildConfig
import com.github.shiroedev2024.leaf.android.MainApplication
import com.github.shiroedev2024.leaf.android.R
import com.github.shiroedev2024.leaf.android.activity.MemoryLoggerActivity
import com.github.shiroedev2024.leaf.android.component.*
import com.github.shiroedev2024.leaf.android.library.ServiceManagement
import com.github.shiroedev2024.leaf.android.library.model.LeafPreferences
import com.github.shiroedev2024.leaf.android.library.model.LogLevel
import com.github.shiroedev2024.leaf.android.model.AppPreferences
import com.github.shiroedev2024.leaf.android.model.toAppPreferences
import com.github.shiroedev2024.leaf.android.viewmodel.LeafViewModel
import com.github.shiroedev2024.leaf.android.viewmodel.UpdateViewModel
import com.zeugmasolutions.localehelper.LocaleHelper
import java.util.Locale

@Composable
fun SettingsContent(
    leafViewModel: LeafViewModel,
    updateViewModel: UpdateViewModel,
    modifier: Modifier = Modifier,
    onUpdateLocale: (locale: Locale) -> Unit,
) {
    val context = LocalContext.current
    val serviceState by leafViewModel.serviceState.observeAsState()
    val preferencesState by leafViewModel.preferencesState.observeAsState()

    val updateState by updateViewModel.updateState.observeAsState()
    LaunchedEffect(updateState) {
        when (updateState) {
            is UpdateViewModel.UpdateState.ManualChecking -> {
                // Checking state handled by UI, no Toast needed
            }
            is UpdateViewModel.UpdateState.NotAvailable -> {
                Toast.makeText(context, R.string.update_check_success, Toast.LENGTH_SHORT).show()
            }
            is UpdateViewModel.UpdateState.Error -> {
                Toast.makeText(context, R.string.update_check_manual_error, Toast.LENGTH_SHORT)
                    .show()
            }
            // Update available will be handled by existing dashboard dialog
            else -> {
                /* Other states handled elsewhere */
            }
        }
    }

    Box(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Column(modifier = Modifier.fillMaxSize()) {
            when (serviceState) {
                LeafViewModel.ServiceState.Loading -> {
                    Box(Modifier.fillMaxSize()) {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    }
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
                                (preferencesState as LeafViewModel.PreferencesState.Success)
                                    .preferences

                            LeafSettingsContent(
                                updateViewModel = updateViewModel,
                                currentPreferences = preferences,
                                onPreferencesChanged = { prefs ->
                                    leafViewModel.setPreferences(prefs)
                                    Log.d("SettingsContent", "Settings saved")
                                },
                                onUpdateLocale = onUpdateLocale,
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
                        message = stringResource(R.string.vpn_service_disconnected),
                        actionLabel = stringResource(R.string.connect),
                        action = { ServiceManagement.getInstance().bindService(context) },
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
}

@Composable
fun LeafSettingsContent(
    updateViewModel: UpdateViewModel,
    currentPreferences: LeafPreferences?,
    onPreferencesChanged: (AppPreferences) -> Unit,
    onUpdateLocale: (locale: Locale) -> Unit,
) {
    val context = LocalContext.current

    var prefs by remember {
        mutableStateOf(currentPreferences?.toAppPreferences() ?: AppPreferences())
    }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        PreferenceCategory(title = stringResource(R.string.general_settings)) {
            SelectBoxPreference(
                title = stringResource(R.string.language),
                description = stringResource(R.string.language_description),
                icon = ImageVector.vectorResource(id = R.drawable.baseline_language_24),
                options = MainApplication.languages,
                selectedOption = LocaleHelper.getLocale(context).language,
                onOptionSelected = { selected ->
                    onUpdateLocale(Locale(selected))
                    // onPreferencesChanged(prefs)
                },
            )

            Spacer(modifier = Modifier.height(8.dp))

            SwitchPreference(
                title = stringResource(R.string.enable_ipv6),
                description = stringResource(R.string.enable_ipv6_description),
                icon = ImageVector.vectorResource(id = R.drawable.baseline_swap_horiz_24),
                value = prefs.enableIpv6,
                onValueChanged = {
                    prefs = prefs.copy(enableIpv6 = it)
                    onPreferencesChanged(prefs)
                },
            )

            Spacer(modifier = Modifier.height(8.dp))

            SwitchPreference(
                title = stringResource(R.string.prefer_ipv6),
                description = stringResource(R.string.prefer_ipv6_description),
                icon = ImageVector.vectorResource(id = R.drawable.baseline_swap_horiz_24),
                value = prefs.preferIpv6,
                onValueChanged = {
                    prefs = prefs.copy(preferIpv6 = it)
                    onPreferencesChanged(prefs)
                },
            )

            Spacer(modifier = Modifier.height(8.dp))

            SwitchPreference(
                title = stringResource(R.string.bypass_lan),
                description = stringResource(R.string.bypass_lan_description),
                icon = ImageVector.vectorResource(id = R.drawable.baseline_lan_24),
                value = prefs.bypassLan,
                onValueChanged = {
                    prefs = prefs.copy(bypassLan = it)
                    onPreferencesChanged(prefs)
                },
            )

            Spacer(modifier = Modifier.height(8.dp))

            SwitchPreference(
                title = stringResource(R.string.bypass_lan_in_core),
                description = stringResource(R.string.bypass_lan_in_core_description),
                icon = ImageVector.vectorResource(id = R.drawable.baseline_lan_24),
                value = prefs.bypassLanInCore,
                onValueChanged = {
                    prefs = prefs.copy(bypassLanInCore = it)
                    onPreferencesChanged(prefs)
                },
                enabled = prefs.bypassLan,
            )

            Spacer(modifier = Modifier.height(8.dp))

            SwitchPreference(
                title = stringResource(R.string.fake_ip),
                description = stringResource(R.string.fake_ip_description),
                icon = ImageVector.vectorResource(id = R.drawable.baseline_swap_horiz_24),
                value = prefs.fakeIp,
                onValueChanged = {
                    prefs = prefs.copy(fakeIp = it)
                    onPreferencesChanged(prefs)
                },
            )

            Spacer(modifier = Modifier.height(8.dp))

            SwitchPreference(
                title = stringResource(R.string.force_resolve_domain),
                description = stringResource(R.string.force_resolve_domain_description),
                icon = ImageVector.vectorResource(id = R.drawable.baseline_refresh_24),
                value = prefs.forceResolveDomain,
                onValueChanged = {
                    prefs = prefs.copy(forceResolveDomain = it)
                    onPreferencesChanged(prefs)
                },
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        PreferenceCategory(title = stringResource(R.string.routing_rules)) {
            ListEditorPreference(
                title = stringResource(R.string.bypass_geoip_list),
                description = stringResource(R.string.bypass_geoip_list_description),
                icon = ImageVector.vectorResource(id = R.drawable.baseline_location_on_24),
                items = prefs.bypassGeoipList,
                onItemsChanged = {
                    prefs = prefs.copy(bypassGeoipList = it)
                    onPreferencesChanged(prefs)
                },
            )

            Spacer(modifier = Modifier.height(8.dp))

            ListEditorPreference(
                title = stringResource(R.string.bypass_geosite_list),
                description = stringResource(R.string.bypass_geosite_list_description),
                icon = ImageVector.vectorResource(id = R.drawable.baseline_public_24),
                items = prefs.bypassGeositeList,
                onItemsChanged = {
                    prefs = prefs.copy(bypassGeositeList = it)
                    onPreferencesChanged(prefs)
                },
            )

            Spacer(modifier = Modifier.height(8.dp))

            ListEditorPreference(
                title = stringResource(R.string.reject_geoip_list),
                description = stringResource(R.string.reject_geoip_list_description),
                icon = ImageVector.vectorResource(id = R.drawable.baseline_block_24),
                items = prefs.rejectGeoipList,
                onItemsChanged = {
                    prefs = prefs.copy(rejectGeoipList = it)
                    onPreferencesChanged(prefs)
                },
            )

            Spacer(modifier = Modifier.height(8.dp))

            ListEditorPreference(
                title = stringResource(R.string.reject_geosite_list),
                description = stringResource(R.string.reject_geosite_list_description),
                icon = ImageVector.vectorResource(id = R.drawable.baseline_block_24),
                items = prefs.rejectGeositeList,
                onItemsChanged = {
                    prefs = prefs.copy(rejectGeositeList = it)
                    onPreferencesChanged(prefs)
                },
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        PreferenceCategory(title = stringResource(R.string.debug_settings)) {
            SwitchPreference(
                title = stringResource(R.string.memory_logger),
                description = stringResource(R.string.memory_logger_description),
                icon = ImageVector.vectorResource(id = R.drawable.baseline_memory_24),
                value = prefs.memoryLogger,
                onValueChanged = {
                    prefs = prefs.copy(memoryLogger = it)
                    onPreferencesChanged(prefs)
                },
            )

            Spacer(modifier = Modifier.height(8.dp))

            val logLevelOptions =
                mapOf(
                    LogLevel.WARN to stringResource(R.string.log_level_warn),
                    LogLevel.INFO to stringResource(R.string.log_level_info),
                    LogLevel.ERROR to stringResource(R.string.log_level_error),
                    LogLevel.DEBUG to stringResource(R.string.log_level_debug),
                    LogLevel.TRACE to stringResource(R.string.log_level_trace),
                )

            val logLevels = logLevelOptions.mapKeys { it.key.name }

            SelectBoxPreference(
                title = stringResource(R.string.log_level),
                description = stringResource(R.string.log_level_description),
                icon = ImageVector.vectorResource(id = R.drawable.baseline_description_24),
                options = logLevels,
                selectedOption = prefs.logLevel.name,
                onOptionSelected = { selectedName ->
                    val found =
                        logLevelOptions.keys.find { it.name == selectedName } ?: LogLevel.WARN
                    prefs = prefs.copy(logLevel = found)
                    onPreferencesChanged(prefs)
                },
            )

            if (prefs.memoryLogger) {
                Spacer(modifier = Modifier.height(8.dp))

                TriggerPreference(
                    title = stringResource(R.string.logs),
                    description = stringResource(R.string.logs_description),
                    icon = ImageVector.vectorResource(id = R.drawable.baseline_bug_report_24),
                    onClick = {
                        context.startActivity(Intent(context, MemoryLoggerActivity::class.java))
                    },
                )
            }

            if (BuildConfig.DEBUG) {
                Spacer(modifier = Modifier.height(8.dp))

                TextFieldPreference(
                    title = stringResource(R.string.api_port),
                    description = stringResource(R.string.api_port_description),
                    icon = ImageVector.vectorResource(id = R.drawable.baseline_router_24),
                    value = prefs.apiPort.toString(),
                    onValueChanged = {
                        prefs = prefs.copy(apiPort = it.toIntOrNull() ?: 0)
                        onPreferencesChanged(prefs)
                    },
                )

                Spacer(modifier = Modifier.height(8.dp))

                TextFieldPreference(
                    title = stringResource(R.string.custom_user_agent),
                    description = stringResource(R.string.custom_user_agent_description),
                    icon = ImageVector.vectorResource(id = R.drawable.baseline_person_24),
                    value = prefs.customUserAgent,
                    onValueChanged = {
                        prefs = prefs.copy(customUserAgent = it)
                        onPreferencesChanged(prefs)
                    },
                )

                Spacer(modifier = Modifier.height(8.dp))

                SwitchPreference(
                    title = stringResource(R.string.auto_reload),
                    description = stringResource(R.string.auto_reload_description),
                    icon = ImageVector.vectorResource(id = R.drawable.baseline_refresh_24),
                    value = prefs.autoReload,
                    onValueChanged = {
                        prefs = prefs.copy(autoReload = it)
                        onPreferencesChanged(prefs)
                    },
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        PreferenceCategory(title = stringResource(R.string.app_updates)) {
            val updateState by updateViewModel.updateState.observeAsState()
            TriggerPreference(
                title = stringResource(R.string.check_for_updates),
                description =
                    if (updateState is UpdateViewModel.UpdateState.ManualChecking) {
                        stringResource(R.string.checking_for_updates)
                    } else {
                        stringResource(R.string.check_for_updates_description)
                    },
                icon = ImageVector.vectorResource(id = R.drawable.baseline_update_24),
                onClick = { updateViewModel.checkForUpdateManual() },
                enabled = updateState !is UpdateViewModel.UpdateState.ManualChecking,
            )

            Spacer(modifier = Modifier.height(8.dp))

            PreferenceInfo(
                title = stringResource(R.string.current_version),
                description = BuildConfig.VERSION_NAME,
                icon = ImageVector.vectorResource(id = R.drawable.baseline_info_24),
            )

            Spacer(modifier = Modifier.height(8.dp))

            PreferenceInfo(
                title = stringResource(R.string.leaf_core_version),
                description = ServiceManagement.getInstance().getVersion(),
                icon = ImageVector.vectorResource(id = R.drawable.baseline_info_24),
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        var showLoadDefaultsDialog by remember { mutableStateOf(false) }

        Button(
            onClick = { showLoadDefaultsDialog = true },
            modifier = Modifier.fillMaxWidth(),
            colors =
                ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                ),
        ) {
            Icon(
                imageVector = ImageVector.vectorResource(id = R.drawable.baseline_refresh_24),
                contentDescription = null,
                modifier = Modifier.padding(end = 8.dp),
            )
            Text(text = stringResource(R.string.load_defaults))
        }

        if (showLoadDefaultsDialog) {
            AlertDialog(
                onDismissRequest = { showLoadDefaultsDialog = false },
                title = { Text(text = stringResource(R.string.load_defaults_confirm_title)) },
                text = { Text(text = stringResource(R.string.load_defaults_confirm_message)) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            prefs = AppPreferences()
                            onPreferencesChanged(prefs)
                            showLoadDefaultsDialog = false
                        }
                    ) {
                        Text(text = stringResource(android.R.string.ok))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showLoadDefaultsDialog = false }) {
                        Text(text = stringResource(android.R.string.cancel))
                    }
                },
            )
        }
    }
}
