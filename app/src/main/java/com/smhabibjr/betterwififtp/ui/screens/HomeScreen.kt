package com.smhabibjr.betterwififtp.ui.screens

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smhabibjr.betterwififtp.ui.AccessMode
import com.smhabibjr.betterwififtp.ui.VolumeInfo
import com.smhabibjr.betterwififtp.ui.WifiState
import com.smhabibjr.betterwififtp.ui.components.FolderPickerSheet
import com.smhabibjr.betterwififtp.ui.components.ModeSelector
import com.smhabibjr.betterwififtp.ui.components.WifiCard
import com.smhabibjr.betterwififtp.ui.theme.AppAccent
import com.smhabibjr.betterwififtp.ui.theme.AppAccent2
import com.smhabibjr.betterwififtp.ui.theme.AppAccentSoft
import com.smhabibjr.betterwififtp.ui.theme.AppBorder
import com.smhabibjr.betterwififtp.ui.theme.AppBorderStrong
import com.smhabibjr.betterwififtp.ui.theme.AppCard
import com.smhabibjr.betterwififtp.ui.theme.AppCard2
import com.smhabibjr.betterwififtp.ui.theme.AppText
import com.smhabibjr.betterwififtp.ui.theme.AppTextDim
import com.smhabibjr.betterwififtp.ui.theme.AppTextFaint

@Composable
fun HomeScreen(
    wifi: WifiState,
    folder: String,
    mode: AccessMode,
    volumes: List<VolumeInfo>,
    ftpUsername: String,
    ftpPassword: String,
    onToggleWifi: () -> Unit,
    onSetFolder: (String) -> Unit,
    onSetMode: (AccessMode) -> Unit,
    onSetUsername: (String) -> Unit,
    onSetPassword: (String) -> Unit,
    onGeneratePassword: () -> Unit,
    onStartServer: () -> Unit,
    onSimulate: () -> Unit,
) {
    val canStart = wifi.connected && folder.isNotEmpty()
    var showFolderPicker by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp)
            .padding(bottom = 28.dp),
    ) {
        // ── Header ──────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp, bottom = 18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .background(
                            Brush.linearGradient(listOf(Color(0xFF22D3EE), Color(0xFF0891B2))),
                            RoundedCornerShape(10.dp),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.Wifi, null, tint = Color(0xFF031018), modifier = Modifier.size(18.dp))
                }
                Column {
                    Text(
                        "WiFi FTP Share",
                        color = AppText,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = (-0.3).sp,
                    )
                    Text(
                        "Local network file server",
                        color = AppTextFaint,
                        fontSize = 11.sp,
                    )
                }
            }
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
                Text("Simulate", color = AppTextDim, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }
        }

        // ── WiFi status card ─────────────────────────────────────
        WifiCard(wifi = wifi, onToggle = onToggleWifi)

        // ── Shared folder ─────────────────────────────────────────
        SectionLabel("Shared folder")
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(AppCard, RoundedCornerShape(18.dp))
                .border(1.dp, AppBorder, RoundedCornerShape(18.dp))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { showFolderPicker = true },
                )
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(
                        if (folder.isNotEmpty()) AppAccentSoft else Color(0xFF94A3B8).copy(alpha = 0.08f),
                        RoundedCornerShape(12.dp),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Rounded.FolderOpen,
                    null,
                    tint = if (folder.isNotEmpty()) AppAccent2 else AppTextFaint,
                    modifier = Modifier.size(22.dp),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (folder.isNotEmpty()) folder else "No folder selected",
                    color = if (folder.isNotEmpty()) AppText else AppTextFaint,
                    fontSize = 15.sp,
                    fontWeight = if (folder.isNotEmpty()) FontWeight.Medium else FontWeight.Normal,
                    fontFamily = if (folder.isNotEmpty()) FontFamily.Monospace else null,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = if (folder.isNotEmpty()) "Tap to change" else "Choose what to share",
                    color = AppTextFaint,
                    fontSize = 12.sp,
                )
            }
            Icon(Icons.Rounded.ChevronRight, null, tint = AppTextFaint, modifier = Modifier.size(14.dp))
        }

        // ── Access mode ───────────────────────────────────────────
        SectionLabel("Access mode")
        ModeSelector(mode = mode, onChange = onSetMode)

        // ── Security (credentials) ────────────────────────────────
        SectionLabel("Security")
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(AppCard, RoundedCornerShape(18.dp))
                .border(1.dp, AppBorder, RoundedCornerShape(18.dp))
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            // Username row
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .background(AppAccentSoft, RoundedCornerShape(11.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.Person, null, tint = AppAccent2, modifier = Modifier.size(18.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("Username", color = AppTextFaint, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.6.sp)
                    BasicTextField(
                        value = ftpUsername,
                        onValueChange = onSetUsername,
                        singleLine = true,
                        textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 14.sp, color = AppText),
                        cursorBrush = SolidColor(AppAccent2),
                        modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                    )
                }
            }

            // Divider
            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(AppBorder))

            // Password row
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .background(AppAccentSoft, RoundedCornerShape(11.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.Lock, null, tint = AppAccent2, modifier = Modifier.size(18.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("Password", color = AppTextFaint, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.6.sp)
                    BasicTextField(
                        value = ftpPassword,
                        onValueChange = onSetPassword,
                        singleLine = true,
                        textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 14.sp, color = AppText),
                        cursorBrush = SolidColor(AppAccent2),
                        modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                    )
                }
                Box(
                    modifier = Modifier
                        .border(1.dp, AppBorderStrong, CircleShape)
                        .clip(CircleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onGeneratePassword,
                        )
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                ) {
                    Text("Generate", color = AppTextDim, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                }
            }

            Text(
                "Credentials saved automatically",
                color = AppTextFaint,
                fontSize = 11.sp,
                modifier = Modifier.padding(top = 10.dp),
            )
        }

        // ── Settings preview row ──────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp)
                .background(AppCard2.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                .border(1.dp, AppBorder, RoundedCornerShape(14.dp))
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            SettingsCell("Protocol", "FTP + HTTP", modifier = Modifier.weight(1f))
            Box(modifier = Modifier.width(1.dp).height(32.dp).background(AppBorder))
            SettingsCell("Port", "2121", modifier = Modifier.weight(1f))
            Box(modifier = Modifier.width(1.dp).height(32.dp).background(AppBorder))
            SettingsCell(
                label = "Mode",
                value = if (mode == AccessMode.ReadWrite) "Read + Write" else "Read-only",
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(Modifier.height(38.dp))

        // ── Start Server button ───────────────────────────────────
        val btnBg = if (canStart)
            Brush.verticalGradient(listOf(Color(0xFF22D3EE), Color(0xFF06B6D4)))
        else
            Brush.verticalGradient(listOf(Color(0xFF1A2340), Color(0xFF141C32)))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .background(btnBg, RoundedCornerShape(16.dp))
                .then(
                    if (!canStart) Modifier.border(1.dp, AppBorder, RoundedCornerShape(16.dp))
                    else Modifier
                )
                .clip(RoundedCornerShape(16.dp))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    enabled = canStart,
                    onClick = onStartServer,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(
                    Icons.Rounded.PowerSettingsNew,
                    null,
                    tint = if (canStart) Color(0xFF031018) else AppTextDim.copy(alpha = 0.4f),
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = when {
                        canStart -> "Start Server"
                        !wifi.connected -> "WiFi Required"
                        else -> "Select a Folder"
                    },
                    color = if (canStart) Color(0xFF031018) else AppText.copy(alpha = 0.4f),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = (-0.2).sp,
                )
            }
        }

        Text(
            "Only devices on the same WiFi can access your files.\nNothing leaves your local network.",
            color = AppTextFaint,
            fontSize = 11.5.sp,
            lineHeight = 17.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 14.dp),
        )
    }

    if (showFolderPicker) {
        FolderPickerSheet(
            volumes = volumes,
            currentFolder = folder,
            onPick = { onSetFolder(it) },
            onDismiss = { showFolderPicker = false },
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        color = AppTextFaint,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 1.2.sp,
        modifier = Modifier.padding(start = 4.dp, top = 14.dp, bottom = 4.dp),
    )
}

@Composable
private fun SettingsCell(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            label,
            color = AppTextFaint,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.8.sp,
        )
        Text(
            value,
            color = AppText,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
