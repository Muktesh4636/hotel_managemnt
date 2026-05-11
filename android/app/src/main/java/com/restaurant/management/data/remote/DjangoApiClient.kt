package com.restaurant.management.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class DjangoApiClient(
    private val baseUrl: String,
    private var authToken: String?,
) {
    private val client =
        OkHttpClient
            .Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build()

    private val jsonMedia = "application/json; charset=utf-8".toMediaType()

    fun setToken(token: String?) {
        authToken = token
    }

    private fun url(path: String): String {
        val b = baseUrl.trimEnd('/')
        val p = if (path.startsWith("/")) path else "/$path"
        return "$b$p"
    }

    private fun Request.Builder.withAuth(): Request.Builder {
        val t = authToken
        if (!t.isNullOrBlank()) {
            header("Authorization", "Token $t")
        }
        return this
    }

    suspend fun get(path: String): String =
        withContext(Dispatchers.IO) {
            val req =
                Request
                    .Builder()
                    .url(url(path))
                    .get()
                    .withAuth()
                    .build()
            client.newCall(req).execute().use { resp ->
                val body = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) {
                    throw ApiException(resp.code, body)
                }
                body
            }
        }

    suspend fun post(
        path: String,
        jsonBody: String,
    ): String =
        withContext(Dispatchers.IO) {
            val req =
                Request
                    .Builder()
                    .url(url(path))
                    .post(jsonBody.toRequestBody(jsonMedia))
                    .withAuth()
                    .build()
            client.newCall(req).execute().use { resp ->
                val body = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) {
                    throw ApiException(resp.code, body)
                }
                body
            }
        }

    suspend fun patch(
        path: String,
        jsonBody: String,
    ): String =
        withContext(Dispatchers.IO) {
            val req =
                Request
                    .Builder()
                    .url(url(path))
                    .patch(jsonBody.toRequestBody(jsonMedia))
                    .withAuth()
                    .build()
            client.newCall(req).execute().use { resp ->
                val body = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) {
                    throw ApiException(resp.code, body)
                }
                body
            }
        }

    suspend fun put(
        path: String,
        jsonBody: String,
    ): String =
        withContext(Dispatchers.IO) {
            val req =
                Request
                    .Builder()
                    .url(url(path))
                    .put(jsonBody.toRequestBody(jsonMedia))
                    .withAuth()
                    .build()
            client.newCall(req).execute().use { resp ->
                val body = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) {
                    throw ApiException(resp.code, body)
                }
                body
            }
        }

    suspend fun delete(path: String): Unit =
        withContext(Dispatchers.IO) {
            val req =
                Request
                    .Builder()
                    .url(url(path))
                    .delete()
                    .withAuth()
                    .build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) {
                    val body = resp.body?.string().orEmpty()
                    throw ApiException(resp.code, body)
                }
            }
        }

    class ApiException(
        val code: Int,
        val body: String,
    ) : Exception("HTTP $code: ${body.take(200)}")

    companion object {
        /** Login without token; returns JSON body with `token` key. */
        suspend fun login(
            baseUrl: String,
            loginRaw: String,
            password: String,
        ): String {
            val client = DjangoApiClient(baseUrl, null)
            val loginId = LoginIdNormalizer.normalize(loginRaw)
            val body =
                JSONObject()
                    .put("login_id", loginId)
                    .put("password", password)
                    .toString()
            return client.post("/api/v1/auth/login/", body)
        }
    }
}
