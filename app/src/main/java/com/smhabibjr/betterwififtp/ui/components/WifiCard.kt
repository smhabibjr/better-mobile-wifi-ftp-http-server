package com.smhabibjr.betterwififtp.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smhabibjr.betterwififtp.ui.WifiState
import com.smhabibjr.betterwififtp.ui.theme.AppGreen
import com.smhabibjr.betterwififtp.ui.theme.AppGreenSoft
import com.smhabibjr.betterwififtp.ui.theme.AppRed
import com.smhabibjr.betterwififtp.ui.theme.AppRedSoft
import com.smhabibjr.betterwififtp.ui.theme.AppText
import com.smhabibjr.betterwififtp.ui.theme.AppTextDim

@Composable
fun WifiCard(wifi: WifiState, onToggle: () -> Unit) {
    val connected = wifi.connected
    val borderColor = if (connected) AppGreen.copy(alpha = 0.25f) else AppRed.copy(alpha = 0.25f)
    val gradientColors = if (connected)
        listOf(AppGreen.copy(alpha = 0.10f), AppGreen.copy(alpha = 0.04f))
    else
        listOf(AppRed.copy(alpha = 0.10f), AppRed.copy(alpha = 0.04f))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(gradientColors), RoundedCornerShape(18.dp))
            .border(1.dp, borderColor, RoundedCornerShape(18.dp))
            .padding(18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .background(if (connected) AppGreenSoft else AppRedSoft, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (connected) Icons.Rounded.Wifi else Icons.Rounded.WifiOff,
                contentDescription = null,
                tint = if (connected) AppGreen else AppRed,
                modifier = Modifier.size(20.dp),
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            if (connected) {
                Text(
                    "Connected",
                    color = AppGreen,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.2.sp,
                )
                Text(
                    wifi.ssid,
                    color = AppText,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = (-0.3).sp,
                )
                Text(
                    wifi.ip,
                    color = AppTextDim,
                    fontSize = 12.5.sp,
                    fontFamily = FontFamily.Monospace,
                )
            } else {
                Text(
                    "Disconnected",
                    color = AppRed,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.2.sp,
                )
                Text(
                    "No WiFi connection",
                    color = AppText,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = (-0.2).sp,
                )
                Text(
                    "Connect to a network to start sharing",
                    color = AppTextDim,
                    fontSize = 12.5.sp,
                )
            }
        }

        AppSwitch(checked = connected, onCheckedChange = onToggle)
    }
}

@Composable
fun AppSwitch(checked: Boolean, onCheckedChange: () -> Unit) {
    val trackColor by animateColorAsState(
        targetValue = if (checked) AppGreen else Color(0xFF94A3B8).copy(alpha = 0.25f),
        label = "trackColor",
    )
    val thumbOffset by animateDpAsState(
        targetValue = if (checked) 21.dp else 3.dp,
        label = "thumbOffset",
    )
    Box(
        modifier = Modifier
            .width(46.dp)
            .height(28.dp)
            .clip(CircleShape)
            .background(trackColor)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onCheckedChange,
            ),
    ) {
        Box(
            modifier = Modifier
                .offset(x = thumbOffset, y = 3.dp)
                .size(22.dp)
                .clip(CircleShape)
                .background(Color.White),
        )
    }
}
