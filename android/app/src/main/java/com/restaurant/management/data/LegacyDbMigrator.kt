package com.restaurant.management.data

import android.content.Context

/**
 * One-time: copy pre-login `restaurant.db` into the first signed-in user's per-account file,
 * then remove the legacy file so only per-user DBs remain.
 */
object LegacyDbMigrator {
    fun migrateLegacyVenueDbToUser(
        context: Context,
        userId: Long,
    ) {
        val legacy = context.getDatabasePath("restaurant.db")
        val target = context.getDatabasePath("restaurant_user_$userId.db")
        if (!legacy.exists() || target.exists()) return
        runCatching {
            legacy.copyTo(target, overwrite = false)
            if (target.exists() && target.length() > 0L) {
                legacy.delete()
            }
        }
    }
}
