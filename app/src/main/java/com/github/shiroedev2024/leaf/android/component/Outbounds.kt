package com.github.shiroedev2024.leaf.android.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.github.shiroedev2024.leaf.android.R
import com.github.shiroedev2024.leaf.android.library.model.OutboundInfo
import com.github.shiroedev2024.leaf.android.parseAsCountryInfo
import com.github.shiroedev2024.leaf.android.viewmodel.LeafViewModel

@Composable
fun OutboundsScreen(leafViewModel: LeafViewModel, modifier: Modifier = Modifier) {
    val outboundState by leafViewModel.outboundState.observeAsState()
    val outbounds by leafViewModel.outbounds.collectAsState()
    val lazyListState = rememberLazyListState()

    Column(modifier = modifier.fillMaxWidth().padding(16.dp)) {
        when (outboundState) {
            is LeafViewModel.OutboundState.Success -> {

                Text(
                    text = stringResource(R.string.outbound_title),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                Divider(modifier = Modifier.padding(bottom = 8.dp))
                LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f), state = lazyListState) {
                    items(outbounds, key = { it.name }) { outboundInfo ->
                        OutboundCard(
                            outboundInfo = outboundInfo,
                            onSelect = { leafViewModel.changeSelectedOutbound(outboundInfo.name) },
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
            is LeafViewModel.OutboundState.Error -> {
                MessageComponent(
                    type = MessageType.ERROR,
                    message = stringResource(R.string.outbound_error),
                    actionLabel = stringResource(R.string.retry),
                    actionIcon = ImageVector.vectorResource(R.drawable.baseline_refresh_24),
                    action = { leafViewModel.getOutboundList() },
                )
            }
            LeafViewModel.OutboundState.Initial -> {
                MessageComponent(
                    type = MessageType.INFO,
                    message = stringResource(R.string.outbound_initial),
                )
            }
            LeafViewModel.OutboundState.Loading -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
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
fun OutboundCard(outboundInfo: OutboundInfo, onSelect: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth().clickable(onClick = onSelect).padding(vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    if (outboundInfo.isSelected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.surface
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Text(
            text = outboundInfo.name.parseAsCountryInfo(),
            modifier = Modifier.padding(16.dp),
            textAlign = TextAlign.Center,
            style =
                if (outboundInfo.isSelected) {
                    MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
        )
    }
}
