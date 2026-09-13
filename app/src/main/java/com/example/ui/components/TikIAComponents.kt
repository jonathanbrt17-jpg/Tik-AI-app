package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.TikIABlackBackground
import com.example.ui.theme.TikIABlackSurface
import com.example.ui.theme.TikIABlackSurfaceVariant
import com.example.ui.theme.TikIABorder
import com.example.ui.theme.TikIARedDark
import com.example.ui.theme.TikIARedPrimary
import com.example.ui.theme.TikIARedVariant
import com.example.ui.theme.TikIATextMuted
import com.example.ui.theme.TikIATextPrimary
import com.example.ui.theme.TikIATextSecondary

@Composable
fun TikIALogoBadge(
    modifier: Modifier = Modifier,
    sizeDp: Int = 96
) {
    val infiniteTransition = rememberInfiniteTransition(label = "badgePulse")
    val glowScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowScale"
    )

    Box(
        modifier = modifier
            .size(sizeDp.dp)
            .scale(glowScale),
        contentAlignment = Alignment.Center
    ) {
        // Outer red glow ring
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(26.dp))
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            TikIARedVariant.copy(alpha = 0.35f),
                            TikIARedDark.copy(alpha = 0.15f),
                            Color.Transparent
                        )
                    )
                )
        )

        // Inner solid shield
        Box(
            modifier = Modifier
                .size((sizeDp * 0.85).dp)
                .clip(RoundedCornerShape(22.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            TikIABlackSurfaceVariant,
                            TikIABlackSurface
                        )
                    )
                )
                .border(
                    width = 2.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(TikIARedPrimary, TikIARedDark)
                    ),
                    shape = RoundedCornerShape(22.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "T",
                    fontSize = (sizeDp * 0.42).sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    letterSpacing = (-1).sp
                )
                Box(
                    modifier = Modifier
                        .width((sizeDp * 0.28).dp)
                        .height(3.dp)
                        .clip(CircleShape)
                        .background(TikIARedPrimary)
                )
            }
        }
    }
}

@Composable
fun TikIALoadingScreen(
    modifier: Modifier = Modifier,
    statusText: String = stringResource(R.string.loading_assistant)
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(TikIABlackBackground)
            .testTag("tikia_loading_screen"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp)
        ) {
            TikIALogoBadge(sizeDp = 100)

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = stringResource(R.string.app_name),
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = TikIATextPrimary,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = statusText,
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                color = TikIATextSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            CircularProgressIndicator(
                modifier = Modifier
                    .size(36.dp)
                    .testTag("tikia_loading_indicator"),
                color = TikIARedPrimary,
                trackColor = TikIABlackSurfaceVariant,
                strokeWidth = 3.5.dp
            )
        }
    }
}

@Composable
fun TikIAErrorScreen(
    modifier: Modifier = Modifier,
    onRetry: () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(TikIABlackBackground)
            .padding(24.dp)
            .testTag("tikia_error_screen"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(TikIABlackSurface)
                .border(1.dp, TikIABorder, RoundedCornerShape(24.dp))
                .padding(horizontal = 24.dp, vertical = 36.dp)
        ) {
            // Error icon in red circle
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(TikIARedDark.copy(alpha = 0.35f))
                    .border(1.5.dp, TikIARedPrimary.copy(alpha = 0.6f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.WifiOff,
                    contentDescription = stringResource(R.string.error_offline_title),
                    tint = TikIARedPrimary,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = stringResource(R.string.error_offline_title),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TikIATextPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = stringResource(R.string.error_offline_desc),
                fontSize = 14.sp,
                color = TikIATextSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(28.dp))

            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(
                    containerColor = TikIARedPrimary,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("retry_button")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.action_retry),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "TikIA • Assistant IA",
                fontSize = 12.sp,
                color = TikIATextMuted
            )
        }
    }
}

@Composable
fun TikIATopProgressBar(
    progress: Float,
    visible: Boolean,
    modifier: Modifier = Modifier
) {
    if (visible && progress < 1.0f) {
        LinearProgressIndicator(
            progress = { progress },
            modifier = modifier
                .fillMaxWidth()
                .height(3.dp)
                .testTag("tikia_top_progress_bar"),
            color = TikIARedPrimary,
            trackColor = TikIABlackBackground
        )
    }
}
