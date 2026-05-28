package com.smhabibjr.betterwififtp.storage

import android.content.Context
import android.os.Environment
import android.os.StatFs
import android.os.storage.StorageManager
import com.smhabibjr.betterwififtp.ui.FolderInfo
import com.smhabibjr.betterwififtp.ui.VolumeInfo
import java.io.File

object StorageHelper {

    private val SCAN_DIRS = listOf(
        "Download", "DCIM", "DCIM/Camera", "Pictures", "Documents",
        "Music", "Movies", "WhatsApp/Media",
    )

    fun getAvailableVolumes(context: Context): List<VolumeInfo> {
        val sm = context.getSystemService(Context.STORAGE_SERVICE) as StorageManager
        val result = mutableListOf<VolumeInfo>()

        for (vol in sm.storageVolumes) {
            if (vol.state != Environment.MEDIA_MOUNTED) continue

            val rootPath: String = if (vol.isPrimary) {
                Environment.getExternalStorageDirectory().absolutePath
            } else {
                @Suppress("DEPRECATION")
                vol.uuid?.let { "/storage/$it" } ?: continue
            }

            val root = File(rootPath)
            if (!root.exists()) continue

            val stat = StatFs(rootPath)
            val totalBytes = stat.blockCountLong * stat.blockSizeLong
            val availBytes = stat.availableBlocksLong * stat.blockSizeLong
            val usedBytes = totalBytes - availBytes
            val totalGb = (totalBytes / (1024L * 1024 * 1024)).toInt().coerceAtLeast(1)
            val usedGb = (usedBytes / (1024L * 1024 * 1024)).toInt()

            val folders = mutableListOf<FolderInfo>()
            for (rel in SCAN_DIRS) {
                val dir = File(root, rel)
                if (!dir.exists() || !dir.isDirectory) continue
                val count = dir.listFiles()?.size ?: 0
                val sizeBytes = dir.walkTopDown().maxDepth(3).filter { it.isFile }.sumOf { it.length() }
                val meta = when {
                    sizeBytes >= 1_073_741_824L -> "$count items · ${"%.1f".format(sizeBytes / 1_073_741_824.0)} GB"
                    sizeBytes >= 1_048_576L -> "$count items · ${"%.0f".format(sizeBytes / 1_048_576.0)} MB"
                    sizeBytes >= 1_024L -> "$count items · ${"%.0f".format(sizeBytes / 1_024.0)} KB"
                    else -> "$count items"
                }
                folders.add(FolderInfo(path = dir.absolutePath, name = rel, meta = meta))
            }

            val label = if (vol.isPrimary) "Internal Storage" else (vol.getDescription(context) ?: "SD Card")
            val short = if (vol.isPrimary) "Internal" else "SD Card"
            val sub = if (vol.isPrimary) "Phone memory" else (vol.uuid ?: "External")

            result.add(
                VolumeInfo(
                    id = if (vol.isPrimary) "internal" else (vol.uuid ?: "external"),
                    label = label,
                    short = short,
                    sub = sub,
                    totalGb = totalGb,
                    usedGb = usedGb,
                    folders = folders,
                )
            )
        }

        return result
    }
}
