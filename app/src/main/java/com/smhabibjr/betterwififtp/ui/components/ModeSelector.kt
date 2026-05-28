package com.smhabibjr.betterwififtp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smhabibjr.betterwififtp.ui.AccessMode
import com.smhabibjr.betterwififtp.ui.theme.AppAccent
import com.smhabibjr.betterwififtp.ui.theme.AppAccent2
import com.smhabibjr.betterwififtp.ui.theme.AppAmber
import com.smhabibjr.betterwififtp.ui.theme.AppBorder
import com.smhabibjr.betterwififtp.ui.theme.AppCard
import com.smhabibjr.betterwififtp.ui.theme.AppText
import com.smhabibjr.betterwififtp.ui.theme.AppTextDim
import com.smhabibjr.betterwififtp.ui.theme.AppTextFaint

@Composable
fun ModeSelector(mode: AccessMode, onChange: (AccessMode) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppCard, RoundedCornerShape(16.dp))
            .border(1.dp, AppBorder, RoundedCornerShape(16.dp))
            .padding(5.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        ModeOption(
            modifier = Modifier.weight(1f),
            id = AccessMode.ReadOnly,
            title = "Read only",
            subtitle = "Clients can only download",
            icon = Icons.Rounded.ArrowDownward,
            active = mode == AccessMode.ReadOnly,
            warn = false,
            onClick = { onChange(AccessMode.ReadOnly) },
        )
        ModeOption(
            modifier = Modifier.weight(1f),
            id = AccessMode.ReadWrite,
            title = "Read + Write",
            subtitle = "Clients can upload + edit",
            icon = Icons.Rounded.ArrowUpward,
            active = mode == AccessMode.ReadWrite,
            warn = true,
            onClick = { onChange(AccessMode.ReadWrite) },
        )
    }
}

@Composable
private fun ModeOption(
    modifier: Modifier = Modifier,
    id: AccessMode,
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    active: Boolean,
    warn: Boolean,
    onClick: () -> Unit,
) {
    val isWarn = active && warn
    val accentColor = if (isWarn) AppAmber else AppAccent2
    val bgColors = when {
        isWarn -> listOf(AppAmber.copy(alpha = 0.18f), AppAmber.copy(alpha = 0.08f))
        active -> listOf(AppAccent.copy(alpha = 0.20f), AppAccent.copy(alpha = 0.06f))
        else -> listOf(Color.Transparent, Color.Transparent)
    }
    val borderColor = when {
        isWarn -> AppAmber.copy(alpha = 0.4f)
        active -> AppAccent.copy(alpha = 0.4f)
        else -> Color.Transparent
    }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Brush.verticalGradient(bgColors))
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (active) accentColor else AppTextDim,
                modifier = Modifier.size(16.dp),
            )
            Text(
                title,
                color = if (active) AppText else AppTextDim,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.1).sp,
                modifier = Modifier.weight(1f),
            )
            if (isWarn) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .background(AppAmber, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "!",
                        color = Color(0xFF3F2D02),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                    )
                }
            }
        }
        Text(
            subtitle,
            color = AppTextFaint,
            fontSize = 11.5.sp,
            lineHeight = 15.sp,
        )
    }
}
