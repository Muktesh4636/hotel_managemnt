package com.restaurant.management.ui.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.restaurant.management.data.remote.ApiPrefs

/** Shown as payee name in UPI apps (short label). */
private const val UPI_PAYEE_DISPLAY_NAME = "Restaurant"

/**
 * Known Play Store package names for UPI apps. [launchSubscriptionUpiPayment] uses the standard
 * `upi://pay` URI; setting the package targets that app directly.
 */
object SubscriptionUpiPackages {
    const val PHONEPE = "com.phonepe.app"
    const val GOOGLE_PAY = "com.google.android.apps.nbu.paisa.user"
    const val PAYTM = "net.one97.paytm"
}

fun subscriptionBillingUpiVpa(context: Context): String = ApiPrefs(context).subscriptionUpiVpa

private fun buildUpiPayUri(
    vpa: String,
    amountRupeesWhole: Int,
    transactionNote: String,
): Uri {
    val note = transactionNote.trim().take(80)
    return Uri.Builder()
        .scheme("upi")
        .authority("pay")
        .appendQueryParameter("pa", vpa.trim())
        .appendQueryParameter("pn", UPI_PAYEE_DISPLAY_NAME)
        .appendQueryParameter("am", amountRupeesWhole.toString())
        .appendQueryParameter("cu", "INR")
        .appendQueryParameter("tn", note.ifBlank { "Subscription" })
        .build()
}

/**
 * Opens a UPI payment screen in the given app (or system handler if [appPackage] is null).
 * Amount is whole rupees (e.g. plan list price 299).
 */
fun launchSubscriptionUpiPayment(
    context: Context,
    vpa: String,
    amountRupeesWhole: Int,
    planLabel: String,
    appPackage: String?,
) {
    val uri = buildUpiPayUri(vpa, amountRupeesWhole, "Plan: $planLabel")
    val intent =
        Intent(Intent.ACTION_VIEW, uri).apply {
            if (!appPackage.isNullOrBlank()) {
                setPackage(appPackage)
            }
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    try {
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(
                context,
                "Could not open this app. Install it or use UPI to pick another app.",
                Toast.LENGTH_LONG,
            )
            .show()
    }
}
