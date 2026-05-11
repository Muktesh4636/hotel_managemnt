package com.restaurant.management.data.remote

import android.content.Context

/**
 * Stores Django REST base URL and API token (after "Log in to backend" in settings).
 */
class ApiPrefs(
    context: Context,
) {
    private val p =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    var baseUrl: String
        get() = p.getString(KEY_BASE, "")?.trim()?.trimEnd('/') ?: ""
        set(value) {
            p.edit().putString(KEY_BASE, value.trim().trimEnd('/')).apply()
        }

    var token: String
        get() = p.getString(KEY_TOKEN, "") ?: ""
        set(value) {
            p.edit().putString(KEY_TOKEN, value).apply()
        }

    fun isConfigured(): Boolean = baseUrl.isNotBlank() && token.isNotBlank()

    fun clear() {
        p.edit().remove(KEY_TOKEN).apply()
    }

    /** Clears saved base URL and token (disconnect from Django). */
    fun clearAll() {
        p.edit().remove(KEY_BASE).remove(KEY_TOKEN).apply()
    }

    /**
     * UPI ID (VPA) for subscription plan payments. Persisted so a future API/settings flow can
     * overwrite it; until then [DEFAULT_SUBSCRIPTION_UPI_VPA] is used when unset.
     */
    var subscriptionUpiVpa: String
        get() {
            val s = p.getString(KEY_SUBSCRIPTION_UPI_VPA, null)?.trim()
            return if (s.isNullOrBlank()) DEFAULT_SUBSCRIPTION_UPI_VPA else s
        }
        set(value) {
            val t = value.trim()
            if (t.isEmpty()) {
                p.edit().remove(KEY_SUBSCRIPTION_UPI_VPA).apply()
            } else {
                p.edit().putString(KEY_SUBSCRIPTION_UPI_VPA, t).apply()
            }
        }

    companion object {
        private const val PREFS = "restaurant_backend_api"
        private const val KEY_BASE = "base_url"
        private const val KEY_TOKEN = "token"
        private const val KEY_SUBSCRIPTION_UPI_VPA = "subscription_upi_vpa"

        /** Fallback when the server has not yet supplied a VPA. */
        const val DEFAULT_SUBSCRIPTION_UPI_VPA = "9182351381@ybl"
    }
}
