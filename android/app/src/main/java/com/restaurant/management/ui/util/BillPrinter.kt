package com.restaurant.management.ui.util

import android.content.Context
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient
import com.restaurant.management.data.RestaurantRepository
import com.restaurant.management.model.orderStatusLabel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

/** Opens the Android print UI with an HTML bill (Wi‑Fi / Bluetooth printers registered as print services). */
object BillPrinter {
    fun print(
        context: Context,
        venueName: String,
        taxPercent: Double,
        serviceChargePercent: Double,
        detail: RestaurantRepository.ReportOrderDetail,
    ) {
        val html = buildBillHtml(venueName, taxPercent, serviceChargePercent, detail)
        val webView = WebView(context)
        webView.settings.javaScriptEnabled = false
        webView.webViewClient =
            object : WebViewClient() {
                override fun onPageFinished(view: WebView, url: String?) {
                    val jobName = "Order ${detail.orderId}"
                    val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
                    val adapter = view.createPrintDocumentAdapter(jobName)
                    printManager.print(
                        jobName,
                        adapter,
                        PrintAttributes.Builder().build(),
                    )
                    view.postDelayed({ runCatching { view.destroy() } }, 5000L)
                }
            }
        webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
    }

    private fun buildBillHtml(
        venueName: String,
        taxPercent: Double,
        serviceChargePercent: Double,
        detail: RestaurantRepository.ReportOrderDetail,
    ): String {
        val fmt = SimpleDateFormat("EEE, MMM d, yyyy HH:mm", Locale.getDefault())
        val whenStr = fmt.format(Date(detail.createdAt))
        val showTax = taxPercent > 1e-9
        val showService = serviceChargePercent > 1e-9
        val subtotalCents = detail.lines.sumOf { it.lineTotalCents }
        val taxCents =
            if (showTax) {
                (subtotalCents * taxPercent / 100.0).roundToInt()
            } else {
                0
            }
        val serviceCents =
            if (showService) {
                (subtotalCents * serviceChargePercent / 100.0).roundToInt()
            } else {
                0
            }
        val grandTotalCents = subtotalCents + taxCents + serviceCents

        val rows =
            buildString {
                if (detail.lines.isEmpty()) {
                    val colspan = if (showTax) 4 else 1
                    append("<tr><td colspan=\"$colspan\" style=\"padding:8px\">No line items</td></tr>")
                } else if (showTax) {
                    for (line in detail.lines) {
                        append("<tr>")
                        append("<td>").append(esc(line.itemName)).append("</td>")
                        append("<td style=\"text-align:right\">").append(line.quantity).append("</td>")
                        append("<td style=\"text-align:right\">")
                            .append(formatCents(line.unitPriceCents))
                            .append("</td>")
                        append("<td style=\"text-align:right\">")
                            .append(formatCents(line.lineTotalCents))
                            .append("</td>")
                        append("</tr>")
                    }
                } else {
                    for (line in detail.lines) {
                        val label =
                            if (line.quantity != 1) {
                                "${line.itemName} × ${line.quantity}"
                            } else {
                                line.itemName
                            }
                        append("<tr><td class=\"itemline\">")
                        append("<span class=\"iname\">").append(esc(label)).append("</span>")
                        append("<span class=\"iamt\">").append(formatCents(line.lineTotalCents)).append("</span>")
                        append("</td></tr>")
                    }
                }
            }

        val tableHeader =
            if (showTax) {
                "<tr><th>Item</th><th style=\"text-align:right\">Qty</th><th style=\"text-align:right\">Each</th><th style=\"text-align:right\">Line</th></tr>"
            } else {
                "<tr><th>Item</th></tr>"
            }

        val summary =
            buildString {
                append("<div class=\"summary\">")
                if (showTax || showService) {
                    append("<div class=\"sumline\"><span>Subtotal</span><span>")
                        .append(formatCents(subtotalCents))
                        .append("</span></div>")
                }
                if (showTax) {
                    val pct = formatPercentLabel(taxPercent)
                    append("<div class=\"sumline\"><span>Tax ($pct)</span><span>")
                        .append(formatCents(taxCents))
                        .append("</span></div>")
                }
                if (showService) {
                    val pct = formatPercentLabel(serviceChargePercent)
                    append("<div class=\"sumline\"><span>Service charge ($pct)</span><span>")
                        .append(formatCents(serviceCents))
                        .append("</span></div>")
                }
                append("</div>")
                append("<div class=\"total\">Total: ${formatCents(grandTotalCents)}</div>")
            }

        val tableLabel =
            detail.tableId?.let { "Table $it" } ?: "Takeaway / counter"
        return """
            <!DOCTYPE html><html><head><meta charset="UTF-8">
            <style>
              body { font-family: sans-serif; padding: 12px; font-size: 14px; }
              h1 { font-size: 18px; margin: 0 0 8px 0; }
              .muted { color: #555; font-size: 12px; }
              table { width: 100%; border-collapse: collapse; margin-top: 12px; }
              th, td { border-bottom: 1px solid #ddd; padding: 6px 4px; text-align: left; }
              th { font-size: 12px; }
              td.itemline { position: relative; }
              .iname { padding-right: 72px; display: inline-block; max-width: 75%; }
              .iamt { position: absolute; right: 4px; top: 6px; white-space: nowrap; }
              .summary { margin-top: 12px; font-size: 14px; }
              .sumline { display: flex; justify-content: space-between; padding: 4px 0; border-bottom: 1px solid #eee; }
              .total { font-weight: bold; margin-top: 12px; text-align: right; font-size: 16px; }
            </style></head><body>
            <h1>${esc(venueName)}</h1>
            <div class="muted">Order ID: ${detail.orderId} · $whenStr · ${esc(orderStatusLabel(detail.status))} · $tableLabel</div>
            <table>
            $tableHeader
            $rows
            </table>
            $summary
            </body></html>
        """.trimIndent()
    }

    private fun formatPercentLabel(value: Double): String {
        val rounded = kotlin.math.round(value * 100.0) / 100.0
        val v =
            if (kotlin.math.abs(rounded - rounded.toInt()) < 1e-6) {
                rounded.toInt().toString()
            } else {
                String.format(Locale.US, "%.2f", rounded).trimEnd('0').trimEnd('.')
            }
        return "$v%"
    }

    private fun esc(s: String): String =
        buildString(s.length + 8) {
            for (c in s) {
                when (c) {
                    '<' -> append("&lt;")
                    '>' -> append("&gt;")
                    '&' -> append("&amp;")
                    '"' -> append("&quot;")
                    else -> append(c)
                }
            }
        }
}
