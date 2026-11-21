package com.vknxworks.ridectrl.ui.logs

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.vknxworks.ridectrl.R
import com.vknxworks.ridectrl.data.CostCategory
import com.vknxworks.ridectrl.data.Fmt
import com.vknxworks.ridectrl.data.ReportLine
import com.vknxworks.ridectrl.data.TyrePosition
import com.vknxworks.ridectrl.data.YearReport
import java.io.OutputStream
import java.time.LocalDate

/**
 * Draws the yearly report as a PDF with Android's own [PdfDocument]: A4 pages, plain text, nothing from the
 * internet. Units, money and dates follow the app's settings ([fmt]); the texts follow the phone's language.
 */
object YearReportPdf {
    private const val PAGE_W = 595 // A4 in PostScript points
    private const val PAGE_H = 842
    private const val MARGIN = 48f

    fun write(context: Context, report: YearReport, fmt: Fmt, out: OutputStream) {
        val doc = PdfDocument()
        try {
            val w = Writer(context, doc)
            draw(context, w, report, fmt)
            w.finish()
            doc.writeTo(out)
        } finally {
            doc.close()
        }
    }

    private fun costLabel(context: Context, c: CostCategory): String = context.getString(
        when (c) {
            CostCategory.PURCHASE -> R.string.cost_purchase
            CostCategory.FUEL -> R.string.cost_fuel
            CostCategory.OIL -> R.string.cost_oil
            CostCategory.MAINTENANCE -> R.string.cost_maintenance
            CostCategory.PARTS -> R.string.cost_parts
            CostCategory.MODS -> R.string.cost_mods
            CostCategory.TYRES -> R.string.cost_tyres
            CostCategory.DOCUMENTS -> R.string.cost_documents
        },
    )

    private fun draw(context: Context, w: Writer, r: YearReport, fmt: Fmt) {
        val v = r.vehicle
        w.title(context.getString(R.string.report_pdf_title, v.name, r.year))
        val sub = listOfNotNull(v.make.ifBlank { null }, v.model.ifBlank { null }, v.year?.toString(), v.plate.ifBlank { null }).joinToString(" · ")
        if (sub.isNotBlank()) w.muted(sub)
        w.muted(context.getString(R.string.report_pdf_made, fmt.date(LocalDate.now().toString())))
        w.gap(10f)

        w.heading(context.getString(R.string.report_riding))
        w.pair(context.getString(R.string.report_km_ridden), fmt.distance(r.kmRidden))
        w.pair(context.getString(R.string.report_odometer_end), fmt.distance(r.odometerAtEnd))
        if (r.tempReadings > 0) w.pair(context.getString(R.string.report_temp_readings), r.tempReadings.toString())
        if (r.checks > 0) w.pair(context.getString(R.string.report_checks), r.checks.toString())
        if (r.jettingChanges > 0) w.pair(context.getString(R.string.report_jetting_changes), r.jettingChanges.toString())

        w.heading(context.getString(R.string.report_fuel))
        w.pair(context.getString(R.string.report_fills), r.fills.toString())
        w.pair(context.getString(R.string.report_litres), fmt.volume(r.litres))
        w.pair(context.getString(R.string.report_fuel_cost), fmt.money(r.fuelCost))
        w.pair(context.getString(R.string.stat_consumption), fmt.consumption(r.avgPer100))

        w.heading(context.getString(R.string.report_costs))
        if (r.costsByCategory.isEmpty()) {
            w.muted(context.getString(R.string.report_none))
        } else {
            r.costsByCategory.forEach { (c, amount) -> w.pair(costLabel(context, c), fmt.money(amount)) }
            w.pair(context.getString(R.string.report_total), fmt.money(r.totalCost), bold = true)
            if (r.kmRidden > 0) {
                val running = r.costsByCategory.filter { it.first != CostCategory.PURCHASE }.sumOf { it.second }
                w.pair(context.getString(R.string.stat_cost_per_km), fmt.moneyPerDistance(running / r.kmRidden))
            }
        }

        fun lines(title: Int, list: List<ReportLine>) {
            w.heading(context.getString(title))
            if (list.isEmpty()) {
                w.muted(context.getString(R.string.report_none))
                return
            }
            list.forEach { l ->
                val pos = l.position?.let {
                    context.getString(if (it == TyrePosition.FRONT) R.string.tyre_front else R.string.tyre_rear)
                }
                val text = listOfNotNull(pos, l.text.ifBlank { null }).joinToString(": ").ifBlank { "–" }
                w.row(fmt.shortDate(l.date), l.odometer?.let { fmt.distance(it) } ?: "", text, l.cost?.let { fmt.money(it) } ?: "")
            }
        }
        lines(R.string.report_services, r.services)
        lines(R.string.report_mods, r.mods)
        lines(R.string.report_tyres, r.tyres)

        w.gap(14f)
        w.muted(context.getString(R.string.report_pdf_note))
    }

    /** Lays text out top to bottom, starting a new page when one is full. */
    private class Writer(private val context: Context, private val doc: PdfDocument) {
        private var pageNo = 0
        private var page: PdfDocument.Page? = null
        private var canvas: Canvas? = null
        private var y = 0f

        private val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 20f; typeface = Typeface.DEFAULT_BOLD; color = Color.BLACK }
        private val headPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 13f; typeface = Typeface.DEFAULT_BOLD; color = Color.rgb(0x5A, 0x32, 0xC8) }
        private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 10.5f; typeface = Typeface.DEFAULT; color = Color.BLACK }
        private val boldPaint = Paint(textPaint).apply { typeface = Typeface.DEFAULT_BOLD }
        private val mutedPaint = Paint(textPaint).apply { color = Color.DKGRAY; textSize = 9.5f }
        private val linePaint = Paint().apply { color = Color.LTGRAY; strokeWidth = 0.6f }
        private val right = PAGE_W - MARGIN

        private fun ensure(height: Float) {
            if (canvas == null || y + height > PAGE_H - MARGIN) newPage()
        }

        private fun newPage() {
            page?.let { finishPage(it) }
            pageNo++
            val p = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, pageNo).create())
            page = p
            canvas = p.canvas
            y = MARGIN
        }

        private fun finishPage(p: PdfDocument.Page) {
            val footer = context.getString(R.string.report_pdf_page, pageNo)
            p.canvas.drawText(footer, right - mutedPaint.measureText(footer), PAGE_H - MARGIN / 2, mutedPaint)
            doc.finishPage(p)
        }

        fun finish() {
            if (canvas == null) newPage()
            page?.let { finishPage(it) }
            page = null
            canvas = null
        }

        fun gap(h: Float) {
            y += h
        }

        fun title(text: String) {
            ensure(30f)
            y += 20f
            canvas!!.drawText(text, MARGIN, y, titlePaint)
            y += 8f
        }

        fun heading(text: String) {
            ensure(40f)
            y += 22f
            canvas!!.drawText(text, MARGIN, y, headPaint)
            y += 5f
            canvas!!.drawLine(MARGIN, y, right, y, linePaint)
            y += 4f
        }

        fun muted(text: String) = wrapped(text, MARGIN, right, mutedPaint)

        /** A label on the left and its value on the right. */
        fun pair(label: String, value: String, bold: Boolean = false) {
            ensure(16f)
            y += 15f
            val paint = if (bold) boldPaint else textPaint
            canvas!!.drawText(label, MARGIN, y, paint)
            canvas!!.drawText(value, right - paint.measureText(value), y, paint)
        }

        /** A table row: date, odometer, text (wrapped) and an amount on the right. */
        fun row(date: String, odo: String, text: String, cost: String) {
            val textLeft = MARGIN + 150f
            val textRight = right - 70f
            val parts = split(text, textRight - textLeft, textPaint)
            ensure(15f * parts.size + 4f)
            y += 15f
            val c = canvas!!
            c.drawText(date, MARGIN, y, textPaint)
            c.drawText(odo, MARGIN + 70f, y, mutedPaint)
            c.drawText(cost, right - textPaint.measureText(cost), y, textPaint)
            parts.forEachIndexed { i, s ->
                if (i > 0) y += 13f
                c.drawText(s, textLeft, y, textPaint)
            }
        }

        private fun wrapped(text: String, left: Float, rightEdge: Float, paint: Paint) {
            split(text, rightEdge - left, paint).forEach { s ->
                ensure(14f)
                y += 13f
                canvas!!.drawText(s, left, y, paint)
            }
        }

        /** Splits [text] into lines that fit [width], breaking at spaces where it can. */
        private fun split(text: String, width: Float, paint: Paint): List<String> {
            val out = mutableListOf<String>()
            text.lines().forEach { para ->
                var rest = para
                if (rest.isEmpty()) out += ""
                while (rest.isNotEmpty()) {
                    var n = paint.breakText(rest, true, width, null)
                    if (n <= 0) n = 1
                    if (n < rest.length) {
                        val space = rest.lastIndexOf(' ', n)
                        if (space > 0) n = space
                    }
                    out += rest.substring(0, n).trimEnd()
                    rest = rest.substring(n).trimStart()
                }
            }
            return out
        }
    }
}
