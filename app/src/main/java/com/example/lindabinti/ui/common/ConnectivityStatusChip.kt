package com.example.lindabinti.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.lindabinti.LindaBintiApp
import com.example.lindabinti.R

/**
 * Neutral Online / Offline chip for protected screens. Never shows sensitive content.
 */
@Composable
fun ConnectivityStatusChip(modifier: Modifier = Modifier) {
    val app = LindaBintiApp.instance
    val online by app.networkMonitor.isOnline.collectAsStateWithLifecycle()
    val syncing by app.syncCoordinator.syncing.collectAsStateWithLifecycle()

    val label = when {
        !online -> stringResource(R.string.connectivity_offline)
        syncing -> stringResource(R.string.connectivity_syncing)
        else -> stringResource(R.string.connectivity_online)
    }
    val accent = when {
        !online -> Color(0xFF94A3B8)
        syncing -> MaterialTheme.colorScheme.tertiary
        else -> Color(0xFF22C55E)
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = Color.Black.copy(alpha = 0.28f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier
                    .size(8.dp)
                    .clip(RoundedCornerShape(50)),
                color = accent
            ) {}
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

@Composable
fun ConnectivityStatusRow(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        ConnectivityStatusChip()
        Text(
            text = stringResource(R.string.connectivity_hint),
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.75f),
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}
