package com.smhabibjr.betterwififtp.ui.screens

import androidx.compose.animation.core.InfiniteRepeatableSpec
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smhabibjr.betterwififtp.ui.AccessMode
import com.smhabibjr.betterwififtp.ui.ServerState
import com.smhabibjr.betterwififtp.ui.WifiState
import com.smhabibjr.betterwififtp.ui.components.QrCodePlaceholder
import com.smhabibjr.betterwififtp.ui.components.UrlRow
import com.smhabibjr.betterwififtp.ui.theme.AppAccent
import com.smhabibjr.betterwififtp.ui.theme.AppAccent2
import com.smhabibjr.betterwififtp.ui.theme.AppAccentSoft
import com.smhabibjr.betterwififtp.ui.theme.AppAmber
import com.smhabibjr.betterwififtp.ui.theme.AppAmberSoft
import com.smhabibjr.betterwififtp.ui.theme.AppBorder
import com.smhabibjr.betterwififtp.ui.theme.AppBorderStrong
import com.smhabibjr.betterwififtp.ui.theme.AppCard
import com.smhabibjr.betterwififtp.ui.theme.AppGreen
import com.smhabibjr.betterwififtp.ui.theme.AppGreenSoft
import com.smhabibjr.betterwififtp.ui.theme.AppText
import com.smhabibjr.betterwififtp.ui.theme.AppTextDim
import com.smhabibjr.betterwififtp.ui.theme.AppTextFaint
import kotlinx.coroutines.delay
import kotlin.random.Random

@Composable
fun ServerScreen(
    wifi: WifiState,
    folder: String,
    mode: AccessMode,
    server: ServerState,
    onStopServer: () -> Unit,
    onSimulate: () -> Unit,
) {
    val ftpUrl = "ftp://${wifi.ip}:${server.ftpPort}"
    val httpUrl = "http://${wifi.ip}:${server.httpPort}"

    var txRate by remember { mutableFloatStateOf(0f) }

    // Animate traffic counter
    LaunchedEffect(Unit) {
        while (true) {
            delay(900)
            txRate += Random.nextFloat() * 14f
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Glow effect at top
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(AppAccent.copy(alpha = 0.12f), Color.Transparent),
                    ),
                ),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
        ) {
            // ── Header ─────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    modifier = Modifier
                        .background(Color(0xFF94A3B8).copy(alpha = 0.08f), CircleShape)
                        .border(1.dp, AppBorderStrong, CircleShape)
                        .clip(CircleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onStopServer,
                        )
                        .padding(start = 9.dp, end = 12.dp, top = 7.dp, bottom = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, null, tint = AppTextDim, modifier = Modifier.size(14.dp))
                    Text("Back", color = AppTextDim, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }

                PulseStatus()

                Row(
                    modifier = Modifier
                        .border(1.dp, AppBorderStrong, CircleShape)
                        .clip(CircleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onSimulate,
                        )
                        .padding(horizontal = 11.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(Icons.Rounded.Bolt, null, tint = AppTextDim, modifier = Modifier.size(14.dp))
                    Text("Test", color = AppTextDim, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
            }

            // ── QR code card ───────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(listOf(Color(0xFF141C32), Color(0xFF0F1729))),
                        RoundedCornerShape(22.dp),
                    )
                    .border(1.dp, AppBorderStrong, RoundedCornerShape(22.dp))
                    .padding(horizontal = 20.dp, vertical = 22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    "Scan to connect",
                    color = AppTextDim,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.4.sp,
                )

                Box(
                    modifier = Modifier
                        .padding(top = 14.dp)
                        .background(Color(0xFFE2E8F0), RoundedCornerShape(18.dp))
                        .padding(12.dp),
                ) {
                    QrCodePlaceholder(
                        modifier = Modifier.size(184.dp),
                        seed = httpUrl,
                    )
                }

                Row(
                    modifier = Modifier.padding(top = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(wifi.ssid, color = AppTextDim, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                    Box(modifier = Modifier.size(3.dp).background(AppTextFaint, CircleShape))
                    Box(
                        modifier = Modifier
                            .background(
                                if (mode == AccessMode.ReadWrite) AppAmberSoft else Color(0xFF94A3B8).copy(alpha = 0.10f),
                                CircleShape,
                            )
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                    ) {
                        Text(
                            if (mode == AccessMode.ReadWrite) "Read + Write" else "Read-only",
                            color = if (mode == AccessMode.ReadWrite) AppAmber else AppTextDim,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }

            // ── URL rows ───────────────────────────────────────────
            Column(
                modifier = Modifier.padding(top = 14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                UrlRow(label = "HTTP", hint = "Browser", url = httpUrl, accent = true)
                UrlRow(label = "FTP", hint = "FTP client · port ${server.ftpPort}", url = ftpUrl, accent = false)
                if (server.password.isNotEmpty()) {
                    UrlRow(label = "PWD", hint = "FTP password — enter in FileZilla", url = server.password, accent = false)
                }
            }

            // ── Stats row ──────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp)
                    .background(AppCard, RoundedCornerShape(16.dp))
                    .border(1.dp, AppBorder, RoundedCornerShape(16.dp))
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(AppAccentSoft, RoundedCornerShape(11.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.People, null, tint = AppAccent2, modifier = Modifier.size(16.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Row {
                        Text(
                            "Connected clients: ",
                            color = AppText,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            server.clients.toString(),
                            color = AppAccent2,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    Text(
                        "$folder  ·  ${"%.1f".format(txRate)} KB/s",
                        color = AppTextFaint,
                        fontSize = 11.5.sp,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Box(
                    modifier = Modifier
                        .background(AppGreenSoft, RoundedCornerShape(999.dp))
                        .padding(horizontal = 9.dp, vertical = 4.dp),
                ) {
                    Text(
                        "LIVE",
                        color = AppGreen,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.4.sp,
                    )
                }
            }

            Spacer(Modifier.height(18.dp))

            // ── Stop Server button ─────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .background(
                        Brush.verticalGradient(listOf(Color(0xFFF87171), Color(0xFFDC2626))),
                        RoundedCornerShape(16.dp),
                    )
                    .clip(RoundedCornerShape(16.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onStopServer,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Icon(Icons.Rounded.PowerSettingsNew, null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Text(
                        "Stop Server",
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = (-0.2).sp,
                    )
                }
            }

            Text(
                buildString {
                    if (mode == AccessMode.ReadWrite) {
                        append("Clients can upload and modify files.\n")
                    } else {
                        append("Files are served read-only.\n")
                    }
                    append("Stopping the server disconnects all clients.")
                },
                color = AppTextFaint,
                fontSize = 11.5.sp,
                lineHeight = 17.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp),
            )
        }
    }
}

@Composable
private fun PulseStatus() {
    val transition = rememberInfiniteTransition(label = "pulse")
    val dotScale by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = InfiniteRepeatableSpec(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "dotScale",
    )
    val ringScale by transition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.6f,
        animationSpec = InfiniteRepeatableSpec(
            animation = tween(1600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "ringScale",
    )
    val ringAlpha by transition.animateFloat(
        initialValue = 0.55f,
        targetValue = 0f,
        animationSpec = InfiniteRepeatableSpec(
            animation = tween(1600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "ringAlpha",
    )

    Row(
        modifier = Modifier
            .background(AppGreen.copy(alpha = 0.10f), CircleShape)
            .border(1.dp, AppGreen.copy(alpha = 0.3f), CircleShape)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier.size(9.dp).wrapContentSize(Alignment.Center),
        ) {
            // Pulse ring
            Box(
                modifier = Modifier
                    .size(9.dp)
                    .scale(ringScale)
                    .background(AppGreen.copy(alpha = ringAlpha), CircleShape),
            )
            // Solid dot
            Box(
                modifier = Modifier
                    .size(9.dp)
                    .scale(dotScale)
                    .background(AppGreen, CircleShape),
            )
        }
        Text(
            "Server running",
            color = AppGreen,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = (-0.1).sp,
        )
    }
}
