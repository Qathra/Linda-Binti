package com.example.lindabinti.ui.ussd

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import com.example.lindabinti.ui.common.LindaProtectedScaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.lindabinti.R
import com.example.lindabinti.data.offline.NetworkMonitor
import com.example.lindabinti.ui.common.LindaSectionCard
import com.example.lindabinti.ui.common.ProtectedTopBar

@Composable
fun UssdHelpScreen(
    onBack: () -> Unit,
    onQuickHide: () -> Unit
) {
    val mod = androidx.compose.ui.Modifier
    val context = LocalContext.current
    val internetLikely = remember { NetworkMonitor(context).isInternetLikely() }

    LindaProtectedScaffold(
        topBar = {
            ProtectedTopBar(
                title = stringResource(R.string.ussd_help_title),
                onBack = onBack,
                onQuickHide = onQuickHide
            )
        },
    ) { padding ->
        Column(
            modifier = mod
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                stringResource(R.string.ussd_help_intro),
                style = MaterialTheme.typography.bodyMedium
            )
            LindaSectionCard(modifier = mod.fillMaxWidth()) {
                Text(stringResource(R.string.ussd_help_not_live_title), fontWeight = FontWeight.SemiBold)
                Text(stringResource(R.string.ussd_help_not_live_body), style = MaterialTheme.typography.bodySmall)
            }
            LindaSectionCard(modifier = mod.fillMaxWidth()) {
                Text(stringResource(R.string.ussd_help_flow_title), fontWeight = FontWeight.SemiBold)
                Text(stringResource(R.string.ussd_help_flow_body), style = MaterialTheme.typography.bodySmall)
            }
            LindaSectionCard(modifier = mod.fillMaxWidth()) {
                Text(stringResource(R.string.ussd_help_offline_title), fontWeight = FontWeight.SemiBold)
                Text(stringResource(R.string.ussd_help_offline_body), style = MaterialTheme.typography.bodySmall)
            }
            LindaSectionCard(modifier = mod.fillMaxWidth()) {
                Text(stringResource(R.string.ussd_help_sms_title), fontWeight = FontWeight.SemiBold)
                Text(stringResource(R.string.ussd_help_sms_body), style = MaterialTheme.typography.bodySmall)
            }
            LindaSectionCard(modifier = mod.fillMaxWidth()) {
                Text(stringResource(R.string.ussd_help_needs_title), fontWeight = FontWeight.SemiBold)
                Text(stringResource(R.string.ussd_help_needs_body), style = MaterialTheme.typography.bodySmall)
            }
            Text(
                stringResource(
                    if (internetLikely) R.string.ussd_help_cloud_available else R.string.ussd_help_cloud_unavailable
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
