package com.github.shiroedev2024.leaf.android.screen

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedSuggestionChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.github.shiroedev2024.leaf.android.R
import com.github.shiroedev2024.leaf.android.component.MessageComponent
import com.github.shiroedev2024.leaf.android.component.MessageType
import com.github.shiroedev2024.leaf.android.getExpirationTextColor
import com.github.shiroedev2024.leaf.android.getRemainingTraffic
import com.github.shiroedev2024.leaf.android.getRemainingTrafficTextColor
import com.github.shiroedev2024.leaf.android.library.ServiceManagement
import com.github.shiroedev2024.leaf.android.parseAsLastUpdatedTime
import com.github.shiroedev2024.leaf.android.viewmodel.LeafViewModel

@Composable
fun ProfileContent(leafViewModel: LeafViewModel, modifier: Modifier = Modifier) {
    val serviceState by leafViewModel.serviceState.observeAsState()
    val subscriptionState by leafViewModel.subscriptionState.observeAsState()
    val preferencesState by leafViewModel.preferencesState.observeAsState()

    var currentId by remember { mutableStateOf("") }

    val context = LocalContext.current

    LaunchedEffect(preferencesState) {
        if (preferencesState is LeafViewModel.PreferencesState.Success) {
            val preferences =
                (preferencesState as LeafViewModel.PreferencesState.Success).preferences
            Log.d("LeafAndroid", "Client ID: ${preferences.clientId}")
            currentId = preferences.clientId ?: ""
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

                        Column(
                            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                        ) {
                            OutlinedTextField(
                                value = currentId,
                                onValueChange = { currentId = it },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text(stringResource(R.string.client_id)) },
                                trailingIcon = {
                                    if (currentId.isNotEmpty()) {
                                        IconButton(onClick = { currentId = "" }) {
                                            Icon(
                                                imageVector =
                                                    ImageVector.vectorResource(
                                                        R.drawable.baseline_clear_24
                                                    ),
                                                contentDescription = null,
                                            )
                                        }
                                    }
                                },
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Button(
                                enabled =
                                    subscriptionState !is
                                        LeafViewModel.SubscriptionState.Fetching &&
                                        currentId.isNotEmpty(),
                                onClick = { leafViewModel.updateSubscription(currentId) },
                                modifier = Modifier.wrapContentWidth(),
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    when (subscriptionState) {
                                        is LeafViewModel.SubscriptionState.Error -> {
                                            Icon(
                                                imageVector =
                                                    ImageVector.vectorResource(
                                                        R.drawable.baseline_warning_24
                                                    ),
                                                contentDescription = null,
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(text = stringResource(R.string.retry))
                                        }
                                        LeafViewModel.SubscriptionState.Fetching -> {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                        LeafViewModel.SubscriptionState.Initial,
                                        LeafViewModel.SubscriptionState.Success -> {
                                            Icon(
                                                imageVector =
                                                    ImageVector.vectorResource(
                                                        R.drawable.baseline_refresh_24
                                                    ),
                                                contentDescription = null,
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(text = stringResource(R.string.update))
                                        }
                                        else -> {
                                            Text(text = stringResource(R.string.unknown))
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            if (subscriptionState is LeafViewModel.SubscriptionState.Error) {
                                val error =
                                    subscriptionState as LeafViewModel.SubscriptionState.Error
                                MessageComponent(type = MessageType.ERROR, message = error.error)
                                Spacer(modifier = Modifier.height(16.dp))
                            }

                            if (preferences.lastUpdateTime == null) {
                                MessageComponent(
                                    type = MessageType.INFO,
                                    message = stringResource(R.string.client_id_help),
                                )
                            } else {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors =
                                        CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.surface
                                        ),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        PreferenceSection(
                                            title = stringResource(R.string.subscription_details),
                                            content = {
                                                PreferenceRow(
                                                    title = stringResource(R.string.last_updated),
                                                    value =
                                                        preferences.lastUpdateTime
                                                            .parseAsLastUpdatedTime(),
                                                    icon =
                                                        ImageVector.vectorResource(
                                                            R.drawable.baseline_access_time_24
                                                        ),
                                                )
                                                Spacer(modifier = Modifier.height(16.dp))
                                                PreferenceRow(
                                                    title =
                                                        stringResource(R.string.remaining_traffic),
                                                    value =
                                                        getRemainingTraffic(
                                                            preferences.traffic,
                                                            preferences.usedTraffic,
                                                        ),
                                                    color =
                                                        getRemainingTrafficTextColor(
                                                            preferences.traffic,
                                                            preferences.usedTraffic,
                                                        ),
                                                    icon =
                                                        ImageVector.vectorResource(
                                                            R.drawable.baseline_network_cell_24
                                                        ),
                                                )
                                                Spacer(modifier = Modifier.height(16.dp))
                                                PreferenceRow(
                                                    title = stringResource(R.string.expire_time),
                                                    value =
                                                        preferences.expireTime
                                                            ?: stringResource(R.string.unknown),
                                                    color =
                                                        preferences.expireTime
                                                            ?.getExpirationTextColor()
                                                            ?: MaterialTheme.colorScheme.onSurface,
                                                    icon =
                                                        ImageVector.vectorResource(
                                                            R.drawable.baseline_calendar_today_24
                                                        ),
                                                )
                                            },
                                        )
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
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun PreferenceSection(title: String, content: @Composable () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        Spacer(modifier = Modifier.height(8.dp))
        content()
    }
}

@Composable
fun PreferenceRow(
    title: String,
    value: String,
    icon: ImageVector,
    color: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
            )
        }
        ElevatedSuggestionChip(
            onClick = {},
            colors = SuggestionChipDefaults.elevatedSuggestionChipColors(labelColor = color),
            label = { Text(text = value) },
        )
    }
}

@Composable
fun ClipboardImportDialog(
    profileText: String?,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    if (profileText != null) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(stringResource(R.string.import_profile)) },
            text = { Text(stringResource(R.string.import_profile_dialog)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onConfirm(profileText)
                        onDismiss()
                    }
                ) {
                    Text(stringResource(android.R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.cancel)) }
            },
        )
    }
}
