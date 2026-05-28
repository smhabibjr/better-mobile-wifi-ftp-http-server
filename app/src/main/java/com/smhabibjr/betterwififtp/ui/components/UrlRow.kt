package com.smhabibjr.betterwififtp.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import android.content.ClipData
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smhabibjr.betterwififtp.ui.theme.AppAccent2
import com.smhabibjr.betterwififtp.ui.theme.AppAccentSoft
import com.smhabibjr.betterwififtp.ui.theme.AppBorder
import com.smhabibjr.betterwififtp.ui.theme.AppBorderStrong
import com.smhabibjr.betterwififtp.ui.theme.AppCard
import com.smhabibjr.betterwififtp.ui.theme.AppGreen
import com.smhabibjr.betterwififtp.ui.theme.AppGreenSoft
import com.smhabibjr.betterwififtp.ui.theme.AppText
import com.smhabibjr.betterwififtp.ui.theme.AppTextFaint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun UrlRow(label: String, hint: String, url: String, accent: Boolean = false) {
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    var copied by remember { mutableStateOf(false) }

    LaunchedEffect(copied) {
        if (copied) {
            delay(1400)
            copied = false
        }
    }

    val copyBg by animateColorAsState(
        if (copied) AppGreenSoft else Color(0xFF94A3B8).copy(alpha = 0.10f),
        label = "copyBg",
    )
    val copyFg by animateColorAsState(
        if (copied) AppGreen else AppText,
        label = "copyFg",
    )
    val copyBorder by animateColorAsState(
        if (copied) AppGreen.copy(alpha = 0.3f) else AppBorderStrong,
        label = "copyBorder",
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppCard, RoundedCornerShape(14.dp))
            .border(1.dp, AppBorder, RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            label,
            modifier = Modifier
                .width(44.dp)
                .background(
                    if (accent) AppAccentSoft else Color(0xFF94A3B8).copy(alpha = 0.08f),
                    RoundedCornerShape(8.dp),
                )
                .padding(vertical = 4.dp),
            color = if (accent) AppAccent2 else Color(0xFF94A3B8),
            fontSize = 10.5.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.6.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                url,
                color = AppText,
                fontSize = 13.5.sp,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            )
            Text(
                hint,
                color = AppTextFaint,
                fontSize = 11.sp,
            )
        }

        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(9.dp))
                .background(copyBg)
                .border(1.dp, copyBorder, RoundedCornerShape(9.dp))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {
                        scope.launch {
                            clipboard.setClipEntry(
                                ClipEntry(ClipData.newPlainText("url", url))
                            )
                        }
                        copied = true
                    },
                )
                .padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Icon(
                imageVector = if (copied) Icons.Rounded.Check else Icons.Rounded.ContentCopy,
                contentDescription = null,
                tint = copyFg,
            )
            Text(
                if (copied) "Copied" else "Copy",
                color = copyFg,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}
