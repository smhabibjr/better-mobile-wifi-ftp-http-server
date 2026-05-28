package com.smhabibjr.betterwififtp.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color

@Composable
fun QrCodePlaceholder(modifier: Modifier = Modifier, seed: String = "http://192.168.1.5:8080") {
    val n = 25
    val cells = remember(seed) {
        val out = BooleanArray(n * n)
        var h = 2166136261L
        for (c in seed) {
            h = h xor c.code.toLong()
            h = (h * 16777619L) and 0xFFFFFFFFL
        }
        for (y in 0 until n) {
            for (x in 0 until n) {
                h = h xor ((x.toLong() * 73856093L) xor (y.toLong() * 19349663L))
                h = (h * 16777619L) and 0xFFFFFFFFL
                out[y * n + x] = ((h ushr 13) and 1L) == 1L
            }
        }
        out
    }

    Canvas(modifier = modifier) {
        val cellPx = size.width / n
        val bg = Color(0xFFE2E8F0)
        val dark = Color(0xFF0F1729)

        drawRoundRect(color = bg, cornerRadius = CornerRadius(28f))

        for (i in cells.indices) {
            val x = i % n
            val y = i / n
            val isFinder = (x < 7 && y < 7) || (x >= n - 7 && y < 7) || (x < 7 && y >= n - 7)
            if (isFinder || !cells[i]) continue
            drawRoundRect(
                color = dark,
                topLeft = Offset(x * cellPx + 0.5f, y * cellPx + 0.5f),
                size = Size(cellPx - 1f, cellPx - 1f),
                cornerRadius = CornerRadius(cellPx * 0.18f),
            )
        }

        fun drawFinder(cx: Int, cy: Int) {
            drawRoundRect(
                color = dark,
                topLeft = Offset(cx * cellPx, cy * cellPx),
                size = Size(cellPx * 7, cellPx * 7),
                cornerRadius = CornerRadius(cellPx * 1.6f),
            )
            drawRoundRect(
                color = bg,
                topLeft = Offset((cx + 1) * cellPx, (cy + 1) * cellPx),
                size = Size(cellPx * 5, cellPx * 5),
                cornerRadius = CornerRadius(cellPx * 1.2f),
            )
            drawRoundRect(
                color = dark,
                topLeft = Offset((cx + 2) * cellPx, (cy + 2) * cellPx),
                size = Size(cellPx * 3, cellPx * 3),
                cornerRadius = CornerRadius(cellPx * 0.7f),
            )
        }

        drawFinder(0, 0)
        drawFinder(n - 7, 0)
        drawFinder(0, n - 7)

        // Center logo (36/220 fraction of total size)
        val logoW = size.width * (36f / 220f)
        val logoX = size.width / 2f - logoW / 2f
        val logoY = size.height / 2f - logoW / 2f

        drawRoundRect(
            color = dark,
            topLeft = Offset(logoX, logoY),
            size = Size(logoW, logoW),
            cornerRadius = CornerRadius(logoW * 0.25f),
        )
        val pad = logoW * 3f / 36f
        drawRoundRect(
            color = Color(0xFF06B6D4),
            topLeft = Offset(logoX + pad, logoY + pad),
            size = Size(logoW - pad * 2, logoW - pad * 2),
            cornerRadius = CornerRadius(logoW * 0.195f),
        )
    }
}
