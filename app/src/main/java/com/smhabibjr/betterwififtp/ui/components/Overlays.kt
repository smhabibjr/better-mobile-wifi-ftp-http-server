package com.smhabibjr.betterwififtp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.smhabibjr.betterwififtp.ui.BannerData
import com.smhabibjr.betterwififtp.ui.Screen
import com.smhabibjr.betterwififtp.ui.ToastData
import com.smhabibjr.betterwififtp.ui.theme.AppAmber
import com.smhabibjr.betterwififtp.ui.theme.AppBg2
import com.smhabibjr.betterwififtp.ui.theme.AppBorder
import com.smhabibjr.betterwififtp.ui.theme.AppBorderStrong
import com.smhabibjr.betterwififtp.ui.theme.AppCard
import com.smhabibjr.betterwififtp.ui.theme.AppGreen
import com.smhabibjr.betterwififtp.ui.theme.AppRed
import com.smhabibjr.betterwififtp.ui.theme.AppRedSoft
import com.smhabibjr.betterwififtp.ui.theme.AppText
import com.smhabibjr.betterwififtp.ui.theme.AppTextDim
import com.smhabibjr.betterwififtp.ui.theme.AppTextFaint
import kotlinx.coroutines.launch

// ── Banner (slides from top) ──────────────────────────────────

@Composable
fun BannerOverlay(banner: BannerData, onClose: () -> Unit) {
    val bg = when (banner.tone) {
        "red" -> Brush.verticalGradient(listOf(Color(0xFFDC2626), Color(0xFFB91C1C)))
        "amber" -> Brush.verticalGradient(listOf(Color(0xFFF59E0B), Color(0xFFD97706)))
        else -> Brush.verticalGradient(listOf(Color(0xFF06B6D4), Color(0xFF0891B2)))
    }

    Row(
        modifier = Modifier
            .padding(horizontal = 12.dp)
            .fillMaxWidth()
            .background(bg, RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(Icons.Rounded.Warning, null, tint = Color.White, modifier = Modifier.size(18.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                banner.title,
                color = Color.White,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.1).sp,
            )
            Text(banner.message, color = Color.White.copy(alpha = 0.92f), fontSize = 12.sp)
        }
        Box(
            modifier = Modifier
                .size(24.dp)
                .background(Color.White.copy(alpha = 0.18f), CircleShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClose,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.Close, null, tint = Color.White, modifier = Modifier.size(12.dp))
        }
    }
}

// ── Toast (slides from bottom) ────────────────────────────────

@Composable
fun ToastOverlay(toast: ToastData) {
    val bg = when (toast.tone) {
        "amber" -> Color(0xFFFBBF24).copy(alpha = 0.95f)
        "red" -> Color(0xFFF87171).copy(alpha = 0.95f)
        else -> Color(0xFF0F1729).copy(alpha = 0.95f)
    }
    val fg = if (toast.tone == "amber") Color(0xFF3F2D02) else Color.White

    Row(
        modifier = Modifier
            .padding(horizontal = 24.dp)
            .fillMaxWidth()
            .background(bg, RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(Icons.Rounded.Warning, null, tint = fg, modifier = Modifier.size(18.dp))
        Text(toast.message, color = fg, fontSize = 13.5.sp, fontWeight = FontWeight.Medium)
    }
}

// ── Permission modal ──────────────────────────────────────────

@Composable
fun PermissionModal(onClose: () -> Unit) {
    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClose,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier
                    .padding(28.dp)
                    .fillMaxWidth()
                    .background(AppBg2, RoundedCornerShape(22.dp))
                    .border(1.dp, AppBorderStrong, RoundedCornerShape(22.dp))
                    .padding(22.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {},
                    ),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(AppRedSoft, RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.Lock, null, tint = AppRed, modifier = Modifier.size(28.dp))
                }
                Spacer(Modifier.height(14.dp))
                Text(
                    "Storage permission required",
                    color = AppText,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = (-0.3).sp,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "WiFi FTP Share needs access to your files to share them on your local network.",
                    color = AppTextDim,
                    fontSize = 13.5.sp,
                    lineHeight = 20.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
                Spacer(Modifier.height(18.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .border(1.dp, AppBorderStrong, RoundedCornerShape(12.dp))
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = onClose,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("Not now", color = AppTextDim, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .background(
                                Brush.verticalGradient(listOf(Color(0xFF22D3EE), Color(0xFF06B6D4))),
                                RoundedCornerShape(12.dp),
                            )
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = onClose,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("Allow", color = Color(0xFF031018), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ── Simulate sheet ────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimulateSheet(
    screen: Screen,
    wifiOn: Boolean,
    onPick: (String) -> Unit,
    onClose: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    data class SimItem(
        val id: String,
        val label: String,
        val sub: String,
        val tone: String,
        val disabled: Boolean = false,
    )

    val items = listOf(
        SimItem("wifiLost", "WiFi lost while server running", "Red banner — auto stops server", "red", disabled = screen != Screen.Server),
        SimItem("permission", "Storage permission denied", "Modal popup — request permission", "red"),
        SimItem("portBusy", "Port 21 busy", "Yellow toast — falls back to 2121", "amber"),
        SimItem("toggleWifi", if (wifiOn) "Disconnect WiFi" else "Reconnect WiFi", "Toggle WiFi state", "neutral"),
    )

    fun dismiss() {
        scope.launch { sheetState.hide() }.invokeOnCompletion { onClose() }
    }

    ModalBottomSheet(
        onDismissRequest = onClose,
        sheetState = sheetState,
        containerColor = AppBg2,
        contentColor = AppText,
        tonalElevation = 0.dp,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 2.dp)
                    .width(38.dp)
                    .height(4.dp)
                    .background(AppTextDim.copy(alpha = 0.3f), CircleShape),
            )
        },
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .navigationBarsPadding(),
        ) {
            Column(modifier = Modifier.padding(horizontal = 4.dp, vertical = 12.dp)) {
                Text(
                    "Simulate state",
                    color = AppText,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = (-0.3).sp,
                )
                Text("Trigger error states and validation flows.", color = AppTextFaint, fontSize = 12.sp)
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AppCard, RoundedCornerShape(16.dp))
                    .border(1.dp, AppBorder, RoundedCornerShape(16.dp)),
            ) {
                items.forEachIndexed { i, item ->
                    val dotColor = when (item.tone) {
                        "red" -> AppRed
                        "amber" -> AppAmber
                        else -> AppTextDim
                    }
                    Column {
                        if (i > 0) {
                            Box(Modifier.fillMaxWidth().height(1.dp).background(AppBorder))
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .then(
                                    if (!item.disabled) Modifier.clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null,
                                        onClick = { onPick(item.id) },
                                    ) else Modifier
                                )
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(
                                        dotColor.copy(alpha = if (item.disabled) 0.4f else 1f),
                                        CircleShape,
                                    ),
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    item.label,
                                    color = if (item.disabled) AppTextFaint else AppText,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                )
                                Text(item.sub, color = AppTextFaint, fontSize = 11.5.sp)
                            }
                            Icon(
                                Icons.Rounded.ChevronRight,
                                null,
                                tint = AppTextFaint,
                                modifier = Modifier.size(14.dp),
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}
