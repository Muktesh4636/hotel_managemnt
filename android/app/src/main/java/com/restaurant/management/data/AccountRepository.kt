package com.restaurant.management.data

import android.content.Context
import com.restaurant.management.data.local.dao.AccountDao
import com.restaurant.management.data.local.entity.AccountEntity
import com.restaurant.management.data.remote.ApiPrefs
import com.restaurant.management.data.remote.DjangoApiClient
import com.restaurant.management.security.PasswordHasher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

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
        if (!phoneLike) return t.lowercase()
        if (digits.length == 12 && digits.startsWith("91")) return digits.drop(2)
        if (digits.length == 11 && digits.startsWith("0")) {
            val rest = digits.drop(1)
            if (rest.length == 10) return rest
        }
        return digits
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

    /**
     * Same sign-in as the web CRM: try Django `/api/v1/auth/login/` first, then fall back to the
     * on-device account DB. On API success, saves the API token and creates or updates the local
     * staff row so [openRestaurantForUser] can run.
     */
    suspend fun verifyLoginWithBackendThenLocal(
        loginRaw: String,
        password: String,
    ): Long? =
        withContext(Dispatchers.IO) {
            val loginId = normalizeLoginId(loginRaw)
            if (loginId.isBlank() || password.isBlank()) return@withContext null
            val prefs = ApiPrefs(appContext)
            val baseUrl = prefs.baseUrl.trimEnd('/')
            if (baseUrl.isBlank()) return@withContext verifyLogin(loginRaw, password)

            suspend fun applyTokenAndPasswordFromDjango() {
                val body = DjangoApiClient.login(baseUrl, loginRaw, password)
                val token = JSONObject(body).getString("token")
                prefs.token = token
                val existing = accountDao.getByLoginId(loginId)
                if (existing != null) {
                    val salt = PasswordHasher.newSalt()
                    val hash = PasswordHasher.hash(password.toCharArray(), salt)
                    accountDao.updatePasswordHashes(
                        existing.id,
                        PasswordHasher.encodeB64(salt),
                        PasswordHasher.encodeB64(hash),
                    )
                }
            }

            // Offline-first: existing on-device accounts sign in without contacting the server.
            val localId = verifyLogin(loginRaw, password)
            if (localId != null) {
                try {
                    applyTokenAndPasswordFromDjango()
                } catch (_: Exception) {
                    // No network or server error — stay signed in locally; token may be stale until online.
                }
                return@withContext localId
            }

            // First-time link: Django account with no local row yet (needs network once).
            try {
                applyTokenAndPasswordFromDjango()
                val existing = accountDao.getByLoginId(loginId)
                if (existing != null) {
                    return@withContext existing.id
                }
                val reg = register(loginRaw, password)
                reg.fold(
                    onSuccess = { return@withContext it },
                    onFailure = {
                        val again = accountDao.getByLoginId(loginId)
                        if (again != null) {
                            val salt = PasswordHasher.newSalt()
                            val hash = PasswordHasher.hash(password.toCharArray(), salt)
                            accountDao.updatePasswordHashes(
                                again.id,
                                PasswordHasher.encodeB64(salt),
                                PasswordHasher.encodeB64(hash),
                            )
                            return@withContext again.id
                        }
                        return@withContext null
                    },
                )
            } catch (_: Exception) {
                // Unreachable server — no local account to fall back to
            }
            null
        }

    companion object {
        private const val PREFS_NAME = "restaurant_session"
        private const val KEY_USER_ID = "signed_in_user_id"
    }
}
