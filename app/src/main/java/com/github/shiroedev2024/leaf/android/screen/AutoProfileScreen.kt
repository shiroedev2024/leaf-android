package com.github.shiroedev2024.leaf.android.screen

import android.content.Intent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import com.github.shiroedev2024.leaf.android.R
import com.github.shiroedev2024.leaf.android.activity.MainActivity
import com.github.shiroedev2024.leaf.android.component.MessageComponent
import com.github.shiroedev2024.leaf.android.component.MessageType
import com.github.shiroedev2024.leaf.android.library.ServiceManagement
import com.github.shiroedev2024.leaf.android.viewmodel.AutoProfileViewModel
import com.github.shiroedev2024.leaf.android.viewmodel.LeafViewModel

@Composable
fun AutoProfileContent(
    leafViewModel: LeafViewModel,
    autoProfileViewModel: AutoProfileViewModel,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    val serviceState by leafViewModel.serviceState.observeAsState()
    val subscriptionState by leafViewModel.subscriptionState.observeAsState()
    val profileId by autoProfileViewModel.profileId.observeAsState()

    LaunchedEffect(serviceState) {
        if (serviceState == LeafViewModel.ServiceState.Connected) {
            profileId?.let { leafViewModel.updateSubscription(it) }
        }
    }

    Box(modifier = modifier.fillMaxSize().padding(16.dp)) {
        when (serviceState) {
            LeafViewModel.ServiceState.Connected -> {
                if (profileId != null) {
                    when (subscriptionState) {
                        LeafViewModel.SubscriptionState.Fetching -> {
                            CircularProgressIndicator()
                        }
                        LeafViewModel.SubscriptionState.Success -> {
                            MessageComponent(
                                type = MessageType.SUCCESS,
                                message = stringResource(R.string.profile_installed_successfully),
                                actionLabel = stringResource(R.string.open_app),
                                action = {
                                    val intent =
                                        Intent(context, MainActivity::class.java).apply {
                                            flags =
                                                Intent.FLAG_ACTIVITY_CLEAR_TASK or
                                                    Intent.FLAG_ACTIVITY_NEW_TASK
                                        }
                                    context.startActivity(intent)
                                },
                                actionIcon =
                                    ImageVector.vectorResource(
                                        id = R.drawable.baseline_exit_to_app_24
                                    ),
                            )
                        }
                        is LeafViewModel.SubscriptionState.Error -> {
                            MessageComponent(
                                type = MessageType.ERROR,
                                message =
                                    stringResource(
                                        R.string.update_subscription_failed,
                                        (subscriptionState as LeafViewModel.SubscriptionState.Error)
                                            .error,
                                    ),
                                actionLabel = stringResource(R.string.retry),
                                action = { leafViewModel.updateSubscription(profileId!!) },
                                actionIcon =
                                    ImageVector.vectorResource(id = R.drawable.baseline_refresh_24),
                            )
                        }
                        LeafViewModel.SubscriptionState.Initial -> {
                            MessageComponent(
                                type = MessageType.INFO,
                                message = stringResource(R.string.initial),
                            )
                        }
                        else -> {
                            MessageComponent(
                                type = MessageType.ERROR,
                                message = stringResource(R.string.unknown),
                            )
                        }
                    }
                } else {
                    MessageComponent(
                        type = MessageType.ERROR,
                        message = stringResource(R.string.no_profile_id),
                    )
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
