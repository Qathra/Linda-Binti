package com.example.lindabinti.ui.common

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.lindabinti.R
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.material3.LocalContentColor
import com.example.lindabinti.ui.theme.LindaCardBorder
import com.example.lindabinti.ui.theme.LindaMuted
import com.example.lindabinti.ui.theme.LindaText

@Composable
fun lindaInputTextStyle(): TextStyle =
    MaterialTheme.typography.bodyLarge.copy(color = LindaText)

@Composable
fun lindaProtectedOverlayTextFieldColors(): TextFieldColors =
    OutlinedTextFieldDefaults.colors(
        focusedTextColor = Color.White,
        unfocusedTextColor = Color.White,
        disabledTextColor = Color.White.copy(alpha = 0.38f),
        errorTextColor = MaterialTheme.colorScheme.error,
        cursorColor = Color.White,
        focusedBorderColor = Color.White.copy(alpha = 0.9f),
        unfocusedBorderColor = Color.White.copy(alpha = 0.55f),
        errorBorderColor = MaterialTheme.colorScheme.error,
        focusedLabelColor = Color.White.copy(alpha = 0.9f),
        unfocusedLabelColor = Color.White.copy(alpha = 0.75f),
        focusedContainerColor = Color(0xFF1E1B4B).copy(alpha = 0.55f),
        unfocusedContainerColor = Color(0xFF1E1B4B).copy(alpha = 0.42f),
        disabledContainerColor = Color(0xFF1E1B4B).copy(alpha = 0.3f)
    )

@Composable
fun lindaOutlinedTextFieldColors(): TextFieldColors =
    OutlinedTextFieldDefaults.colors(
        focusedTextColor = LindaText,
        unfocusedTextColor = LindaText,
        disabledTextColor = LindaText.copy(alpha = 0.38f),
        errorTextColor = MaterialTheme.colorScheme.error,
        cursorColor = MaterialTheme.colorScheme.primary,
        focusedBorderColor = MaterialTheme.colorScheme.primary,
        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
        errorBorderColor = MaterialTheme.colorScheme.error,
        focusedLabelColor = MaterialTheme.colorScheme.primary,
        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
        focusedContainerColor = MaterialTheme.colorScheme.surface,
        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
        disabledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
    )

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProtectedTopBar(
    title: String,
    onBack: (() -> Unit)? = null,
    onQuickHide: () -> Unit
) {
    TopAppBar(
        title = {
            Text(
                title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
        },
        navigationIcon = {
            if (onBack != null) {
                TextButton(onClick = onBack) {
                    Text(stringResource(R.string.action_back), color = MaterialTheme.colorScheme.onPrimary)
                }
            }
        },
        actions = {
            TextButton(onClick = onQuickHide) {
                Text(
                    stringResource(R.string.action_hide),
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primary,
            titleContentColor = MaterialTheme.colorScheme.onPrimary,
            navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
            actionIconContentColor = MaterialTheme.colorScheme.onPrimary
        )
    )
}

@Composable
fun LindaSectionCard(
    modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, LindaCardBorder),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = androidx.compose.ui.Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            CompositionLocalProvider(LocalContentColor provides LindaText) {
                content()
            }
        }
    }
}

@Composable
fun ModuleTile(
    title: String,
    subtitle: String,
    emphasized: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    iconRes: Int? = null,
    modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier
) {
    Card(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        border = if (emphasized) null else BorderStroke(1.dp, LindaCardBorder),
        colors = CardDefaults.cardColors(
            containerColor = if (emphasized) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (emphasized) 2.dp else 0.dp)
    ) {
        Column(
            modifier = androidx.compose.ui.Modifier.padding(horizontal = 16.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (iconRes != null) {
                Box(
                    modifier = androidx.compose.ui.Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (emphasized) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.18f)
                            else MaterialTheme.colorScheme.primaryContainer
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.foundation.Image(
                        painter = androidx.compose.ui.res.painterResource(iconRes),
                        contentDescription = null,
                        modifier = androidx.compose.ui.Modifier.size(26.dp),
                        colorFilter = if (emphasized) {
                            androidx.compose.ui.graphics.ColorFilter.tint(MaterialTheme.colorScheme.onPrimary)
                        } else null
                    )
                }
            } else {
                Box(
                    modifier = androidx.compose.ui.Modifier
                        .size(10.dp)
                        .clip(RoundedCornerShape(50))
                        .background(
                            if (emphasized) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f)
                            else MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
                        )
                )
            }
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                color = if (emphasized) MaterialTheme.colorScheme.onPrimary else LindaText
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = if (emphasized) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.88f)
                else LindaMuted
            )
        }
    }
}

@Composable
fun StatusBanner(
    visible: Boolean,
    title: String,
    body: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    secondaryActionLabel: String? = null,
    onSecondaryAction: (() -> Unit)? = null
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + slideInVertically(),
        exit = fadeOut()
    ) {
        Surface(
            color = MaterialTheme.colorScheme.error.copy(alpha = 0.08f),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.25f)),
            modifier = androidx.compose.ui.Modifier.fillMaxWidth()
        ) {
            Column(modifier = androidx.compose.ui.Modifier.padding(16.dp)) {
                Text(title, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.error)
                Spacer(modifier = androidx.compose.ui.Modifier.height(4.dp))
                Text(body, style = MaterialTheme.typography.bodySmall)
                if (actionLabel != null && onAction != null) {
                    TextButton(onClick = onAction) {
                        Text(actionLabel)
                    }
                }
                if (secondaryActionLabel != null && onSecondaryAction != null) {
                    TextButton(onClick = onSecondaryAction) {
                        Text(secondaryActionLabel)
                    }
                }
            }
        }
    }
}

@Composable
fun SetupProgressHeader(stepIndex: Int, totalSteps: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            stringResource(R.string.setup_step_progress, stepIndex + 1, totalSteps),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        LinearProgressIndicator(
            progress = { (stepIndex + 1).toFloat() / totalSteps.toFloat() },
            modifier = androidx.compose.ui.Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(50)),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.outlineVariant
        )
    }
}

fun Context.dialNumber(phone: String) {
    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${phone.trim()}"))
    try {
        startActivity(intent)
    } catch (_: ActivityNotFoundException) {
    }
}