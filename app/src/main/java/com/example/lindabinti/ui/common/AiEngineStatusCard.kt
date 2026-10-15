package com.example.lindabinti.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.lindabinti.LindaBintiApp
import com.example.lindabinti.R
import com.example.lindabinti.ai.AiOrchestrationMode

@Composable
fun AiEngineStatusCard(modifier: Modifier = Modifier) {
    val app = LindaBintiApp.instance
    var mode by remember { mutableStateOf(AiOrchestrationMode.OFFLINE) }

    LaunchedEffect(Unit) {
        mode = app.aiOrchestrator.routingMode()
    }

    val (titleRes, hintRes, accent) = when (mode) {
        AiOrchestrationMode.ONLINE -> Triple(
            R.string.home_ai_mode_online,
            R.string.home_ai_hint_online,
            MaterialTheme.colorScheme.primary
        )
        AiOrchestrationMode.DEGRADED -> Triple(
            R.string.home_ai_mode_degraded,
            R.string.home_ai_hint_degraded,
            MaterialTheme.colorScheme.tertiary
        )
        AiOrchestrationMode.OFFLINE -> Triple(
            R.string.home_ai_mode_offline,
            R.string.home_ai_hint_offline,
            MaterialTheme.colorScheme.secondary
        )
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier
                    .size(10.dp)
                    .clip(RoundedCornerShape(50)),
                color = accent
            ) {}
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = stringResource(R.string.home_ai_status_title),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = stringResource(titleRes),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(hintRes),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
