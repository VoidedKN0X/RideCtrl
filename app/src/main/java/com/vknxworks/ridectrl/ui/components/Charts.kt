package com.vknxworks.ridectrl.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.vknxworks.ridectrl.ui.theme.CtrlColors
import com.vknxworks.ridectrl.ui.theme.CtrlType
import kotlin.math.max

/**
 * A simple line chart drawn on a canvas (no chart library): points in order, the highest and lowest value
 * labelled on the left, and optional first/last labels underneath.
 */
@Composable
fun LineChart(
    values: List<Double>,
    modifier: Modifier = Modifier,
    labelFor: (Double) -> String = { it.toString() },
    startLabel: String? = null,
    endLabel: String? = null,
) {
    if (values.isEmpty()) return
    val line = CtrlColors.AccentText
    val grid = CtrlColors.Outline
    val muted = CtrlColors.Muted
    val measurer = rememberTextMeasurer()
    val style = CtrlType.Data.copy(color = muted)
    Canvas(modifier.fillMaxWidth().height(170.dp)) {
        val hi = values.max()
        val lo = values.min()
        val range = (hi - lo).takeIf { it > 0 } ?: 1.0
        val hiText = measurer.measure(labelFor(hi), style)
        val loText = measurer.measure(labelFor(lo), style)
        val left = max(hiText.size.width, loText.size.width) + 10f
        val bottomPad = if (startLabel != null || endLabel != null) 22.dp.toPx() else 6.dp.toPx()
        val top = 8.dp.toPx()
        val w = size.width - left - 6.dp.toPx()
        val h = size.height - top - bottomPad
        // Grid: top and bottom lines.
        drawLine(grid, Offset(left, top), Offset(left + w, top), 1.dp.toPx())
        drawLine(grid, Offset(left, top + h), Offset(left + w, top + h), 1.dp.toPx())
        drawText(hiText, topLeft = Offset(0f, top - hiText.size.height / 2f))
        drawText(loText, topLeft = Offset(0f, top + h - loText.size.height / 2f))
        fun pt(i: Int): Offset {
            val x = if (values.size == 1) left + w / 2 else left + w * i / (values.size - 1)
            val y = top + h - ((values[i] - lo) / range * h).toFloat()
            return Offset(x, y)
        }
        if (values.size > 1) {
            val path = Path()
            values.indices.forEach { i -> val p = pt(i); if (i == 0) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y) }
            drawPath(path, line, style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
        values.indices.forEach { i -> drawCircle(line, radius = 3.dp.toPx(), center = pt(i)) }
        if (startLabel != null) {
            val t = measurer.measure(startLabel, style)
            drawText(t, topLeft = Offset(left, size.height - t.size.height))
        }
        if (endLabel != null) {
            val t = measurer.measure(endLabel, style)
            drawText(t, topLeft = Offset(size.width - t.size.width, size.height - t.size.height))
        }
    }
}

/** A bar chart with a label under each bar and the value on top. */
@Composable
fun BarChart(
    bars: List<Pair<String, Double>>,
    modifier: Modifier = Modifier,
    labelFor: (Double) -> String = { it.toString() },
) {
    if (bars.isEmpty()) return
    val fill = CtrlColors.Accent
    val muted = CtrlColors.Muted
    val text = CtrlColors.Text
    val measurer = rememberTextMeasurer()
    val labelStyle = CtrlType.Data.copy(color = muted)
    val valueStyle = CtrlType.Data.copy(color = text)
    Canvas(modifier.fillMaxWidth().height(190.dp)) {
        val hi = bars.maxOf { it.second }.takeIf { it > 0 } ?: 1.0
        val bottom = 22.dp.toPx()
        val topPad = 20.dp.toPx()
        val slot = size.width / bars.size
        val barW = (slot * 0.62f).coerceAtMost(46.dp.toPx())
        val h = size.height - bottom - topPad
        bars.forEachIndexed { i, (label, v) ->
            val bh = (v / hi * h).toFloat().coerceAtLeast(if (v > 0) 2.dp.toPx() else 0f)
            val x = slot * i + (slot - barW) / 2
            drawRoundRect(fill, topLeft = Offset(x, topPad + h - bh), size = Size(barW, bh), cornerRadius = CornerRadius(6.dp.toPx()))
            val lt = measurer.measure(label, labelStyle, maxLines = 1)
            drawText(lt, topLeft = Offset(slot * i + (slot - lt.size.width) / 2, size.height - lt.size.height))
            if (bars.size <= 8) {
                val vt = measurer.measure(labelFor(v), valueStyle, maxLines = 1)
                drawText(vt, topLeft = Offset(slot * i + (slot - vt.size.width) / 2, topPad + h - bh - vt.size.height - 2))
            }
        }
    }
}
