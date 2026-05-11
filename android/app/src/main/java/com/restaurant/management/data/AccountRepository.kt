package com.restaurant.management.data

import android.content.Context
import com.restaurant.management.data.local.dao.AccountDao
import com.restaurant.management.data.local.entity.AccountEntity
import com.restaurant.management.security.PasswordHasher

class AccountRepository(
    private val accountDao: AccountDao,
    private val appContext: Context,
) {
    private val prefs =
        appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getSavedUserId(): Long? =
        prefs.getLong(KEY_USER_ID, -1L).takeIf { it > 0L }

    suspend fun getAccountById(id: Long): AccountEntity? = accountDao.getById(id)

    fun saveUserId(userId: Long) {
        prefs.edit().putLong(KEY_USER_ID, userId).apply()
    }

    fun clearSavedUserId() {
        prefs.edit().remove(KEY_USER_ID).apply()
    }

    fun normalizeLoginId(raw: String): String {
        val t = raw.trim()
        if (t.isEmpty()) return ""
        val digits = t.filter { it.isDigit() }
        val phoneLike =
            digits.length >= 10 &&
                t.all { ch ->
                    ch.isDigit() || ch == '+' || ch == ' ' || ch == '-' || ch == '(' || ch == ')'
                }
        return if (phoneLike) digits else t.lowercase()
    }

    suspend fun register(
        loginRaw: String,
        password: String,
    ): Result<Long> {
        if (password.length < 6) {
            return Result.failure(IllegalArgumentException("Password must be at least 6 characters."))
        }
        val loginId = normalizeLoginId(loginRaw)
        if (loginId.isBlank()) {
            return Result.failure(IllegalArgumentException("Enter a phone number or username."))
        }
        if (accountDao.getByLoginId(loginId) != null) {
            return Result.failure(IllegalArgumentException("That phone or username is already registered."))
        }
        val isFirstAccount = accountDao.count() == 0
        val salt = PasswordHasher.newSalt()
        val hash = PasswordHasher.hash(password.toCharArray(), salt)
        val newId =
            accountDao.insert(
                AccountEntity(
                    loginId = loginId,
                    passwordSaltB64 = PasswordHasher.encodeB64(salt),
                    passwordHashB64 = PasswordHasher.encodeB64(hash),
                    createdAtEpochMillis = System.currentTimeMillis(),
                ),
            )
        if (isFirstAccount) {
            LegacyDbMigrator.migrateLegacyVenueDbToUser(appContext, newId)
        }
        return Result.success(newId)
    }

    /**
     * If no account exists for this login id, registers it. Used for dev/demo installs only.
     */
    suspend fun ensureAccountExistsIfAbsent(
        loginRaw: String,
        password: String,
    ) {
        val loginId = normalizeLoginId(loginRaw)
        if (loginId.isBlank() || password.length < 6) return
        if (accountDao.getByLoginId(loginId) != null) return
        register(loginRaw, password).getOrElse { return }
    }

    suspend fun verifyLogin(
        loginRaw: String,
        password: String,
    ): Long? {
        val loginId = normalizeLoginId(loginRaw)
        if (loginId.isBlank()) return null
        val acc = accountDao.getByLoginId(loginId) ?: return null
        val ok =
            PasswordHasher.verify(
                password.toCharArray(),
                acc.passwordSaltB64,
                acc.passwordHashB64,
            )
        return if (ok) acc.id else null
    }

    companion object {
        private const val PREFS_NAME = "restaurant_session"
        private const val KEY_USER_ID = "signed_in_user_id"
    }
}
