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
import androidx.compose.ui.graphics.PathEffect
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

/**
 * Values over time on a canvas: the x axis is real time (so a gap of months looks like one), each point is
 * one value, and a dashed line marks [average] with its label on the right. The highest and lowest values are
 * labelled on the left, the first and last dates underneath.
 */
@Composable
fun TimeChart(
    points: List<Pair<java.time.LocalDate, Double>>,
    modifier: Modifier = Modifier,
    average: Double? = null,
    averageLabel: String? = null,
    labelFor: (Double) -> String = { it.toString() },
    dateLabel: (java.time.LocalDate) -> String = { it.toString() },
) {
    if (points.isEmpty()) return
    val sorted = points.sortedBy { it.first }
    val line = CtrlColors.AccentText
    val grid = CtrlColors.Outline
    val muted = CtrlColors.Muted
    val avgColor = CtrlColors.Text
    val measurer = rememberTextMeasurer()
    val style = CtrlType.Data.copy(color = muted)
    val avgStyle = CtrlType.Data.copy(color = avgColor)
    Canvas(modifier.fillMaxWidth().height(190.dp)) {
        val values = sorted.map { it.second } + listOfNotNull(average)
        val hi = values.max()
        val lo = values.min()
        // A little room above and below, so the dots and the average line don't sit on the frame.
        val pad = ((hi - lo).takeIf { it > 0 } ?: (hi.coerceAtLeast(1.0) * 0.1)) * 0.08
        val top0 = hi + pad
        val bottom0 = lo - pad
        val range = top0 - bottom0
        val hiText = measurer.measure(labelFor(hi), style)
        val loText = measurer.measure(labelFor(lo), style)
        val left = max(hiText.size.width, loText.size.width) + 10f
        val bottomPad = 22.dp.toPx()
        val top = 8.dp.toPx()
        val w = size.width - left - 6.dp.toPx()
        val h = size.height - top - bottomPad
        val firstDay = sorted.first().first.toEpochDay()
        val span = (sorted.last().first.toEpochDay() - firstDay).toFloat()
        fun y(v: Double) = top + h - ((v - bottom0) / range * h).toFloat()
        fun x(d: java.time.LocalDate) = if (span <= 0f) left + w / 2 else left + w * ((d.toEpochDay() - firstDay) / span)
        drawLine(grid, Offset(left, top), Offset(left + w, top), 1.dp.toPx())
        drawLine(grid, Offset(left, top + h), Offset(left + w, top + h), 1.dp.toPx())
        drawText(hiText, topLeft = Offset(0f, y(hi) - hiText.size.height / 2f))
        drawText(loText, topLeft = Offset(0f, y(lo) - loText.size.height / 2f))
        if (average != null) {
            val ay = y(average)
            drawLine(
                avgColor.copy(alpha = 0.7f), Offset(left, ay), Offset(left + w, ay), 1.5.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8.dp.toPx(), 6.dp.toPx())),
            )
            if (averageLabel != null) {
                val t = measurer.measure(averageLabel, avgStyle, maxLines = 1)
                val ty = (ay - t.size.height - 2.dp.toPx()).coerceAtLeast(0f)
                drawText(t, topLeft = Offset(left + w - t.size.width, ty))
            }
        }
        if (sorted.size > 1) {
            val path = Path()
            sorted.forEachIndexed { i, (d, v) -> if (i == 0) path.moveTo(x(d), y(v)) else path.lineTo(x(d), y(v)) }
            drawPath(path, line, style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
        sorted.forEach { (d, v) -> drawCircle(line, radius = 3.dp.toPx(), center = Offset(x(d), y(v))) }
        val start = measurer.measure(dateLabel(sorted.first().first), style)
        drawText(start, topLeft = Offset(left, size.height - start.size.height))
        if (sorted.size > 1 && span > 0f) {
            val end = measurer.measure(dateLabel(sorted.last().first), style)
            drawText(end, topLeft = Offset(size.width - end.size.width, size.height - end.size.height))
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
