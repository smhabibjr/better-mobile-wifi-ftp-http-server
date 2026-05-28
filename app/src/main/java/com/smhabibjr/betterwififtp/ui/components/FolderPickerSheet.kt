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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.SdCard
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smhabibjr.betterwififtp.ui.FolderInfo
import com.smhabibjr.betterwififtp.ui.VolumeInfo
import com.smhabibjr.betterwififtp.ui.theme.AppAccent
import com.smhabibjr.betterwififtp.ui.theme.AppAccent2
import com.smhabibjr.betterwififtp.ui.theme.AppAccentSoft
import com.smhabibjr.betterwififtp.ui.theme.AppAmber
import com.smhabibjr.betterwififtp.ui.theme.AppBg2
import com.smhabibjr.betterwififtp.ui.theme.AppBorder
import com.smhabibjr.betterwififtp.ui.theme.AppBorderStrong
import com.smhabibjr.betterwififtp.ui.theme.AppCard
import com.smhabibjr.betterwififtp.ui.theme.AppText
import com.smhabibjr.betterwififtp.ui.theme.AppTextDim
import com.smhabibjr.betterwififtp.ui.theme.AppTextFaint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderPickerSheet(
    volumes: List<VolumeInfo>,
    currentFolder: String,
    onPick: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    val initialVolume = volumes.indexOfFirst { v -> v.folders.any { it.path == currentFolder } }
    var selectedVolume by remember { mutableIntStateOf(if (initialVolume >= 0) initialVolume else 0) }

    // Subfolder navigation stack: list of paths drilled into
    var navStack by remember { mutableStateOf<List<String>>(emptyList()) }
    var navFolders by remember { mutableStateOf<List<FolderInfo>>(emptyList()) }

    // Reset stack when switching volumes
    LaunchedEffect(selectedVolume) { navStack = emptyList() }

    // Load sub-level folders from IO thread
    val currentNavPath = navStack.lastOrNull()
    LaunchedEffect(currentNavPath) {
        if (currentNavPath == null) {
            navFolders = emptyList()
        } else {
            navFolders = withContext(Dispatchers.IO) {
                File(currentNavPath)
                    .listFiles { f -> f.isDirectory && !f.name.startsWith('.') }
                    ?.sortedBy { it.name.lowercase() }
                    ?.map { subdir ->
                        val count = subdir.listFiles()?.size ?: 0
                        val sizeBytes = subdir.walkTopDown().maxDepth(2).filter { it.isFile }.sumOf { it.length() }
                        val meta = when {
                            sizeBytes >= 1_073_741_824L -> "$count items · ${"%.1f".format(sizeBytes / 1_073_741_824.0)} GB"
                            sizeBytes >= 1_048_576L -> "$count items · ${"%.0f".format(sizeBytes / 1_048_576.0)} MB"
                            sizeBytes >= 1_024L -> "$count items · ${"%.0f".format(sizeBytes / 1_024.0)} KB"
                            else -> "$count items"
                        }
                        FolderInfo(path = subdir.absolutePath, name = subdir.name, meta = meta)
                    } ?: emptyList()
            }
        }
    }

    val vol = if (volumes.isNotEmpty()) volumes[selectedVolume.coerceAtMost(volumes.lastIndex)] else null
    val usedPct = vol?.let { (it.usedGb.toFloat() / it.totalGb.toFloat()).coerceIn(0f, 1f) } ?: 0f
    val displayedFolders = if (currentNavPath == null) (vol?.folders ?: emptyList()) else navFolders

    fun fmtGb(gb: Int) = if (gb >= 1024) "%.1f TB".format(gb / 1024f) else "$gb GB"

    val volumeIcons: List<ImageVector> = listOf(
        Icons.Rounded.PhoneAndroid,
        Icons.Rounded.Storage,
        Icons.Rounded.SdCard,
    )

    fun dismiss() {
        scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
    }

    fun pick(path: String) {
        onPick(path)
        dismiss()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
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
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp, horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Back button when navigated, else title
                if (currentNavPath != null) {
                    Row(
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { navStack = navStack.dropLast(1) },
                            )
                            .padding(end = 10.dp, top = 4.dp, bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowBack,
                            null,
                            tint = AppTextDim,
                            modifier = Modifier.size(16.dp),
                        )
                        Text("Back", color = AppTextDim, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    }
                    Text(
                        File(currentNavPath).name,
                        color = AppText,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                        textAlign = TextAlign.Center,
                    )
                } else {
                    Column {
                        Text(
                            "Choose folder",
                            color = AppText,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = (-0.3).sp,
                        )
                        Text(
                            "Pick from any connected storage",
                            color = AppTextFaint,
                            fontSize = 12.sp,
                        )
                    }
                }
                IconButton(onClick = ::dismiss) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(Color(0xFF94A3B8).copy(alpha = 0.12f), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Rounded.Close, null, tint = AppTextDim, modifier = Modifier.size(14.dp))
                    }
                }
            }

            // Volume tabs — only show at root level
            if (currentNavPath == null && volumes.size > 1) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(AppCard, RoundedCornerShape(12.dp))
                        .border(1.dp, AppBorder, RoundedCornerShape(12.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    volumes.forEachIndexed { i, v ->
                        val active = i == selectedVolume
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(9.dp))
                                .background(
                                    if (active) AppAccent.copy(alpha = 0.18f) else Color.Transparent,
                                )
                                .border(
                                    1.dp,
                                    if (active) AppAccent.copy(alpha = 0.35f) else Color.Transparent,
                                    RoundedCornerShape(9.dp),
                                )
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = { selectedVolume = i },
                                )
                                .padding(vertical = 8.dp, horizontal = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(3.dp),
                        ) {
                            Icon(
                                imageVector = volumeIcons.getOrElse(i) { Icons.Rounded.Storage },
                                contentDescription = null,
                                tint = if (active) AppAccent2 else AppTextDim,
                                modifier = Modifier.size(16.dp),
                            )
                            Text(
                                v.short,
                                color = if (active) AppAccent2 else AppTextDim,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            // Capacity card — only at root level
            if (currentNavPath == null && vol != null) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(AppCard, RoundedCornerShape(14.dp))
                        .border(1.dp, AppBorder, RoundedCornerShape(14.dp))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .background(AppAccentSoft, RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                volumeIcons.getOrElse(selectedVolume) { Icons.Rounded.Storage },
                                null,
                                tint = AppAccent2,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(vol.label, color = AppText, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            Text(vol.sub, color = AppTextFaint, fontSize = 11.5.sp)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                fmtGb(vol.totalGb - vol.usedGb),
                                color = AppText,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = FontFamily.Monospace,
                            )
                            Text("free of ${fmtGb(vol.totalGb)}", color = AppTextFaint, fontSize = 10.5.sp)
                        }
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(5.dp)
                            .background(Color(0xFF94A3B8).copy(alpha = 0.12f), CircleShape),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(usedPct)
                                .height(5.dp)
                                .background(
                                    if (usedPct > 0.85f) Brush.horizontalGradient(listOf(AppAmber, AppAmber))
                                    else Brush.horizontalGradient(listOf(AppAccent, AppAccent2)),
                                    CircleShape,
                                ),
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            // Folder list
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .background(AppCard, RoundedCornerShape(16.dp))
                    .border(1.dp, AppBorder, RoundedCornerShape(16.dp))
                    .verticalScroll(rememberScrollState()),
            ) {
                // "Select this folder" row when navigated
                if (currentNavPath != null) {
                    SelectCurrentRow(
                        path = currentNavPath,
                        active = currentNavPath == currentFolder,
                        onSelect = { pick(currentNavPath) },
                    )
                    if (navFolders.isNotEmpty()) {
                        Box(Modifier.fillMaxWidth().height(1.dp).background(AppBorder))
                    }
                }

                displayedFolders.forEachIndexed { i, folder ->
                    FolderRow(
                        folder = folder,
                        active = folder.path == currentFolder,
                        showDivider = i > 0 || currentNavPath != null,
                        onSelect = { pick(folder.path) },
                        onNavigate = { navStack = navStack + folder.path },
                    )
                }

                if (displayedFolders.isEmpty() && currentNavPath != null) {
                    Text(
                        "No subfolders here",
                        color = AppTextFaint,
                        fontSize = 13.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        textAlign = TextAlign.Center,
                    )
                }
            }

            Text(
                if (currentNavPath != null)
                    "Tap to select · › to go deeper"
                else
                    "Tap to select a folder · › to browse subfolders",
                color = AppTextFaint,
                fontSize = 11.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun SelectCurrentRow(path: String, active: Boolean, onSelect: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onSelect,
            )
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .background(AppAccentSoft, RoundedCornerShape(9.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.Check, null, tint = AppAccent2, modifier = Modifier.size(16.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "Select this folder",
                color = if (active) AppAccent2 else AppText,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                File(path).name,
                color = AppTextFaint,
                fontSize = 11.5.sp,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun FolderRow(
    folder: FolderInfo,
    active: Boolean,
    showDivider: Boolean,
    onSelect: () -> Unit,
    onNavigate: () -> Unit,
) {
    Column {
        if (showDivider) {
            Box(Modifier.fillMaxWidth().height(1.dp).background(AppBorder))
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Selectable main area
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onSelect,
                    )
                    .padding(start = 14.dp, top = 12.dp, bottom = 12.dp, end = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .background(
                            if (active) AppAccentSoft else Color(0xFF94A3B8).copy(alpha = 0.08f),
                            RoundedCornerShape(9.dp),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Rounded.FolderOpen,
                        null,
                        tint = if (active) AppAccent2 else AppTextDim,
                        modifier = Modifier.size(18.dp),
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        folder.name,
                        color = AppText,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        folder.meta,
                        color = AppTextFaint,
                        fontSize = 11.5.sp,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (active) {
                    Icon(Icons.Rounded.Check, null, tint = AppAccent2, modifier = Modifier.size(14.dp))
                }
            }

            // Navigate-into button (always visible, clearly separate tap target)
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(topEnd = 0.dp, bottomEnd = 0.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onNavigate,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Rounded.ChevronRight,
                    null,
                    tint = if (active) AppAccent2 else AppTextFaint,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}
