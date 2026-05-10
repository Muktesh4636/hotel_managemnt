package com.restaurant.management.ui.util

import android.content.Context
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient
import com.restaurant.management.data.RestaurantRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Opens the Android print UI with an HTML bill (Wi‑Fi / Bluetooth printers registered as print services). */
object BillPrinter {
    fun print(
        context: Context,
        venueName: String,
        detail: RestaurantRepository.ReportOrderDetail,
    ) {
        val html = buildBillHtml(venueName, detail)
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
        detail: RestaurantRepository.ReportOrderDetail,
    ): String {
        val fmt = SimpleDateFormat("EEE, MMM d, yyyy HH:mm", Locale.getDefault())
        val whenStr = fmt.format(Date(detail.createdAt))
        val rows =
            buildString {
                if (detail.lines.isEmpty()) {
                    append("<tr><td colspan=\"4\" style=\"padding:8px\">No line items</td></tr>")
                } else {
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
                }
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
              .total { font-weight: bold; margin-top: 12px; text-align: right; font-size: 16px; }
            </style></head><body>
            <h1>${esc(venueName)}</h1>
            <div class="muted">Order ID: ${detail.orderId} · $whenStr · ${esc(detail.status)} · $tableLabel</div>
            <table>
            <tr><th>Item</th><th style="text-align:right">Qty</th><th style="text-align:right">Each</th><th style="text-align:right">Line</th></tr>
            $rows
            </table>
            <div class="total">Total: ${formatCents(detail.totalCents)}</div>
            </body></html>
        """.trimIndent()
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
